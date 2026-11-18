package com.valoralo.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Test

class ConRedTest {
    @Test
    fun `cargando sin red es sin conexion, nunca no encontrado`() {
        assertEquals(DetailUiState.SinConexion, conRed(DetailUiState.Cargando, conectado = false))
        assertEquals(DetailUiState.Cargando, conRed(DetailUiState.Cargando, conectado = true))
    }

    @Test
    fun `lo ya cargado se sigue mostrando sin red`() {
        assertEquals(DetailUiState.NoEncontrado, conRed(DetailUiState.NoEncontrado, conectado = false))
        assertEquals(DetailUiState.Error, conRed(DetailUiState.Error, conectado = false))
    }
}
