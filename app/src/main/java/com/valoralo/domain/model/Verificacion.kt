package com.valoralo.domain.model

import java.time.LocalDate

enum class EstadoSolicitud { PENDIENTE, APROBADA, RECHAZADA, CADUCADA }

/** verificaciones/{uid} as the user sees it (the name and the CSV are never sent back). */
data class MiVerificacion(
    val estado: EstadoSolicitud,
    val fechaSolicitud: LocalDate?,
    val motivoRechazo: String?,
)
