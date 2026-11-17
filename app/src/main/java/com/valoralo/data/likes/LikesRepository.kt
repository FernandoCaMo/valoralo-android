package com.valoralo.data.likes

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.valoralo.domain.usecase.LimiteLikes
import com.valoralo.domain.usecase.MAX_LIKES_POR_HORA
import com.valoralo.domain.usecase.PasoLimite
import com.valoralo.domain.usecase.siguientePasoLimite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** A review: centros/{codigo}/valoraciones/{autorUid}. */
data class ClaveResena(val codigo: String, val autorUid: String)

enum class ErrorLike(val mensaje: String) {
    SIN_SESION("Inicia sesión para dar like a las reseñas."),
    LIMITE("Has dado muchos likes en poco tiempo. Prueba más tarde."),
    OTRO("No se ha podido guardar el like. Inténtalo de nuevo."),
}

class ExcepcionLike(val error: ErrorLike) : Exception(error.mensaje)

interface LikesRepository {
    /** uid of the signed-in user (to tell their own reviews), or null. */
    fun uidActual(): String?

    /** The reviews I have liked among those centros (live). Empty without a session. */
    fun misLikes(codigos: List<String>): Flow<Set<ClaveResena>>

    /** Gives or takes back my like. Throws [ExcepcionLike]. */
    suspend fun alternar(resena: ClaveResena, dar: Boolean)
}

/**
 * Likes are written by the app; the rules check them (one per user and review, not your own,
 * only published, hourly limit) and alCambiarLike keeps the review's nLikes counter.
 */
@Singleton
class FirebaseLikesRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) : LikesRepository {

    override fun uidActual(): String? = auth.currentUser?.uid

    private val uid = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun misLikes(codigos: List<String>): Flow<Set<ClaveResena>> = uid.flatMapLatest { uid ->
        if (uid == null || codigos.isEmpty()) flowOf(emptySet()) else callbackFlow<Set<ClaveResena>> {
            // Collection-group query (index likes: uid + codigo); "in" accepts up to 30 values.
            val consulta = db.collectionGroup("likes").whereEqualTo("uid", uid).whereIn("codigo", codigos.distinct().take(30))
            val registro = consulta.addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "Error leyendo mis likes", error)
                    return@addSnapshotListener
                }
                trySend(snap?.documents.orEmpty().mapNotNull { d ->
                    val autor = d.reference.parent.parent?.id ?: return@mapNotNull null
                    ClaveResena(d.getString("codigo") ?: return@mapNotNull null, autor)
                }.toSet())
            }
            awaitClose { registro.remove() }
        }
    }

    override suspend fun alternar(resena: ClaveResena, dar: Boolean) {
        val uid = auth.currentUser?.uid ?: throw ExcepcionLike(ErrorLike.SIN_SESION)
        val like = db.document("centros/${resena.codigo}/valoraciones/${resena.autorUid}/likes/$uid")
        try {
            if (!dar) {
                like.delete().await()
                return
            }
            val max = runCatching {
                (db.document("config/likes").get().await().getLong("maxPorHora"))?.toInt()
            }.getOrNull() ?: MAX_LIKES_POR_HORA
            val limite = db.document("usuarios/$uid/limites/likes")
            // The like and the hourly counter in one transaction, as the rules require.
            db.runTransaction { tx ->
                val snap = tx.get(limite)
                val actual = if (snap.exists()) {
                    LimiteLikes(snap.getTimestamp("inicio")?.toDate()?.time ?: 0L, snap.getLong("n")?.toInt() ?: 0)
                } else null
                when (siguientePasoLimite(actual, Timestamp.now().toDate().time, max)) {
                    PasoLimite.Lleno -> throw ExcepcionLike(ErrorLike.LIMITE)
                    PasoLimite.Empezar -> tx.set(limite, mapOf("inicio" to FieldValue.serverTimestamp(), "n" to 1))
                    PasoLimite.Sumar -> tx.update(limite, "n", (actual?.n ?: 0) + 1)
                }
                tx.set(like, mapOf("uid" to uid, "codigo" to resena.codigo, "fecha" to FieldValue.serverTimestamp()))
            }.await()
        } catch (e: ExcepcionLike) {
            throw e
        } catch (e: Exception) {
            // The transaction wraps exceptions thrown inside it.
            (e.cause as? ExcepcionLike)?.let { throw it }
            Log.w(TAG, "Error guardando el like", e)
            throw ExcepcionLike(ErrorLike.OTRO)
        }
    }

    private companion object {
        const val TAG = "Likes"
    }
}
