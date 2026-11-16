package com.valoralo.data.valoraciones

import com.valoralo.data.remote.firestore.aFragmentos
import com.valoralo.domain.model.Fragmento
import com.valoralo.domain.model.Valoracion
import com.valoralo.domain.usecase.FormularioValoracion
import kotlinx.coroutines.flow.Flow

sealed interface ResultadoPublicacion {
    data object Publicada : ResultadoPublicacion

    /** Saved but hidden until a moderator approves it (or the author edits the fragments). */
    data class EnRevision(val fragmentos: List<Fragmento>) : ResultadoPublicacion
}

enum class TipoErrorPublicacion(val mensaje: String) {
    TEXTO_NO_PERMITIDO("Tu texto tiene nombres de personas, insultos o datos personales. Corrígelo para publicar."),
    DATOS_NO_VALIDOS("Revisa los datos de la valoración."),
    SIN_SESION("Inicia sesión para continuar."),
    SIN_CONEXION("No hay conexión. Inténtalo de nuevo."),
    YA_DENUNCIADA("Ya has denunciado esta reseña."),
    OTRO("No se ha podido completar. Inténtalo de nuevo."),
}

class ErrorPublicacion(
    val tipo: TipoErrorPublicacion,
    mensaje: String = tipo.mensaje,
    val fragmentos: List<Fragmento> = emptyList(),
) : Exception(mensaje)

enum class MotivoDenuncia(val etiqueta: String) {
    INSULTOS("Insultos o faltas de respeto"),
    NOMBRE_PROPIO("Nombra a un profesor u otra persona"),
    DATOS_PERSONALES("Contiene datos personales"),
    FALSA_O_SPAM("Es falsa o spam"),
    OTRO("Otro motivo"),
}

/** Maps a callable error (FirebaseFunctionsException code, message, details) to [ErrorPublicacion]. */
fun errorDeFuncion(codigo: String, mensaje: String?, detalles: Any?): ErrorPublicacion {
    val d = detalles as? Map<*, *>
    return when (codigo) {
        "INVALID_ARGUMENT" -> if (d?.get("motivo") == "FILTRO") {
            ErrorPublicacion(TipoErrorPublicacion.TEXTO_NO_PERMITIDO, fragmentos = aFragmentos(d["fragmentos"]))
        } else {
            ErrorPublicacion(TipoErrorPublicacion.DATOS_NO_VALIDOS, mensaje ?: TipoErrorPublicacion.DATOS_NO_VALIDOS.mensaje)
        }
        "UNAUTHENTICATED" -> ErrorPublicacion(TipoErrorPublicacion.SIN_SESION)
        "ALREADY_EXISTS" -> ErrorPublicacion(TipoErrorPublicacion.YA_DENUNCIADA)
        "UNAVAILABLE", "DEADLINE_EXCEEDED" -> ErrorPublicacion(TipoErrorPublicacion.SIN_CONEXION)
        // Messages written by our functions are already in Spanish.
        "FAILED_PRECONDITION", "NOT_FOUND" -> ErrorPublicacion(TipoErrorPublicacion.OTRO, mensaje ?: TipoErrorPublicacion.OTRO.mensaje)
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
