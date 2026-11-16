package com.valoralo.domain.usecase

import com.valoralo.data.verificacion.miVerificacionDesde
import com.valoralo.domain.model.EstadoSolicitud
import com.valoralo.domain.model.MiVerificacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class SolicitudVerificacionTest {

    @Test
    fun `nombre y apellidos`() {
        assertNull(validarNombreDeclarado("Lucía Ruiz Pérez"))
        assertNull(validarNombreDeclarado("María José O'Neill-García"))
        assertEquals(ErrorSolicitud.NOMBRE, validarNombreDeclarado("Lucía"))
        assertEquals(ErrorSolicitud.NOMBRE, validarNombreDeclarado("Lucía 123"))
        assertEquals(ErrorSolicitud.NOMBRE, validarNombreDeclarado(""))
    }

    @Test
    fun `codigo CSV como en el servidor`() {
        assertEquals("VH5DPNPY5AXNHX4H6", normalizarCsv(" vh5dp-nPy5 axnh.x4h6 "))
        assertNull(validarCsv("VH5DP NPY5 AXNH X4H6"))
        assertEquals(ErrorSolicitud.CSV, validarCsv("ABC123"))
        assertEquals(ErrorSolicitud.CSV, validarCsv("VH5DP<script>12345"))
    }

    @Test
    fun `pdf opcional de hasta 5 MB`() {
        assertNull(validarPdf(null))
        assertNull(validarPdf(4_999_999L))
        assertEquals(ErrorSolicitud.PDF_GRANDE, validarPdf(5L * 1024 * 1024))
    }

    @Test
    fun `estado de mi solicitud desde Firestore`() {
        val fecha = Date.from(LocalDate.of(2026, 11, 17).atStartOfDay(ZoneId.of("Europe/Madrid")).plusHours(9).toInstant())
        assertEquals(
            MiVerificacion(EstadoSolicitud.RECHAZADA, LocalDate.of(2026, 11, 17), "El nombre no coincide."),
            miVerificacionDesde(mapOf("estado" to "RECHAZADA", "fechaSolicitud" to fecha, "motivoRechazo" to "El nombre no coincide.", "csvHash" to "abc")),
        )
        assertEquals(EstadoSolicitud.PENDIENTE, miVerificacionDesde(mapOf("estado" to "PENDIENTE"))?.estado)
        assertNull(miVerificacionDesde(mapOf("estado" to "RARO")))
        assertNull(miVerificacionDesde(null))
    }
}
