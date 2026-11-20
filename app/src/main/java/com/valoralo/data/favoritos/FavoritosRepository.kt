package com.valoralo.data.favoritos

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Limit per user, checked in the app (docs/decisiones.md). */
const val MAX_FAVORITOS = 100

enum class ErrorFavorito(val mensaje: String) {
    SIN_SESION("Inicia sesión para guardar centros en favoritos."),
    LIMITE("Puedes guardar como máximo $MAX_FAVORITOS centros en favoritos."),
    OTRO(com.valoralo.data.red.MENSAJE_ERROR_GENERICO),
}

class ExcepcionFavorito(val error: ErrorFavorito) : Exception(error.mensaje)

interface FavoritosRepository {
    /** Codes of the user's favourite centros; null = no session. */
    val favoritos: StateFlow<Set<String>?>

    /** Adds or removes; returns true if it is a favourite now. Throws [ExcepcionFavorito]. */
    suspend fun alternar(codigo: String): Boolean
}

/** usuarios/{uid}/favoritos/{codigo} = { codigo, fecha }. Works offline through Firestore's cache. */
@Singleton
class FirebaseFavoritosRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) : FavoritosRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val uid = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val favoritos: StateFlow<Set<String>?> = uid.flatMapLatest { uid ->
        if (uid == null) flowOf<Set<String>?>(null) else callbackFlow<Set<String>?> {
            // Most recently added first (Inicio shows the first three). The set keeps this order.
            val consulta = db.collection("usuarios/$uid/favoritos").orderBy("fecha", Query.Direction.DESCENDING)
            val registro = consulta.addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "Error leyendo favoritos", error)
                    return@addSnapshotListener
                }
                trySend(snap?.documents.orEmpty().map { it.id }.toSet())
            }
            awaitClose { registro.remove() }
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun alternar(codigo: String): Boolean {
        val uid = auth.currentUser?.uid ?: throw ExcepcionFavorito(ErrorFavorito.SIN_SESION)
        val actuales = favoritos.value.orEmpty()
        val ref = db.document("usuarios/$uid/favoritos/$codigo")
        return try {
            if (codigo in actuales) {
                ref.delete().await()
                false
            } else {
                if (actuales.size >= MAX_FAVORITOS) throw ExcepcionFavorito(ErrorFavorito.LIMITE)
                ref.set(mapOf("codigo" to codigo, "fecha" to FieldValue.serverTimestamp())).await()
                true
            }
        } catch (e: ExcepcionFavorito) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error guardando favorito", e)
            throw ExcepcionFavorito(ErrorFavorito.OTRO)
        }
    }

    private companion object {
        const val TAG = "Favoritos"
    }
}
