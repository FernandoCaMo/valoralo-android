package com.valoralo.data.red

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.valoralo.data.valoraciones.ErrorPublicacion
import com.valoralo.data.valoraciones.TipoErrorPublicacion
import com.valoralo.data.valoraciones.errorDeFuncion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** What a check after a lost connection found out (QA 1.0.5, findings 11 and 12). */
sealed interface Comprobacion {
    /** The action reached the server; [resultado] stands in for the call's answer. */
    data class Hecha(val resultado: Any? = null) : Comprobacion
    /** It didn't: nothing was saved. */
    data object NoHecha : Comprobacion
}

/**
 * Calls to Cloud Functions with honest messages when the network fails:
 *  - no network before calling → nothing is sent: "Sin conexión…";
 *  - the connection is lost during the call → wait up to [ESPERA_RED_MS] for it to come back and ask
 *    `comprobar` whether the action happened: done → success; not done → "no se ha podido… inténtalo de
 *    nuevo"; can't tell (still offline, or the check fails) → a message saying we don't know and how to check.
 * Other errors keep their mapping ([errorDeFuncion]).
 */
@Singleton
class LlamadasFunciones @Inject constructor(
    private val functions: FirebaseFunctions,
    private val conectividad: Conectividad,
) {
    suspend fun llamar(
        nombre: String,
        datos: Map<String, Any?>,
        mensajeIncierto: String = TipoErrorPublicacion.INCIERTO.mensaje,
        comprobar: (suspend () -> Comprobacion)? = null,
    ): Any? = llamarConComprobacion(
        conectado = { conectividad.conectado.value },
        esperarRed = { withTimeoutOrNull(ESPERA_RED_MS) { conectividad.conectado.first { it } } != null },
        llamada = { functions.getHttpsCallable(nombre).call(datos).await().data },
        comprobar = comprobar,
        mensajeIncierto = mensajeIncierto,
    )

    companion object {
        const val ESPERA_RED_MS = 20_000L
    }
}

/** Testable core of [LlamadasFunciones.llamar]. Throws [ErrorPublicacion]. */
internal suspend fun llamarConComprobacion(
    conectado: () -> Boolean,
    esperarRed: suspend () -> Boolean,
    llamada: suspend () -> Any?,
    comprobar: (suspend () -> Comprobacion)?,
    mensajeIncierto: String,
): Any? {
    if (!conectado()) throw ErrorPublicacion(TipoErrorPublicacion.SIN_CONEXION)
    try {
        return llamada()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (!esFalloDeRed(e, conectado())) {
            throw (e as? FirebaseFunctionsException)?.let { errorDeFuncion(it.code.name, it.message, it.details) }
                ?: (e as? ErrorPublicacion)
                ?: ErrorPublicacion(TipoErrorPublicacion.OTRO)
        }
        // The request may or may not have reached the server.
        if (comprobar == null) throw ErrorPublicacion(TipoErrorPublicacion.INCIERTO, mensajeIncierto)
        val comprobado = if (esperarRed()) runCatching { comprobar() }.getOrNull() else null
        when (comprobado) {
            is Comprobacion.Hecha -> return comprobado.resultado
            Comprobacion.NoHecha -> throw ErrorPublicacion(TipoErrorPublicacion.CORTE_SIN_HACER)
            null -> throw ErrorPublicacion(TipoErrorPublicacion.INCIERTO, mensajeIncierto)
        }
    }
}

/**
 * True when the failure is the network's (the request may not have arrived, or its answer was lost),
 * not an answer from our functions.
 */
internal fun esFalloDeRed(e: Throwable, conectado: Boolean): Boolean {
    val cadena = generateSequence(e) { it.cause?.takeIf { c -> c !== it } }.take(8).toList()
    if (cadena.any { it is FirebaseNetworkException || it is IOException }) return true
    val funcion = cadena.filterIsInstance<FirebaseFunctionsException>().firstOrNull()
    if (funcion != null) {
        val codigo = funcion.code.name
        return codigo in CODIGOS_DE_RED || (!conectado && codigo in CODIGOS_SIN_RESPUESTA)
    }
    return cadena.none { it is ErrorPublicacion } && !conectado
}

// By name: the SDK's enum is only touched when there is a FirebaseFunctionsException.
private val CODIGOS_DE_RED = setOf("UNAVAILABLE", "DEADLINE_EXCEEDED")
private val CODIGOS_SIN_RESPUESTA = setOf("INTERNAL", "UNKNOWN")
