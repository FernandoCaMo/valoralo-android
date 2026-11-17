package com.valoralo.data.destacadas

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.valoralo.data.remote.firestore.aDestacadas
import com.valoralo.domain.model.Destacadas
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface DestacadasRepository {
    /** One read of destacadas/actual; the cached copy if there is no network. */
    suspend fun cargar(): Destacadas
}

@Singleton
class FirebaseDestacadasRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : DestacadasRepository {

    override suspend fun cargar(): Destacadas {
        val ref = db.document("destacadas/actual")
        val snap = try {
            ref.get().await()
        } catch (e: Exception) {
            Log.w("Destacadas", "Sin red: se usa la copia local", e)
            runCatching { ref.get(Source.CACHE).await() }.getOrNull()
        }
        return snap?.data?.aDestacadas() ?: Destacadas()
    }
}
