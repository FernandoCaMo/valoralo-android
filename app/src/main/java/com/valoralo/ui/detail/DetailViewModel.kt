package com.valoralo.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoralo.data.repository.CentroRepository
import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.StatsDepartamento
import com.valoralo.domain.model.StatsGrupo
import com.valoralo.domain.model.Valoracion
import com.valoralo.domain.usecase.FiltroRol
import com.valoralo.domain.usecase.filtrarPor
import com.valoralo.domain.usecase.para
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface DetailUiState {
    data object Cargando : DetailUiState
    data object NoEncontrado : DetailUiState
    data class Listo(
        val centro: Centro,
        val filtroRol: FiltroRol,
        /** Averages for the selected role. */
        val notas: StatsGrupo,
        /** Department averages for the selected role, best first. */
        val departamentos: List<Pair<String, StatsDepartamento>>,
        val resenas: List<Valoracion>,
    ) : DetailUiState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: CentroRepository,
) : ViewModel() {

    private val codigo: String = checkNotNull(savedStateHandle[ARG_CODIGO])
    private val filtroRol = MutableStateFlow(FiltroRol.TODOS)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observarCentro(codigo),
        repository.observarValoraciones(codigo),
        filtroRol,
    ) { centro, valoraciones, filtro ->
        if (centro == null) {
            DetailUiState.NoEncontrado
        } else {
            val notas = centro.stats.para(filtro)
            DetailUiState.Listo(
                centro = centro,
                filtroRol = filtro,
                notas = notas,
                departamentos = notas.porDepartamento.toList()
                    .sortedWith(compareByDescending<Pair<String, StatsDepartamento>> { it.second.media }.thenBy { it.first }),
                resenas = valoraciones.filtrarPor(filtro),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Cargando)

    fun onFiltroRolChange(filtro: FiltroRol) = filtroRol.update { filtro }

    companion object {
        const val ARG_CODIGO = "codigo"
    }
}
