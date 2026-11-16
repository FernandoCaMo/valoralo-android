package com.valoralo.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoNotaTest {

    @Test
    fun `una cifra decimal con coma`() {
        assertEquals("4,2", formatearNota(4.2))
        assertEquals("4,0", formatearNota(4.0))
        assertEquals("3,5", formatearNota(3.5))
    }

    @Test
    fun `redondea la mitad hacia arriba`() {
        assertEquals("4,3", formatearNota(4.25))
        assertEquals("3,7", formatearNota(3.6666))
        assertEquals("5,0", formatearNota(4.96))
    }

    @Test
    fun `sin nota muestra guion`() {
        assertEquals("–", formatearNota(null))
    }

    @Test
    fun `numero de resenas en singular y plural`() {
        assertEquals("sin reseñas", textoResenas(0))
        assertEquals("1 reseña", textoResenas(1))
        assertEquals("124 reseñas", textoResenas(124))
        assertEquals("1.240 reseñas", textoResenas(1240))
    }
}
