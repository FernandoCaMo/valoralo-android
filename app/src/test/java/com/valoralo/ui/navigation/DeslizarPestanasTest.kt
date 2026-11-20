package com.valoralo.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeslizarPestanasTest {

    // Pixel values as if 1 dp = 1 px: 56 long, 24 short-but-fast, 800/s fast.
    private fun deslizar(actual: Pestana, dx: Float, dy: Float = 0f, velocidad: Float = 0f) =
        pestanaAlDeslizar(actual, dx, dy, velocidad, 56f, 24f, 800f)

    @Test
    fun haciaLaIzquierdaVaALaSiguienteYHaciaLaDerechaALaAnterior() {
        assertEquals(Pestana.BUSCAR, deslizar(Pestana.INICIO, dx = -200f))
        assertEquals(Pestana.FAVORITOS, deslizar(Pestana.BUSCAR, dx = -200f))
        assertEquals(Pestana.CUENTA, deslizar(Pestana.FAVORITOS, dx = -200f))
        assertEquals(Pestana.FAVORITOS, deslizar(Pestana.CUENTA, dx = 200f))
        assertEquals(Pestana.INICIO, deslizar(Pestana.BUSCAR, dx = 200f))
    }

    @Test
    fun enLosExtremosNoHayMasPestanas() {
        assertNull(deslizar(Pestana.INICIO, dx = 200f))
        assertNull(deslizar(Pestana.CUENTA, dx = -200f))
    }

    @Test
    fun unGestoCortoYLentoNoCambia() {
        assertNull(deslizar(Pestana.INICIO, dx = -40f, velocidad = -100f))
        assertNull(deslizar(Pestana.INICIO, dx = -10f, velocidad = -3000f)) // a flick shorter than 24
    }

    @Test
    fun unGestoCortoPeroRapidoCambia() {
        assertEquals(Pestana.BUSCAR, deslizar(Pestana.INICIO, dx = -30f, velocidad = -1500f))
    }

    @Test
    fun laVelocidadDebeIrEnLaMismaDireccion() {
        assertNull(deslizar(Pestana.BUSCAR, dx = -30f, velocidad = 1500f))
    }

    @Test
    fun unGestoDiagonalOVerticalNoCambia() {
        assertNull(deslizar(Pestana.INICIO, dx = -200f, dy = 150f)) // vertical scroll with some sideways
        assertNull(deslizar(Pestana.INICIO, dx = -100f, dy = 50f)) // exactly 2:1 is not enough
        assertEquals(Pestana.BUSCAR, deslizar(Pestana.INICIO, dx = -101f, dy = 50f))
        assertNull(deslizar(Pestana.INICIO, dx = 0f))
    }

    @Test
    fun losBordesDelGestoAtrasSeDejanAlSistema() {
        assertTrue(empiezaEnBordeDelSistema(x = 10f, ancho = 1080f, bordeIzquierdo = 66f, bordeDerecho = 66f))
        assertTrue(empiezaEnBordeDelSistema(x = 1070f, ancho = 1080f, bordeIzquierdo = 66f, bordeDerecho = 66f))
        assertFalse(empiezaEnBordeDelSistema(x = 540f, ancho = 1080f, bordeIzquierdo = 66f, bordeDerecho = 66f))
        // Three-button navigation: no back gesture, the whole width counts.
        assertFalse(empiezaEnBordeDelSistema(x = 1f, ancho = 1080f, bordeIzquierdo = 0f, bordeDerecho = 0f))
    }
}
