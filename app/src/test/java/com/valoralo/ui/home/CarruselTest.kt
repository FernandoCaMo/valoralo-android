package com.valoralo.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarruselTest {

    @Test
    fun `gira solo salvo si lo han tocado, hay menos de dos o la accesibilidad lo pide`() {
        assertTrue(debeRotarSolo(5, tocado = false, animacionesDesactivadas = false, lectorDePantalla = false))
        assertFalse(debeRotarSolo(5, tocado = true, animacionesDesactivadas = false, lectorDePantalla = false))
        assertFalse(debeRotarSolo(5, tocado = false, animacionesDesactivadas = true, lectorDePantalla = false))
        assertFalse(debeRotarSolo(5, tocado = false, animacionesDesactivadas = false, lectorDePantalla = true))
        assertFalse(debeRotarSolo(1, tocado = false, animacionesDesactivadas = false, lectorDePantalla = false))
    }

    @Test
    fun `empieza en la primera resena y siempre avanza`() {
        val n = 12
        val inicio = paginaInicial(n)
        assertEquals(0, resenaDePagina(inicio, n))
        assertEquals(11, resenaDePagina(inicio + 11, n))
        assertEquals(0, resenaDePagina(inicio + 12, n))
        assertEquals(11, resenaDePagina(inicio - 1, n))
        assertTrue(inicio in 1 until paginasVirtuales(n))
    }

    @Test
    fun `con una sola resena no hay paginas virtuales`() {
        assertEquals(1, paginasVirtuales(1))
        assertEquals(0, paginaInicial(1))
        assertEquals(0, resenaDePagina(0, 1))
    }

    @Test
    fun `nueve segundos por resena`() {
        assertEquals(9_000L, MS_POR_RESENA)
    }
}
