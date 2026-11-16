package com.valoralo.data.verificacion

import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.valoralo.data.valoraciones.ErrorPublicacion
import com.valoralo.data.valoraciones.TipoErrorPublicacion
import com.valoralo.data.valoraciones.errorDeFuncion
import com.valoralo.domain.model.MiVerificacion
import com.valoralo.domain.usecase.normalizarCsv
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Teacher verification (doc 04). Operations throw [ErrorPublicacion]. */
interface VerificacionRepository {
    /** The signed-in user's request (null = none or no session). */
    fun observarMiVerificacion(): Flow<MiVerificacion?>

    /** Uploads the optional PDF and creates the request. */
    suspend fun solicitar(nombre: String, csv: String, pdf: Uri?)
}

@Singleton
class FirebaseVerificacionRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage,
) : VerificacionRepository {

    private fun uidActual(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarMiVerificacion(): Flow<MiVerificacion?> = uidActual().flatMapLatest { uid ->
        if (uid == null) flowOf(null) else callbackFlow {
            val registro = db.document("verificaciones/$uid").addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "Error leyendo mi verificación", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(miVerificacionDesde(snap?.data))
            }
            awaitClose { registro.remove() }
        }
    }

    override suspend fun solicitar(nombre: String, csv: String, pdf: Uri?) {
        val uid = auth.currentUser?.uid ?: throw ErrorPublicacion(TipoErrorPublicacion.SIN_SESION)
        try {
            val ruta = pdf?.let { subirPdf(uid, it) }
            val datos = buildMap<String, Any> {
                put("nombre", nombre.trim())
                put("csv", normalizarCsv(csv))
                ruta?.let { put("pdfPath", it) }
            }
            functions.getHttpsCallable("solicitarVerificacion").call(datos).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorPublicacion) {
            throw e
        } catch (e: FirebaseFunctionsException) {
            throw errorDeFuncion(e.code.name, e.message, e.details)
        } catch (e: FirebaseNetworkException) {
            throw ErrorPublicacion(TipoErrorPublicacion.SIN_CONEXION)
        } catch (e: Exception) {
            Log.w(TAG, "Error solicitando la verificación", e)
            throw ErrorPublicacion(TipoErrorPublicacion.OTRO)
        }
    }

    /** verificaciones/{uid}/hoja-<time>.pdf: a new name each time (the rules forbid overwriting). */
    private suspend fun subirPdf(uid: String, pdf: Uri): String {
        val ruta = "verificaciones/$uid/hoja-${System.currentTimeMillis()}.pdf"
        val metadatos = StorageMetadata.Builder().setContentType("application/pdf").build()
        storage.reference.child(ruta).putFile(pdf, metadatos).await()
        return ruta
    }

    private companion object {
        const val TAG = "Verificacion"
    }
}
