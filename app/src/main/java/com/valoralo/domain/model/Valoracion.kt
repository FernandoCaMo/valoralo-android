package com.valoralo.domain.model

import java.time.LocalDate

enum class Rol { DOCENTE, ESTUDIANTE, OTRO }

enum class EstadoValoracion { PUBLICADA, EN_REVISION, OCULTA }

/**
 * One review per user and centro: `centros/{codigo}/valoraciones/{uid}` (doc 02).
 * Teachers are rated by department, never by person.
 */
data class Valoracion(
    val uid: String,
    val alias: String,
    val rolAutor: Rol,
    val verificado: Boolean,
    val fecha: LocalDate,
    val editada: Boolean = false,
    val centro: NotaCentro,
    val instalaciones: NotaInstalaciones,
    val profesorado: List<NotaDepartamento> = emptyList(),
    val texto: String,
    val estado: EstadoValoracion = EstadoValoracion.PUBLICADA,
    /** Why it is under review (only visible to its author). */
    val revision: Revision? = null,
    /** Reason shown to the author when a moderator hides the review. */
    val motivoModeracion: String? = null,
    /** Written only by the backend (alCambiarLike). */
    val nLikes: Int = 0,
) {
    /** Shows the "Docente verificado" badge. */
    val esDocenteVerificado: Boolean get() = rolAutor == Rol.DOCENTE && verificado
}

enum class OrigenRevision { FILTRO, DENUNCIAS, MODERACION }

data class Revision(val origen: OrigenRevision, val fragmentos: List<Fragmento>)

/** A fragment marked by the text filter; [inicio]/[fin] index the review text (-1 = department name). */
data class Fragmento(val inicio: Int, val fin: Int, val texto: String, val motivo: String) {
    val explicacion: String
        get() = when (motivo) {
            "INSULTO" -> "insulto"
            "INSULTO_DUDOSO" -> "posible insulto"
            "NOMBRE_PROPIO" -> "nombre de una persona"
            "NOMBRE_DUDOSO" -> "posible nombre de una persona"
            "DATO_PERSONAL" -> "dato personal (correo, teléfono o enlace)"
            else -> "texto no permitido"
        }
}

data class NotaCentro(
    val nota: Int,
    val gestion: Int? = null,
    val convivencia: Int? = null,
    val comunicacion: Int? = null,
)

data class NotaInstalaciones(
    val nota: Int,
    val aulas: Int? = null,
    val equipamiento: Int? = null,
    val limpieza: Int? = null,
    val accesibilidad: Int? = null,
)

data class NotaDepartamento(
    val departamento: String,
    val nota: Int,
)
