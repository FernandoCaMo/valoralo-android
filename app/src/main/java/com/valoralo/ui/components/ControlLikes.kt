package com.valoralo.ui.components

import com.valoralo.data.likes.ClaveResena
import com.valoralo.data.likes.ErrorLike
import com.valoralo.data.likes.ExcepcionLike
import com.valoralo.data.likes.LikesRepository
import com.valoralo.domain.usecase.AjusteLike
import com.valoralo.domain.usecase.EstadoLike
import com.valoralo.domain.usecase.estadoLike
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Likes of a screen (detail, Inicio): the heart changes at once and the count is adjusted
 * locally until the review's nLikes counter catches up; on an error it goes back.
 */
class ControlLikes(private val scope: CoroutineScope, private val repo: LikesRepository) {

    private val _ajustes = MutableStateFlow<Map<ClaveResena, AjusteLike>>(emptyMap())
    val ajustes: StateFlow<Map<ClaveResena, AjusteLike>> = _ajustes.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoAccion>(extraBufferCapacity = 4)
    val eventos: SharedFlow<EventoAccion> = _eventos

    fun uidActual(): String? = repo.uidActual()

    /** What the heart of [clave] shows, given the server count and whether my like is stored. */
    fun estado(clave: ClaveResena, nServidor: Int, dadoServidor: Boolean, ajustes: Map<ClaveResena, AjusteLike>): EstadoLike =
        estadoLike(nServidor, dadoServidor, ajustes[clave])

    fun alternar(clave: ClaveResena, nServidor: Int, dadoServidor: Boolean) {
        if (repo.uidActual() == null) {
            _eventos.tryEmit(EventoAccion.IniciarSesion(ErrorLike.SIN_SESION.mensaje))
            return
        }
        val previo = _ajustes.value[clave]?.takeUnless { it.caducado(nServidor) }
        val dadoAntes = previo?.dadoAntes ?: dadoServidor
        val dar = !(previo?.dadoAhora ?: dadoServidor)
        _ajustes.update { it + (clave to AjusteLike(base = nServidor, dadoAntes = dadoAntes, dadoAhora = dar)) }
        scope.launch {
            try {
                repo.alternar(clave, dar)
            } catch (e: ExcepcionLike) {
                // Back to what it was.
                _ajustes.update { actuales ->
                    if (actuales[clave]?.dadoAhora == dar) actuales - clave else actuales
                }
                _eventos.emit(
                    if (e.error == ErrorLike.SIN_SESION) EventoAccion.IniciarSesion(e.error.mensaje)
                    else EventoAccion.Aviso(e.error.mensaje),
                )
            }
        }
    }

    /** Drops the adjustments the server count has already caught up with. */
    fun podar(nServidor: Map<ClaveResena, Int>) {
        if (_ajustes.value.any { (clave, a) -> nServidor[clave]?.let(a::caducado) == true }) {
            _ajustes.update { it.filterNot { (clave, a) -> nServidor[clave]?.let(a::caducado) == true } }
        }
    }
}
