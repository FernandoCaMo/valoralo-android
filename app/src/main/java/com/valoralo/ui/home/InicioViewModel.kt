package com.valoralo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoralo.data.destacadas.DestacadasRepository
import com.valoralo.data.repository.CentroRepository
import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Destacadas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InicioViewModel @Inject constructor(
    private val destacadasRepo: DestacadasRepository,
    centros: CentroRepository,
) : ViewModel() {

    /** null while loading. One read of destacadas/actual per visit to Inicio. */
    private val _destacadas = MutableStateFlow<Destacadas?>(null)
    val destacadas: StateFlow<Destacadas?> = _destacadas.asStateFlow()

    /** The catalogue (local cache) to draw the favourite cards without extra reads. */
    val catalogo: StateFlow<List<Centro>> = centros.observarCentros()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _destacadas.value = runCatching { destacadasRepo.cargar() }.getOrDefault(Destacadas())
        }
    }
}
