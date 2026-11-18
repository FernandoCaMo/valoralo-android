package com.valoralo.ui.detail

import com.valoralo.domain.usecase.FiltroRol
import org.junit.Assert.assertEquals
import org.junit.Test

class TextoSinResenasTest {
    @Test
    fun `sin reseña propia`() {
        assertEquals("Aún no hay reseñas.", textoSinResenas(FiltroRol.TODOS, tengoPropia = false))
        assertEquals("Aún no hay reseñas de docentes.", textoSinResenas(FiltroRol.DOCENTES, tengoPropia = false))
    }

    @Test
    fun `con la reseña propia como unica`() {
        assertEquals("Aún no hay reseñas de otras personas.", textoSinResenas(FiltroRol.TODOS, tengoPropia = true))
        assertEquals("Aún no hay otras reseñas de estudiantes.", textoSinResenas(FiltroRol.ESTUDIANTES, tengoPropia = true))
    }
}
