package com.valoralo.data.remote.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.valoralo.data.repository.CentrosRemoto
import com.valoralo.data.repository.InfoCatalogo
import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.Valoracion
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/** Read-only access to Firestore (the rules forbid client writes to centros, catalogo and config). */
@Singleton
class FirestoreCentrosRemoto @Inject constructor(
    private val db: FirebaseFirestore,
) : CentrosRemoto {

    override suspend fun infoCatalogo(): InfoCatalogo {
        val snap = db.document(CONFIG_CATALOGO).get(Source.SERVER).await()
        val partes = (snap.get("partes") as? List<*>).orEmpty().filterIsInstance<String>()
        return InfoCatalogo(snap.getString("version"), partes)
    }

    override suspend fun descargarCatalogo(
        partes: List<String>,
        alProgresar: (hechas: Int, total: Int) -> Unit,
    ): List<Centro> {
        if (partes.isEmpty()) {
            // Fallback for a project without catalogo/ documents: the whole collection.
            alProgresar(0, 1)
            return db.collection(CENTROS).get(Source.SERVER).await().documents
                .map { it.data.orEmpty().aCentro(it.id) }
                .also { alProgresar(1, 1) }
        }
        val hechas = AtomicInteger()
        return coroutineScope {
            partes.map { id ->
                async {
                    db.collection(CATALOGO).document(id).get(Source.SERVER).await()
                        .data.orEmpty().aCentrosDeCatalogo()
                        .also { alProgresar(hechas.incrementAndGet(), partes.size) }
                }
            }.awaitAll().flatten()
        }
    }

    override fun observarCentro(codigo: String): Flow<Centro?> = callbackFlow {
        val registro = db.collection(CENTROS).document(codigo).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "Error leyendo el centro $codigo", error)
                close(error)
                return@addSnapshotListener
            }
            // Offline and not cached: Firestore says "doesn't exist (from cache)". That is not "not found":
            // keep waiting for the server (the detail shows "Sin conexión" meanwhile).
            if (snap != null && !snap.exists() && snap.metadata.isFromCache) return@addSnapshotListener
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
        const val CATALOGO = "catalogo"
        const val VALORACIONES = "valoraciones"
        const val CONFIG_CATALOGO = "config/catalogo"
    }
}
