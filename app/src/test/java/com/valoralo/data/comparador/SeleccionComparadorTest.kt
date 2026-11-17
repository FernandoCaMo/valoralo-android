package com.valoralo.data.comparador

import org.junit.Assert.assertEquals
import org.junit.Test

class SeleccionComparadorTest {

    @Test
    fun `anade, quita y se llena en 3`() {
        val s = SeleccionComparador()
        assertEquals(ResultadoSeleccion.ANADIDO, s.alternar("1"))
        assertEquals(ResultadoSeleccion.ANADIDO, s.alternar("2"))
        assertEquals(ResultadoSeleccion.ANADIDO, s.alternar("3"))
        assertEquals(ResultadoSeleccion.LLENO, s.alternar("4"))
        assertEquals(listOf("1", "2", "3"), s.seleccion.value)
        assertEquals(ResultadoSeleccion.QUITADO, s.alternar("2"))
        assertEquals(listOf("1", "3"), s.seleccion.value)
        s.vaciar()
        assertEquals(emptyList<String>(), s.seleccion.value)
    }
}
