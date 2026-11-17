package com.valoralo.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoralo.data.comparador.ResultadoSeleccion
import com.valoralo.data.comparador.SeleccionComparador
import com.valoralo.data.favoritos.ErrorFavorito
import com.valoralo.data.favoritos.ExcepcionFavorito
import com.valoralo.data.favoritos.FavoritosRepository
import com.valoralo.domain.usecase.MAX_CENTROS_COMPARADOR
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EventoAccion {
    data class Aviso(val texto: String) : EventoAccion
    data class IniciarSesion(val mensaje: String = "Inicia sesión para guardar centros en favoritos.") : EventoAccion
}

/** Favourite and compare actions, shared by Inicio, Favoritos and the detail (app-level). */
@HiltViewModel
class AccionesCentroViewModel @Inject constructor(
    private val favoritosRepo: FavoritosRepository,
    private val comparador: SeleccionComparador,
) : ViewModel() {

    /** null = no session. */
    val favoritos: StateFlow<Set<String>?> = favoritosRepo.favoritos
    val seleccion: StateFlow<List<String>> = comparador.seleccion

    private val _eventos = MutableSharedFlow<EventoAccion>(extraBufferCapacity = 4)
    val eventos: SharedFlow<EventoAccion> = _eventos

    fun alternarFavorito(codigo: String) {
        viewModelScope.launch {
            try {
                val ahora = favoritosRepo.alternar(codigo)
                _eventos.emit(EventoAccion.Aviso(if (ahora) "Guardado en favoritos." else "Quitado de favoritos."))
            } catch (e: ExcepcionFavorito) {
                _eventos.emit(if (e.error == ErrorFavorito.SIN_SESION) EventoAccion.IniciarSesion() else EventoAccion.Aviso(e.error.mensaje))
            }
        }
    }

    fun alternarComparar(codigo: String) {
        val aviso = when (comparador.alternar(codigo)) {
            ResultadoSeleccion.ANADIDO -> null
            ResultadoSeleccion.QUITADO -> null
            ResultadoSeleccion.LLENO -> "Puedes comparar como máximo $MAX_CENTROS_COMPARADOR centros. Quita alguno primero."
        }
        aviso?.let { _eventos.tryEmit(EventoAccion.Aviso(it)) }
    }

    fun vaciarComparador() = comparador.vaciar()
}
