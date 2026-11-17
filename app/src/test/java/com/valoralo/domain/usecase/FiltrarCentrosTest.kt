package com.valoralo.domain.usecase

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Ensenanza
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        localidad: String = "",
    ) = Centro(
        codigo = codigo,
        nombre = nombre,
        tipo = tipo,
        direccion = "",
        cp = "",
        localidad = localidad,
        municipio = municipio,
        provincia = provincia,
        ensenanzas = niveles.map { Ensenanza(it, it) },
    )

    private val sevilla = centro("1", "IES Guadalquivir", "Sevilla", "Sevilla", niveles = listOf("ESO", "Bachillerato"))
    private val dosHermanas = centro("2", "IES Velázquez", "Dos Hermanas", "Sevilla", niveles = listOf("ESO", "FP Grado Medio"))
    private val malaga = centro("3", "CEIP Málaga Centro", "Málaga", "Málaga", tipo = "CEIP", niveles = listOf("Infantil", "Primaria"))
    private val aguadulce = centro("4", "CEIP Torrequebrada", "Roquetas de Mar", "Almería", tipo = "CEIP", localidad = "Aguadulce")
    private val roquetas = centro("5", "IES Roquetas", "Roquetas de Mar", "Almería", localidad = "Roquetas de Mar")
    private val todos = listOf(sevilla, dosHermanas, malaga, aguadulce, roquetas)

    @Test
    fun `sin texto ni filtros devuelve todos`() {
        assertEquals(todos, todos.filtrar("", FiltroCentros()))
    }

    @Test
    fun `busca por nombre, municipio y localidad sin distinguir mayusculas ni tildes`() {
        assertEquals(listOf(dosHermanas), todos.filtrar("  VELÁZQUEZ ", FiltroCentros()))
        assertEquals(listOf(dosHermanas), todos.filtrar("dos herm", FiltroCentros()))
        assertEquals(listOf(aguadulce), todos.filtrar("aguadulce", FiltroCentros()))
    }

    @Test
    fun `seleccion multiple dentro de cada filtro, y los filtros se combinan`() {
        assertEquals(listOf(sevilla, dosHermanas, malaga), todos.filtrar("", FiltroCentros(provincias = setOf("Sevilla", "Málaga"))))
        assertEquals(listOf(sevilla, malaga), todos.filtrar("", FiltroCentros(municipios = setOf("Sevilla", "Málaga"))))
        assertEquals(listOf(aguadulce), todos.filtrar("", FiltroCentros(localidades = setOf("Aguadulce"))))
        assertEquals(listOf(malaga, aguadulce), todos.filtrar("", FiltroCentros(tipos = setOf("CEIP"))))
        assertEquals(listOf(sevilla, dosHermanas), todos.filtrar("", FiltroCentros(niveles = setOf("Bachillerato", "FP Grado Medio"))))
        assertEquals(listOf(sevilla), todos.filtrar("", FiltroCentros(provincias = setOf("Sevilla"), niveles = setOf("Bachillerato"))))
        assertEquals(emptyList<Centro>(), todos.filtrar("guadalquivir", FiltroCentros(provincias = setOf("Málaga"))))
    }

    @Test
    fun `sin localidad propia, la localidad es el municipio`() {
        assertEquals(listOf(sevilla), todos.filtrar("", FiltroCentros(localidades = setOf("Sevilla"))))
    }

    @Test
    fun `cuenta los filtros activos`() {
        assertEquals(0, FiltroCentros().activos)
        assertFalse(FiltroCentros().hayActivos)
        val filtro = FiltroCentros(provincias = setOf("Sevilla"), tipos = setOf("IES", "CEIP"))
        assertEquals(3, filtro.activos)
        assertTrue(filtro.hayActivos)
    }

    @Test
    fun `ciudad y localidad solo ofrecen lo que encaja con lo elegido arriba`() {
        val sinNada = opcionesFiltro(todos, FiltroCentros())
        assertEquals(listOf("Dos Hermanas", "Málaga", "Roquetas de Mar", "Sevilla"), sinNada.municipios)
        assertEquals(listOf("Aguadulce", "Dos Hermanas", "Málaga", "Roquetas de Mar", "Sevilla"), sinNada.localidades)

        val enSevilla = opcionesFiltro(todos, FiltroCentros(provincias = setOf("Sevilla")))
        assertEquals(listOf("Dos Hermanas", "Sevilla"), enSevilla.municipios)
        assertEquals(listOf("Dos Hermanas", "Sevilla"), enSevilla.localidades)

        val enRoquetas = opcionesFiltro(todos, FiltroCentros(municipios = setOf("Roquetas de Mar")))
        assertEquals(listOf("Aguadulce", "Roquetas de Mar"), enRoquetas.localidades)
        assertEquals(listOf("CEIP", "IES"), enRoquetas.tipos)
        assertEquals(listOf("Infantil", "Primaria", "ESO", "Bachillerato", "FP Grado Medio"), sinNada.niveles)
    }

    @Test
    fun `al cambiar lo de arriba se quitan las ciudades y localidades que ya no encajan`() {
        val filtro = FiltroCentros(
            provincias = setOf("Sevilla", "Almería"),
            municipios = setOf("Dos Hermanas", "Roquetas de Mar"),
            localidades = setOf("Dos Hermanas", "Aguadulce"),
        )
        assertEquals(
            FiltroCentros(provincias = setOf("Almería"), municipios = setOf("Roquetas de Mar"), localidades = setOf("Aguadulce")),
            filtro.copy(provincias = setOf("Almería")).coherente(todos),
        )
        assertEquals(
            FiltroCentros(municipios = setOf("Sevilla")),
            FiltroCentros(municipios = setOf("Sevilla"), localidades = setOf("Aguadulce")).coherente(todos),
        )
    }

    @Test
    fun `alternar una opcion la anade o la quita`() {
        assertEquals(setOf("IES", "CEIP"), setOf("IES").alternar("CEIP"))
        assertEquals(setOf("IES"), setOf("IES", "CEIP").alternar("CEIP"))
    }

    @Test
    fun `resumen de cada seccion cerrada`() {
        assertNull(resumenSeleccion(emptySet()))
        assertEquals("Sevilla", resumenSeleccion(setOf("Sevilla")))
        assertEquals("2", resumenSeleccion(setOf("IES", "CEIP")))
    }

    @Test
    fun `la provincia se deduce de la ciudad elegida`() {
        assertEquals(setOf("Sevilla"), FiltroCentros(municipios = setOf("Dos Hermanas", "Sevilla")).provinciasDeducidas(todos))
        assertEquals(setOf("Sevilla", "Almería"), FiltroCentros(municipios = setOf("Sevilla", "Roquetas de Mar")).provinciasDeducidas(todos))
        // With provincias chosen there is nothing to deduce.
        assertEquals(emptySet<String>(), FiltroCentros(provincias = setOf("Málaga"), municipios = setOf("Málaga")).provinciasDeducidas(todos))
        assertEquals(emptySet<String>(), FiltroCentros().provinciasDeducidas(todos))
    }

    @Test
    fun `buscador de opciones sin tildes`() {
        assertEquals(listOf("Málaga"), listOf("Dos Hermanas", "Málaga", "Sevilla").buscar("mala"))
        assertEquals(listOf("Dos Hermanas", "Málaga"), listOf("Dos Hermanas", "Málaga").buscar(""))
    }
}
