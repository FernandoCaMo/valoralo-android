package com.valoralo.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoralo.data.auth.CuentaRepository
import com.valoralo.data.repository.CentroRepository
import com.valoralo.data.valoraciones.ErrorPublicacion
import com.valoralo.data.valoraciones.MotivoDenuncia
import com.valoralo.data.valoraciones.ValoracionRepository
import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Sesion
import com.valoralo.domain.model.StatsDepartamento
import com.valoralo.domain.model.StatsGrupo
import com.valoralo.domain.model.Valoracion
import com.valoralo.data.bloqueos.BloqueosRepository
import com.valoralo.data.bloqueos.ExcepcionBloqueo
import com.valoralo.data.likes.ClaveResena
import com.valoralo.data.likes.LikesRepository
import com.valoralo.domain.usecase.EstadoLike
import com.valoralo.domain.usecase.OrdenResenas
import com.valoralo.domain.usecase.ordenadas
import com.valoralo.ui.components.ControlLikes
import com.valoralo.domain.usecase.FiltroRol
import com.valoralo.domain.usecase.filtrarPor
import com.valoralo.domain.usecase.para
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailUiState {
    data object Cargando : DetailUiState
    data object NoEncontrado : DetailUiState
    data object Error : DetailUiState
    data class Listo(
        val centro: Centro,
        val filtroRol: FiltroRol,
        /** Averages for the selected role. */
        val notas: StatsGrupo,
        /** Department averages for the selected role, best first. */
        val departamentos: List<Pair<String, StatsDepartamento>>,
        val resenas: List<Valoracion>,
        /** The signed-in user's own review in any state (null = none). */
        val miValoracion: Valoracion? = null,
        /** uid of the signed-in user with a complete profile (null = can't review or report). */
        val uidActual: String? = null,
        val orden: OrdenResenas = OrdenResenas.RECIENTES,
        /** Heart of each review by author uid (others' and my own). */
        val likes: Map<String, EstadoLike> = emptyMap(),
    ) : DetailUiState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: CentroRepository,
    cuentas: CuentaRepository,
    private val valoraciones: ValoracionRepository,
    likesRepo: LikesRepository,
    private val bloqueos: BloqueosRepository,
) : ViewModel() {

    private val codigo: String = checkNotNull(savedStateHandle[ARG_CODIGO])
    private val filtroRol = MutableStateFlow(FiltroRol.TODOS)
    private val orden = MutableStateFlow(OrdenResenas.RECIENTES)
    val likes = ControlLikes(viewModelScope, likesRepo)
    private val misLikes = likesRepo.misLikes(listOf(codigo)).onStart { emit(emptySet()) }
    private val _aviso = MutableStateFlow<String?>(null)
    /** One-off messages (report sent, errors). */
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    // Reviews of blocked authors are not shown to this user.
    private val base = combine(
        repository.observarCentro(codigo),
        repository.observarValoraciones(codigo),
        filtroRol,
        bloqueos.bloqueados,
    ) { centro, resenas, filtro, bloqueados -> Triple(centro, resenas.filterNot { it.uid in bloqueados }, filtro) }

    private val usuario = combine(
        cuentas.sesion,
        valoraciones.observarMiValoracion(codigo).onStart { emit(null) },
    ) { sesion, mia -> (sesion as? Sesion.Iniciada)?.takeIf { it.perfil != null }?.uid to mia }

    val uiState: StateFlow<DetailUiState> = combine(
        base, usuario, misLikes, likes.ajustes, orden,
    ) { (centro, resenas, filtro), (uid, mia), mios, ajustes, orden ->
        if (centro == null) {
            DetailUiState.NoEncontrado
        } else {
            val notas = centro.stats.para(filtro)
            val todas = resenas + listOfNotNull(mia)
            likes.podar(todas.associate { ClaveResena(codigo, it.uid) to it.nLikes })
            DetailUiState.Listo(
                centro = centro,
                filtroRol = filtro,
                notas = notas,
                departamentos = notas.porDepartamento.toList()
                    .sortedWith(compareByDescending<Pair<String, StatsDepartamento>> { it.second.media }.thenBy { it.first }),
                resenas = resenas.filterNot { it.uid == uid }.filtrarPor(filtro).ordenadas(orden),
                miValoracion = mia,
                uidActual = uid,
                orden = orden,
                likes = todas.associate { v ->
                    val clave = ClaveResena(codigo, v.uid)
                    v.uid to likes.estado(clave, v.nLikes, clave in mios, ajustes)
                },
            )
        }
    }.catch { emit(DetailUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Cargando)

    fun onFiltroRolChange(filtro: FiltroRol) = filtroRol.update { filtro }

    fun onOrdenChange(nuevo: OrdenResenas) = orden.update { nuevo }

    /** [v] is someone else's published review. */
    fun alternarLike(v: Valoracion) {
        val estado = (uiState.value as? DetailUiState.Listo) ?: return
        likes.alternar(ClaveResena(codigo, v.uid), v.nLikes, estado.likes[v.uid]?.let { it.dado } ?: false)
    }

    fun denunciar(autorUid: String, motivo: MotivoDenuncia) {
        viewModelScope.launch {
            _aviso.value = try {
                valoraciones.denunciar(codigo, autorUid, motivo)
                "Gracias. Hemos recibido tu denuncia y la revisaremos."
            } catch (e: ErrorPublicacion) {
                e.message
            }
        }
    }

    fun bloquear(v: Valoracion) {
        viewModelScope.launch {
            _aviso.value = try {
                bloqueos.bloquear(v.uid, v.alias)
                "Has bloqueado a ${v.alias}. Ya no verás sus reseñas. Puedes desbloquearlo en Mi cuenta."
            } catch (e: ExcepcionBloqueo) {
                e.message
            }
        }
    }

    fun avisoMostrado() = _aviso.update { null }

    companion object {
        const val ARG_CODIGO = "codigo"
    }
}
