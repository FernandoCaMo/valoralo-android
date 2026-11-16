package com.valoralo.data.verificacion

import com.google.firebase.Timestamp
import com.valoralo.domain.model.EstadoSolicitud
import com.valoralo.domain.model.MiVerificacion
import java.time.ZoneId
import java.util.Date

/** verificaciones/{uid} → [MiVerificacion]; null if there is none or it is not understood. */
fun miVerificacionDesde(datos: Map<String, Any?>?): MiVerificacion? {
    if (datos == null) return null
    val estado = EstadoSolicitud.entries.firstOrNull { it.name == datos["estado"] } ?: return null
    val fecha = when (val f = datos["fechaSolicitud"]) {
        is Date -> f
        is Timestamp -> f.toDate()
        else -> null
    }
    return MiVerificacion(
        estado = estado,
        fechaSolicitud = fecha?.toInstant()?.atZone(ZoneId.of("Europe/Madrid"))?.toLocalDate(),
        motivoRechazo = datos["motivoRechazo"] as? String,
    )
}
