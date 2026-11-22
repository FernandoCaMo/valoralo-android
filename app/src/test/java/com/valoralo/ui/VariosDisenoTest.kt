package com.valoralo.ui

import com.valoralo.ui.components.etiquetaNota
import com.valoralo.ui.detail.indiceDeResenas
import com.valoralo.ui.search.textoCentros
import com.valoralo.ui.theme.Apariencia
import com.valoralo.ui.theme.Tema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Small pieces of the 1.0.8 design round. */
class VariosDisenoTest {

    @Test
    fun etiquetasDeLasEstrellas() {
        assertEquals(listOf("Muy mal", "Mal", "Regular", "Bien", "Excelente"), (1..5).map(::etiquetaNota))
    }

    @Test
    fun verReseñasSaltaAlTituloDeReseñas() {
        // cabecera, valoraciones, notas, información, oferta → "Reseñas" is item 5…
        assertEquals(5, indiceDeResenas(hayDepartamentos = false, hayPropia = false))
        // …plus "Profesorado por departamento" and "Tu valoración" (title + card).
        assertEquals(8, indiceDeResenas(hayDepartamentos = true, hayPropia = true))
    }

    @Test
    fun numeroDeResultados() {
        assertEquals("1 centro", textoCentros(1))
        assertEquals("42 centros", textoCentros(42))
        assertEquals("3.720 centros", textoCentros(3720))
    }

    @Test
    fun temaElegidoFrenteAlDelSistema() {
        assertTrue(Apariencia.oscuro(Tema.SISTEMA, sistemaOscuro = true))
        assertFalse(Apariencia.oscuro(Tema.SISTEMA, sistemaOscuro = false))
        assertTrue(Apariencia.oscuro(Tema.OSCURO, sistemaOscuro = false))
        assertFalse(Apariencia.oscuro(Tema.CLARO, sistemaOscuro = true))
    }
}
