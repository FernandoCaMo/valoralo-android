package com.valoralo.ui.review

import com.valoralo.domain.usecase.FormularioValoracion
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HayCambiosTest {
    private val inicial = FormularioValoracion()

    @Test
    fun `sin tocar nada no hay cambios`() {
        assertFalse(ReviewUiState(cargando = false, formulario = inicial, inicial = inicial).hayCambios)
    }

    @Test
    fun `una nota o un texto nuevos son cambios`() {
        assertTrue(ReviewUiState(cargando = false, formulario = inicial.copy(notaCentro = 4), inicial = inicial).hayCambios)
        assertTrue(ReviewUiState(cargando = false, formulario = inicial.copy(texto = "Hola"), inicial = inicial).hayCambios)
    }

    @Test
    fun `cargando o ya terminado no pregunta`() {
        val cambiado = inicial.copy(notaCentro = 4)
        assertFalse(ReviewUiState(cargando = true, formulario = cambiado, inicial = inicial).hayCambios)
        assertFalse(ReviewUiState(cargando = false, formulario = cambiado, inicial = inicial, terminado = "Publicada").hayCambios)
    }
}
