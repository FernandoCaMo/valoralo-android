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
) {
    /** Shows the "Docente verificado" badge. */
    val esDocenteVerificado: Boolean get() = rolAutor == Rol.DOCENTE && verificado
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
