package com.valoralo.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoralo.data.repository.CentroRepository
import com.valoralo.data.repository.ProgresoDescarga
import com.valoralo.domain.model.Centro
import com.valoralo.domain.usecase.FiltroCentros
import com.valoralo.domain.usecase.OpcionesFiltro
import com.valoralo.domain.usecase.filtrar
import com.valoralo.domain.usecase.opcionesFiltro
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class BuscarUiState(
    val cargando: Boolean = true,
    /** The catalogue could not be loaded (no network, permissions…). */
    val error: Boolean = false,
    /** Non-null while the catalogue is being downloaded (first launch or refresh). */
    val progreso: ProgresoDescarga? = null,
    val texto: String = "",
    val filtro: FiltroCentros = FiltroCentros(),
    val centros: List<Centro> = emptyList(),
    val opciones: OpcionesFiltro = OpcionesFiltro(),
    /** Provincias deduced from the chosen ciudades when no provincia is chosen. */
    val provinciasDeducidas: Set<String> = emptySet(),
)

@HiltViewModel
class BuscarViewModel @Inject constructor(
    repository: CentroRepository,
) : ViewModel() {

    private val texto = MutableStateFlow("")
    private val filtro = MutableStateFlow(FiltroCentros())
    private var todos: List<Centro> = emptyList()

    private val catalogo: StateFlow<BuscarUiState> =
        combine(repository.observarCentros(), texto, filtro) { centros, texto, filtro ->
            todos = centros
            BuscarUiState(
                cargando = false,
                texto = texto,
                filtro = filtro,
                centros = centros.filtrar(texto, filtro),
                opciones = opcionesFiltro(centros, filtro),
                provinciasDeducidas = filtro.provinciasDeducidas(centros),
            )
        }.catch { emit(BuscarUiState(cargando = false, error = true)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BuscarUiState())

    val uiState: StateFlow<BuscarUiState> =
        combine(catalogo, repository.progresoDescarga) { estado, progreso -> estado.copy(progreso = progreso) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BuscarUiState())

    fun onTextoChange(nuevo: String) = texto.update { nuevo }

    /** Always keeps ciudades and localidades consistent with what is chosen above them. */
    fun onFiltroChange(nuevo: FiltroCentros) = filtro.update { nuevo.coherente(todos) }

    fun limpiarFiltros() = filtro.update { FiltroCentros() }
}
