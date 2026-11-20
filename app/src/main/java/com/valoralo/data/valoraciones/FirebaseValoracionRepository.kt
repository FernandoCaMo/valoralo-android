package com.valoralo.data.valoraciones

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.valoralo.data.red.Comprobacion
import com.valoralo.data.red.LlamadasFunciones
import com.valoralo.data.remote.firestore.aFragmentos
import com.valoralo.data.remote.firestore.aValoracion
import com.valoralo.domain.model.OrigenRevision
import com.valoralo.domain.model.Valoracion
import com.valoralo.domain.usecase.FormularioValoracion
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

@Singleton
class FirebaseValoracionRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val llamadas: LlamadasFunciones,
) : ValoracionRepository {

    private fun uidActual(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarMiValoracion(codigo: String): Flow<Valoracion?> = uidActual().flatMapLatest { uid ->
        if (uid == null) flowOf(null) else callbackFlow {
            val registro = db.document("centros/$codigo/valoraciones/$uid").addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "Error leyendo mi valoración", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snap?.takeIf { it.exists() }?.let { it.data.orEmpty().aValoracion(it.id) })
            }
            awaitClose { registro.remove() }
        }
    }

    override suspend fun publicar(codigo: String, formulario: FormularioValoracion): ResultadoPublicacion {
        val uid = auth.currentUser?.uid ?: throw ErrorPublicacion(TipoErrorPublicacion.SIN_SESION)
        val enviado = formulario.paraEnviar(codigo)
        val datos = llamadas.llamar(
            "publicarValoracion", enviado,
            mensajeIncierto = "Se ha cortado la conexión y no sabemos si tu valoración se ha guardado. " +
                "Cuando vuelvas a tener red, abre el centro para comprobarlo; tu texto sigue aquí.",
        ) {
            // Connection lost while publishing: is the review on the server with what was sent?
            val guardada = db.document("centros/$codigo/valoraciones/$uid").get(Source.SERVER).await()
            if (coincideConGuardada(enviado, guardada.data)) {
                val revision = guardada.get("revision") as? Map<*, *>
                Comprobacion.Hecha(mapOf("estado" to guardada.getString("estado"), "fragmentos" to revision?.get("fragmentos"), "origen" to revision?.get("origen")))
            } else {
                Comprobacion.NoHecha
            }
        } as? Map<*, *>
        return if (datos?.get("estado") == "EN_REVISION") {
            ResultadoPublicacion.EnRevision(
                aFragmentos(datos["fragmentos"]),
                OrigenRevision.entries.firstOrNull { it.name == datos["origen"] },
            )
        } else {
            ResultadoPublicacion.Publicada
        }
    }

    override suspend fun borrar(codigo: String) {
        val uid = auth.currentUser?.uid ?: throw ErrorPublicacion(TipoErrorPublicacion.SIN_SESION)
        try {
            db.document("centros/$codigo/valoraciones/$uid").delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error borrando", e)
            throw ErrorPublicacion(TipoErrorPublicacion.OTRO)
        }
    }

    override suspend fun denunciar(codigo: String, autorUid: String, motivo: MotivoDenuncia) {
        val datos = mapOf("codigo" to codigo, "autorUid" to autorUid, "motivo" to motivo.name)
        llamadas.llamar(
            "denunciarValoracion", datos,
            mensajeIncierto = "Se ha cortado la conexión y no sabemos si se ha enviado la denuncia. Inténtalo de nuevo cuando vuelvas a tener red.",
        ) {
            // Reporting is idempotent: sending it again tells us. "Ya has denunciado" = it had arrived.
            try {
                functions.getHttpsCallable("denunciarValoracion").call(datos).await()
                Comprobacion.Hecha()
            } catch (e: FirebaseFunctionsException) {
                if (e.code == FirebaseFunctionsException.Code.ALREADY_EXISTS) Comprobacion.Hecha() else throw e
            }
        }
    }

    private companion object {
        const val TAG = "Valoraciones"
    }
}
