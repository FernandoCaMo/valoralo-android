package com.valoralo.data.bloqueos

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

class ExcepcionBloqueo(mensaje: String) : Exception(mensaje)

interface BloqueosRepository {
    /** Blocked authors: uid → alias. Empty without a session. */
    val bloqueados: StateFlow<Map<String, String>>

    /** Throws [ExcepcionBloqueo]. */
    suspend fun bloquear(autorUid: String, alias: String)
    suspend fun desbloquear(autorUid: String)
}

/**
 * usuarios/{uid}/bloqueados/{autorUid} = { fecha, alias } (Google Play's user-generated content
 * policy). The user stops seeing that author's reviews; only on this account.
 */
@Singleton
class FirebaseBloqueosRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) : BloqueosRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val uid = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val bloqueados: StateFlow<Map<String, String>> = uid.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyMap()) else callbackFlow<Map<String, String>> {
            val registro = db.collection("usuarios/$uid/bloqueados").addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "Error leyendo bloqueados", error)
                    return@addSnapshotListener
                }
                trySend(snap?.documents.orEmpty().associate { it.id to (it.getString("alias") ?: "") })
            }
            awaitClose { registro.remove() }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    override suspend fun bloquear(autorUid: String, alias: String) {
        val uid = auth.currentUser?.uid ?: throw ExcepcionBloqueo("Inicia sesión para bloquear a un autor.")
        try {
            db.document("usuarios/$uid/bloqueados/$autorUid")
                .set(mapOf("fecha" to FieldValue.serverTimestamp(), "alias" to alias.take(30)))
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Error bloqueando", e)
            throw ExcepcionBloqueo("No se ha podido bloquear. Inténtalo de nuevo.")
        }
    }

    override suspend fun desbloquear(autorUid: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            db.document("usuarios/$uid/bloqueados/$autorUid").delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Error desbloqueando", e)
            throw ExcepcionBloqueo("No se ha podido desbloquear. Inténtalo de nuevo.")
        }
    }

    private companion object {
        const val TAG = "Bloqueos"
    }
}
