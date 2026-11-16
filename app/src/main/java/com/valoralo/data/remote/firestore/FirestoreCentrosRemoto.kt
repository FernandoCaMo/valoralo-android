package com.valoralo.data.remote.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.valoralo.data.repository.CentrosRemoto
import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.Valoracion
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Read-only access to Firestore (the rules forbid client writes to centros and config). */
@Singleton
class FirestoreCentrosRemoto @Inject constructor(
    private val db: FirebaseFirestore,
) : CentrosRemoto {

    override suspend fun versionCatalogo(): String? =
        db.document(CONFIG_CATALOGO).get(Source.SERVER).await().getString("version")

    override suspend fun descargarCentros(): List<Centro> =
        db.collection(CENTROS).get(Source.SERVER).await().documents
            .map { it.data.orEmpty().aCentro(it.id) }

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
        // Sorting by fecha happens in the domain (filtrarPor). The composite index
        // (estado, fecha) is deployed for when reviews need server-side pagination.
        val consulta = db.collection(CENTROS).document(codigo).collection(VALORACIONES)
            .whereEqualTo("estado", EstadoValoracion.PUBLICADA.name)
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
        const val TAG = "FirestoreCentros"
        const val CENTROS = "centros"
        const val VALORACIONES = "valoraciones"
        const val CONFIG_CATALOGO = "config/catalogo"
    }
}
