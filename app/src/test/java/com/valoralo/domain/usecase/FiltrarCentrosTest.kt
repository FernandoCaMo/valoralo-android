package com.valoralo.domain.usecase

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Ensenanza
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltrarCentrosTest {

    private fun centro(
        codigo: String,
        nombre: String,
        municipio: String,
        provincia: String,
        tipo: String = "IES",
        niveles: List<String> = listOf("ESO"),
    ) = Centro(
        codigo = codigo,
        nombre = nombre,
        tipo = tipo,
        direccion = "",
        cp = "",
        municipio = municipio,
        provincia = provincia,
        ensenanzas = niveles.map { Ensenanza(it, it) },
    )

    private val sevilla = centro("1", "IES Guadalquivir", "Sevilla", "Sevilla", niveles = listOf("ESO", "Bachillerato"))
    private val dosHermanas = centro("2", "IES Velázquez", "Dos Hermanas", "Sevilla", niveles = listOf("ESO", "FP Grado Medio"))
    private val malaga = centro("3", "CEIP Málaga Centro", "Málaga", "Málaga", tipo = "CEIP", niveles = listOf("Infantil", "Primaria"))
    private val todos = listOf(sevilla, dosHermanas, malaga)

    @Test
    fun `sin texto ni filtros devuelve todos`() {
        assertEquals(todos, todos.filtrar("", FiltroCentros()))
    }

    @Test
    fun `busca por nombre sin distinguir mayusculas ni tildes`() {
        assertEquals(listOf(dosHermanas), todos.filtrar("velazquez", FiltroCentros()))
        assertEquals(listOf(dosHermanas), todos.filtrar("  VELÁZQUEZ ", FiltroCentros()))
    }

    @Test
    fun `busca tambien por municipio`() {
        assertEquals(listOf(dosHermanas), todos.filtrar("dos herm", FiltroCentros()))
    }

    @Test
    fun `filtra por provincia`() {
        assertEquals(listOf(sevilla, dosHermanas), todos.filtrar("", FiltroCentros(provincia = "Sevilla")))
    }

    @Test
    fun `filtra por municipio`() {
        assertEquals(listOf(dosHermanas), todos.filtrar("", FiltroCentros(municipio = "Dos Hermanas")))
    }

    @Test
    fun `filtra por tipo`() {
        assertEquals(listOf(malaga), todos.filtrar("", FiltroCentros(tipo = "CEIP")))
    }

    @Test
    fun `filtra por nivel de ensenanza`() {
        assertEquals(listOf(sevilla), todos.filtrar("", FiltroCentros(nivel = "Bachillerato")))
    }

    @Test
    fun `combina texto y filtros`() {
        assertEquals(emptyList<Centro>(), todos.filtrar("guadalquivir", FiltroCentros(provincia = "Málaga")))
        assertEquals(listOf(sevilla), todos.filtrar("ies", FiltroCentros(nivel = "Bachillerato")))
    }

    @Test
    fun `cuenta los filtros activos`() {
        assertEquals(0, FiltroCentros().activos)
        assertFalse(FiltroCentros().hayActivos)
        val filtro = FiltroCentros(provincia = "Sevilla", tipo = "IES")
        assertEquals(2, filtro.activos)
        assertTrue(filtro.hayActivos)
    }

    @Test
    fun `cambiar de provincia borra el municipio`() {
        val filtro = FiltroCentros(provincia = "Sevilla", municipio = "Dos Hermanas")
        assertEquals(FiltroCentros(provincia = "Málaga"), filtro.conProvincia("Málaga"))
    }
}
