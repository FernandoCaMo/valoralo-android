package com.valoralo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoralo.data.repository.CentroRepository
import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.PROVINCIAS_ANDALUCIA
import com.valoralo.domain.usecase.FiltroCentros
import com.valoralo.domain.usecase.filtrar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class OpcionesFiltro(
    val provincias: List<String> = PROVINCIAS_ANDALUCIA,
    /** Municipios of the selected provincia (empty when none is selected). */
    val municipios: List<String> = emptyList(),
    val tipos: List<String> = emptyList(),
    val niveles: List<String> = emptyList(),
)

data class HomeUiState(
    val cargando: Boolean = true,
    val texto: String = "",
    val filtro: FiltroCentros = FiltroCentros(),
    val centros: List<Centro> = emptyList(),
    val opciones: OpcionesFiltro = OpcionesFiltro(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: CentroRepository,
) : ViewModel() {

    private val texto = MutableStateFlow("")
    private val filtro = MutableStateFlow(FiltroCentros())

    val uiState: StateFlow<HomeUiState> =
        combine(repository.observarCentros(), texto, filtro) { centros, texto, filtro ->
            HomeUiState(
                cargando = false,
                texto = texto,
                filtro = filtro,
                centros = centros.filtrar(texto, filtro),
                opciones = opcionesPara(centros, filtro),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onTextoChange(nuevo: String) = texto.update { nuevo }

    fun onFiltroChange(nuevo: FiltroCentros) = filtro.update { nuevo }

    fun limpiarFiltros() = filtro.update { FiltroCentros() }

    private fun opcionesPara(centros: List<Centro>, filtro: FiltroCentros) = OpcionesFiltro(
        municipios = filtro.provincia?.let { p ->
            centros.filter { it.provincia == p }.map { it.municipio }.distinct().sorted()
        }.orEmpty(),
        tipos = centros.map { it.tipo }.distinct().sorted(),
        niveles = centros.flatMap { c -> c.ensenanzas.map { it.nivel } }.distinct()
            .sortedBy { ORDEN_NIVELES.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE },
    )

    private companion object {
        val ORDEN_NIVELES = listOf(
            "Infantil", "Primaria", "ESO", "Bachillerato",
            "FP Grado Básico", "FP Grado Medio", "FP Grado Superior",
        )
    }
}
