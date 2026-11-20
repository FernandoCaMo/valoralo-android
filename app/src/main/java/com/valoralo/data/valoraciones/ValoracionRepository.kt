package com.valoralo.data.valoraciones

import com.valoralo.data.red.MENSAJE_ERROR_GENERICO
import com.valoralo.data.red.MENSAJE_SIN_CONEXION
import com.valoralo.data.remote.firestore.aFragmentos
import com.valoralo.domain.model.Fragmento
import com.valoralo.domain.model.OrigenRevision
import com.valoralo.domain.model.Valoracion
import com.valoralo.domain.usecase.FormularioValoracion
import kotlinx.coroutines.flow.Flow

sealed interface ResultadoPublicacion {
    data object Publicada : ResultadoPublicacion

    /**
     * Saved but hidden until a moderator approves it. Only when [origen] is FILTRO does
     * correcting the marked fragments publish it directly.
     */
    data class EnRevision(val fragmentos: List<Fragmento>, val origen: OrigenRevision?) : ResultadoPublicacion
}

enum class TipoErrorPublicacion(val mensaje: String) {
    TEXTO_NO_PERMITIDO("Tu texto tiene nombres de personas, insultos o datos personales. Corrígelo para publicar."),
    DATOS_NO_VALIDOS("Revisa los datos de la valoración."),
    SIN_SESION("Inicia sesión para continuar."),
    /** No network before sending: nothing was sent. */
    SIN_CONEXION(MENSAJE_SIN_CONEXION),
    /** The connection was lost while sending and, once back, we checked: it wasn't done. */
    CORTE_SIN_HACER("Se ha cortado la conexión y no se ha completado. Inténtalo de nuevo."),
    /** The connection was lost while sending and we couldn't check whether it was done (callers give a specific text). */
    INCIERTO("Se ha cortado la conexión y no sabemos si se ha completado. Compruébalo cuando vuelvas a tener red."),
    YA_DENUNCIADA("Ya has denunciado esta reseña."),
    CONDICIONES("Acepta las condiciones de uso antes de publicar."),
    OTRO(MENSAJE_ERROR_GENERICO),
}

class ErrorPublicacion(
    val tipo: TipoErrorPublicacion,
    mensaje: String = tipo.mensaje,
    val fragmentos: List<Fragmento> = emptyList(),
    /** Form field the server rejected ("alias"…), when it says so. */
    val campo: String? = null,
) : Exception(mensaje)

enum class MotivoDenuncia(val etiqueta: String) {
    INSULTOS("Insultos o faltas de respeto"),
    NOMBRE_PROPIO("Nombra a un profesor u otra persona"),
    DATOS_PERSONALES("Contiene datos personales"),
    FALSA_O_SPAM("Es falsa o spam"),
    /** Reports the author, not only this review (Play's user-generated content policy). */
    USUARIO_ABUSIVO("El autor publica de forma abusiva"),
    OTRO("Otro motivo"),
}

/**
 * Maps a callable error (FirebaseFunctionsException code, message, details) to [ErrorPublicacion].
 * The server's message is shown only when our functions marked it as written for the user
 * (`details.paraUsuario`); Firebase's own errors ("NOT_FOUND", "Unauthenticated"…) get our text.
 */
fun errorDeFuncion(codigo: String, mensajeServidor: String?, detalles: Any?): ErrorPublicacion {
    val d = detalles as? Map<*, *>
    val mensaje = mensajeServidor?.takeIf { d?.get("paraUsuario") == true && it.isNotBlank() }
    return when (codigo) {
        "INVALID_ARGUMENT" -> if (d?.get("motivo") == "FILTRO") {
            ErrorPublicacion(TipoErrorPublicacion.TEXTO_NO_PERMITIDO, fragmentos = aFragmentos(d["fragmentos"]))
        } else {
            ErrorPublicacion(
                TipoErrorPublicacion.DATOS_NO_VALIDOS, mensaje ?: TipoErrorPublicacion.DATOS_NO_VALIDOS.mensaje,
                campo = d?.get("campo") as? String,
            )
        }
        "UNAUTHENTICATED" -> ErrorPublicacion(TipoErrorPublicacion.SIN_SESION)
        "ALREADY_EXISTS" -> ErrorPublicacion(TipoErrorPublicacion.YA_DENUNCIADA, mensaje ?: TipoErrorPublicacion.YA_DENUNCIADA.mensaje)
        "UNAVAILABLE", "DEADLINE_EXCEEDED" -> ErrorPublicacion(TipoErrorPublicacion.SIN_CONEXION)
        "FAILED_PRECONDITION" if d?.get("motivo") == "CONDICIONES" -> ErrorPublicacion(TipoErrorPublicacion.CONDICIONES)
        // Messages written by our functions are already in Spanish.
        "FAILED_PRECONDITION", "NOT_FOUND", "RESOURCE_EXHAUSTED", "PERMISSION_DENIED" ->
            ErrorPublicacion(TipoErrorPublicacion.OTRO, mensaje ?: TipoErrorPublicacion.OTRO.mensaje)
        else -> ErrorPublicacion(TipoErrorPublicacion.OTRO)
    }
}

/** The user's own reviews. Writes go through Cloud Functions (filter, role); deletes are direct. */
interface ValoracionRepository {
    /** The signed-in user's review of this centro, in any state (null = none or no session). */
    fun observarMiValoracion(codigo: String): Flow<Valoracion?>

    /** Throws [ErrorPublicacion]. */
    suspend fun publicar(codigo: String, formulario: FormularioValoracion): ResultadoPublicacion

    suspend fun borrar(codigo: String)

    /** Throws [ErrorPublicacion]. */
    suspend fun denunciar(codigo: String, autorUid: String, motivo: MotivoDenuncia)
}
