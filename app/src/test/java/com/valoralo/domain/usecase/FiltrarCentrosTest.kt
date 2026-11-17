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
        localidad: String = municipio,
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
    private val roquetas = centro("5", "IES Roquetas", "Roquetas de Mar", "Almería")
    private val gines = centro("6", "CEIP El Majuelo", "Gines", "Sevilla", tipo = "CEIP")
    private val bormujos = centro("7", "IES Juan Ciudad Duarte", "Bormujos", "Sevilla")
    // A centro named after San Ginés, in another provincia.
    private val sanGines = centro("8", "CEIP San Ginés", "Purchena", "Almería", tipo = "CEIP")
    private val todos = listOf(sevilla, dosHermanas, malaga, aguadulce, roquetas, gines, bormujos, sanGines)

    private val lugarGines = Lugar.municipio("Gines", "Sevilla")
    private val lugarSevilla = Lugar.municipio("Sevilla", "Sevilla")
    private val lugarRoquetas = Lugar.municipio("Roquetas de Mar", "Almería")
    private val lugarAguadulce = Lugar.localidad("Aguadulce", "Roquetas de Mar", "Almería")

    @Test
    fun `sin texto ni filtros devuelve todos`() {
        assertEquals(todos, todos.filtrar("", FiltroCentros()))
    }

    @Test
    fun `busca por nombre, municipio y localidad sin distinguir mayusculas ni tildes`() {
        assertEquals(listOf(dosHermanas), todos.filtrar("  VELÁZQUEZ ", FiltroCentros()))
        assertEquals(listOf(dosHermanas), todos.filtrar("dos herm", FiltroCentros()))
        assertEquals(listOf(aguadulce), todos.filtrar("aguadulce", FiltroCentros()))
        assertEquals(listOf(bormujos), todos.filtrar("Bormujos", FiltroCentros()))
    }

    @Test
    fun `los centros del municipio buscado van antes que los que solo lo llevan en el nombre`() {
        assertEquals(listOf(gines, sanGines), todos.filtrar("Gines", FiltroCentros()))
        assertEquals(listOf(gines, sanGines), todos.filtrar("ginés", FiltroCentros()))
    }

    @Test
    fun `seleccion multiple dentro de cada filtro, y los filtros se combinan`() {
        assertEquals(listOf(sevilla, dosHermanas, malaga, gines, bormujos), todos.filtrar("", FiltroCentros(provincias = setOf("Sevilla", "Málaga"))))
        assertEquals(listOf(sevilla, gines), todos.filtrar("", FiltroCentros(lugares = setOf(lugarSevilla, lugarGines))))
        assertEquals(listOf(malaga, aguadulce, gines, sanGines), todos.filtrar("", FiltroCentros(tipos = setOf("CEIP"))))
        assertEquals(listOf(sevilla, dosHermanas), todos.filtrar("", FiltroCentros(niveles = setOf("Bachillerato", "FP Grado Medio"))))
        assertEquals(listOf(gines), todos.filtrar("", FiltroCentros(provincias = setOf("Sevilla"), tipos = setOf("CEIP"))))
        assertEquals(emptyList<Centro>(), todos.filtrar("guadalquivir", FiltroCentros(provincias = setOf("Málaga"))))
    }

    @Test
    fun `un municipio incluye sus localidades y una localidad solo la suya`() {
        assertEquals(listOf(aguadulce, roquetas), todos.filtrar("", FiltroCentros(lugares = setOf(lugarRoquetas))))
        assertEquals(listOf(aguadulce), todos.filtrar("", FiltroCentros(lugares = setOf(lugarAguadulce))))
    }

    @Test
    fun `etiqueta con su contexto`() {
        assertEquals("Gines · Sevilla", lugarGines.etiqueta)
        assertEquals("Aguadulce · Roquetas de Mar, Almería", lugarAguadulce.etiqueta)
    }

    @Test
    fun `cuenta los filtros activos`() {
        assertEquals(0, FiltroCentros().activos)
        assertFalse(FiltroCentros().hayActivos)
        val filtro = FiltroCentros(provincias = setOf("Sevilla"), tipos = setOf("IES", "CEIP"), lugares = setOf(lugarGines))
        assertEquals(4, filtro.activos)
        assertTrue(filtro.hayActivos)
    }

    @Test
    fun `los lugares son los municipios y las localidades distintas de su municipio, de lo elegido arriba`() {
        val sinNada = opcionesFiltro(todos, FiltroCentros()).lugares
        assertEquals(
            listOf(lugarAguadulce, Lugar.municipio("Bormujos", "Sevilla"), Lugar.municipio("Dos Hermanas", "Sevilla"), lugarGines,
                Lugar.municipio("Málaga", "Málaga"), Lugar.municipio("Purchena", "Almería"), lugarRoquetas, lugarSevilla),
            sinNada,
        )
        val enSevilla = opcionesFiltro(todos, FiltroCentros(provincias = setOf("Sevilla"))).lugares
        assertTrue(lugarGines in enSevilla && Lugar.municipio("Bormujos", "Sevilla") in enSevilla)
        assertFalse(lugarAguadulce in enSevilla)

        val enRoquetas = opcionesFiltro(todos, FiltroCentros(lugares = setOf(lugarRoquetas)))
        assertEquals(listOf("CEIP", "IES"), enRoquetas.tipos)
        assertEquals(listOf("Infantil", "Primaria", "ESO", "Bachillerato", "FP Grado Medio"), opcionesFiltro(todos, FiltroCentros()).niveles)
    }

    @Test
    fun `sugerencias con lo que empieza por el texto primero, municipios y localidades juntos`() {
        val lugares = opcionesFiltro(todos, FiltroCentros()).lugares
        assertEquals(listOf(lugarGines), lugares.sugerir("gin"))
        assertEquals(listOf(lugarAguadulce), lugares.sugerir("AGUA"))
        // "Roquetas de Mar" starts with r; "Dos Hermanas", "Purchena"… only contain it.
        val r = lugares.sugerir("r")
        assertEquals(lugarRoquetas, r.first())
        assertTrue(Lugar.municipio("Dos Hermanas", "Sevilla") in r)
        // A word inside the name ("mar" in Roquetas de Mar) ranks before a mere substring.
        assertEquals(lugarRoquetas, lugares.sugerir("mar").first())
        assertEquals(lugares, lugares.sugerir(""))
    }

    @Test
    fun `al cambiar la provincia se quitan los lugares que ya no encajan`() {
        val filtro = FiltroCentros(provincias = setOf("Sevilla", "Almería"), lugares = setOf(lugarGines, lugarAguadulce))
        assertEquals(
            FiltroCentros(provincias = setOf("Almería"), lugares = setOf(lugarAguadulce)),
            filtro.copy(provincias = setOf("Almería")).coherente(),
        )
        assertEquals(FiltroCentros(lugares = setOf(lugarGines)), FiltroCentros(lugares = setOf(lugarGines)).coherente())
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
    fun `la provincia se deduce del municipio o la localidad elegidos`() {
        assertEquals(setOf("Sevilla"), FiltroCentros(lugares = setOf(lugarGines, lugarSevilla)).provinciasDeducidas())
        assertEquals(setOf("Sevilla", "Almería"), FiltroCentros(lugares = setOf(lugarGines, lugarAguadulce)).provinciasDeducidas())
        assertEquals(emptySet<String>(), FiltroCentros(provincias = setOf("Málaga"), lugares = setOf(lugarGines)).provinciasDeducidas())
        assertEquals(emptySet<String>(), FiltroCentros().provinciasDeducidas())
    }
}
