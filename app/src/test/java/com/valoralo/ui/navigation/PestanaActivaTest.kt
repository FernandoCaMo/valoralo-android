package com.valoralo.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class PestanaActivaTest {
    // The back stack starts with the graph itself (route null).
    private fun pila(vararg rutas: String) = listOf<String?>(null, *rutas)

    @Test
    fun `un centro abierto desde Buscar es de Buscar`() {
        assertEquals(Rutas.SEARCH, rutaPestanaDe(pila(Rutas.HOME, Rutas.SEARCH, Rutas.CENTRO)))
        assertEquals(Rutas.SEARCH, rutaPestanaDe(pila(Rutas.HOME, Rutas.SEARCH, Rutas.COMPARAR, Rutas.CENTRO)))
    }

    @Test
    fun `un centro abierto desde Inicio o Favoritos es de esa pestana`() {
        assertEquals(Rutas.HOME, rutaPestanaDe(pila(Rutas.HOME, Rutas.CENTRO, Rutas.VALORAR)))
        assertEquals(Rutas.FAVORITOS, rutaPestanaDe(pila(Rutas.HOME, Rutas.FAVORITOS, Rutas.CENTRO)))
    }

    @Test
    fun `las pantallas de cuenta son de la pestana desde la que se abren`() {
        assertEquals(Rutas.CUENTA, rutaPestanaDe(pila(Rutas.HOME, Rutas.CUENTA, Rutas.LOGIN, Rutas.REGISTRO)))
        assertEquals(Rutas.SEARCH, rutaPestanaDe(pila(Rutas.HOME, Rutas.SEARCH, Rutas.LOGIN)))
        assertEquals(Rutas.CUENTA, rutaPestanaDe(pila(Rutas.HOME, Rutas.CUENTA, Rutas.ACERCA_DE)))
    }

    @Test
    fun `pila vacia`() {
        assertEquals(null, rutaPestanaDe(emptyList()))
    }
}
