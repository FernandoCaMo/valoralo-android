package com.valoralo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoralo.data.bloqueos.BloqueosRepository
import com.valoralo.data.destacadas.DestacadasRepository
import com.valoralo.data.likes.ClaveResena
import com.valoralo.data.likes.LikesRepository
import com.valoralo.data.repository.CentroRepository
import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Destacada
import com.valoralo.domain.model.Destacadas
import com.valoralo.domain.usecase.EstadoLike
import com.valoralo.ui.components.ControlLikes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Heart of a featured review; [propia] = my own review (only the number). */
data class LikeDestacada(val estado: EstadoLike, val propia: Boolean)

@HiltViewModel
class InicioViewModel @Inject constructor(
    private val destacadasRepo: DestacadasRepository,
    centros: CentroRepository,
    private val likesRepo: LikesRepository,
    bloqueos: BloqueosRepository,
) : ViewModel() {

    /** null while loading. One read of destacadas/actual per visit to Inicio. */
    private val _destacadas = MutableStateFlow<Destacadas?>(null)

    /** Without the reviews of authors this user has blocked. */
    val destacadas: StateFlow<Destacadas?> = combine(_destacadas, bloqueos.bloqueados) { d, bloqueados ->
        d?.copy(resenas = d.resenas.filterNot { it.autorUid != null && it.autorUid in bloqueados })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val likes = ControlLikes(viewModelScope, likesRepo)

    /** Hearts of the carousel by review. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val corazones: StateFlow<Map<ClaveResena, LikeDestacada>> = _destacadas.filterNotNull().flatMapLatest { d ->
        val claves = d.resenas.mapNotNull { it.clave() }
        combine(likesRepo.misLikes(claves.map { it.codigo }).onStart { emit(emptySet()) }, likes.ajustes) { mios, ajustes ->
            val uid = likes.uidActual()
            d.resenas.mapNotNull { r ->
                val clave = r.clave() ?: return@mapNotNull null
                clave to LikeDestacada(likes.estado(clave, r.nLikes, clave in mios, ajustes), propia = clave.autorUid == uid)
            }.toMap()
        }
    }.catch { emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** The catalogue (local cache) to draw the favourite cards without extra reads. */
    val catalogo: StateFlow<List<Centro>> = centros.observarCentros()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _destacadas.value = runCatching { destacadasRepo.cargar() }.getOrDefault(Destacadas())
        }
    }

    fun alternarLike(r: Destacada) {
        val clave = r.clave() ?: return
        // The count in destacadas/actual does not change until the next calculation: the local adjustment stays.
        likes.alternar(clave, r.nLikes, corazones.value[clave]?.estado?.dado ?: false)
    }
}

fun Destacada.clave(): ClaveResena? = autorUid?.let { ClaveResena(codigo, it) }
