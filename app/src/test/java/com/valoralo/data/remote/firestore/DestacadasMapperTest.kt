package com.valoralo.data.remote.firestore

import com.valoralo.domain.model.Rol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class DestacadasMapperTest {

    private val fecha = Date.from(LocalDate.of(2026, 11, 2).atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant())

    private fun resena(codigo: String, rol: String = "DOCENTE") = mapOf(
        "clave" to "abc", "codigo" to codigo, "nombre" to "IES Delgado Hernández", "municipio" to "Bollullos Par del Condado",
        "tipo" to "IES", "alias" to "Profe Mates", "rolAutor" to rol, "verificado" to true, "nota" to 3.7,
        "extracto" to "Buen ambiente…", "fecha" to fecha,
    )

    @Test
    fun `convierte el documento de destacadas`() {
        val d = mapOf("resenas" to listOf(resena("21000693")), "minimoParaMostrar" to 3L).aDestacadas()
        assertEquals(3, d.minimoParaMostrar)
        val r = d.resenas.single()
        assertEquals("21000693", r.codigo)
        assertEquals("IES Delgado Hernández", r.nombreCentro)
        assertEquals(Rol.DOCENTE, r.rol)
        assertTrue(r.verificado)
        assertEquals(3.7, r.nota, 0.0)
        assertEquals(LocalDate.of(2026, 11, 2), r.fecha)
        assertFalse(d.mostrarCarrusel)
    }

    @Test
    fun `descarta entradas incompletas y respeta el minimo para mostrar`() {
        val d = mapOf(
            "resenas" to listOf(resena("1"), resena("2"), resena("3", rol = "RARO"), mapOf("codigo" to "4"), "basura"),
            "minimoParaMostrar" to 2L,
        ).aDestacadas()
        assertEquals(listOf("1", "2"), d.resenas.map { it.codigo })
        assertTrue(d.mostrarCarrusel)
    }

    @Test
    fun `sin documento no hay carrusel`() {
        val d = emptyMap<String, Any?>().aDestacadas()
        assertEquals(3, d.minimoParaMostrar)
        assertFalse(d.mostrarCarrusel)
    }
}
