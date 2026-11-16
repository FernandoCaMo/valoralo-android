package com.valoralo.data.remote.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.valoralo.data.repository.CentroRepository
import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.Valoracion
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the catalogue from Firestore with snapshot listeners (live stats).
 * Read-only: the rules forbid client writes to centros.
 */
@Singleton
class FirestoreCentroRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : CentroRepository {

    override fun observarCentros(): Flow<List<Centro>> = callbackFlow {
        val registro = db.collection(CENTROS).orderBy("nombre").addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "Error leyendo centros", error)
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.documents.orEmpty().map { it.data.orEmpty().aCentro(it.id) })
        }
        awaitClose { registro.remove() }
    }

    override fun observarCentro(codigo: String): Flow<Centro?> = callbackFlow {
        val registro = db.collection(CENTROS).document(codigo).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "Error leyendo el centro $codigo", error)
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.takeIf { it.exists() }?.let { it.data.orEmpty().aCentro(it.id) })
        }
        awaitClose { registro.remove() }
    }

    override fun observarValoraciones(codigo: String): Flow<List<Valoracion>> = callbackFlow {
        // The rules only allow reading published reviews, so the query must filter by estado.
        val consulta = db.collection(CENTROS).document(codigo).collection(VALORACIONES)
            .whereEqualTo("estado", EstadoValoracion.PUBLICADA.name)
            .orderBy("fecha", Query.Direction.DESCENDING)
        val registro = consulta.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "Error leyendo valoraciones de $codigo", error)
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.documents.orEmpty().mapNotNull { it.data.orEmpty().aValoracion(it.id) })
        }
        awaitClose { registro.remove() }
    }

    private companion object {
        const val TAG = "FirestoreCentroRepo"
        const val CENTROS = "centros"
        const val VALORACIONES = "valoraciones"
    }
}
