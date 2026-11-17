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
    fun `boton de los filtros con el numero de centros`() {
        assertEquals("Ver 0 centros", textoVerCentros(0))
        assertEquals("Ver 1 centro", textoVerCentros(1))
        assertEquals("Ver 3.711 centros", textoVerCentros(3711))
    }

    @Test
    fun `numero de resenas en singular y plural`() {
        assertEquals("sin reseñas", textoResenas(0))
        assertEquals("1 reseña", textoResenas(1))
        assertEquals("124 reseñas", textoResenas(124))
        assertEquals("1.240 reseñas", textoResenas(1240))
    }
}
