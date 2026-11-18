package com.valoralo.data.pendientes

import com.valoralo.data.auth.CuentaRepository
import com.valoralo.data.favoritos.FavoritosRepository
import com.valoralo.data.likes.ClaveResena
import com.valoralo.data.likes.ExcepcionLike
import com.valoralo.data.likes.LikesRepository
import com.valoralo.domain.model.Sesion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** What the user tried to do without an account (bookmark or like), to do it once they sign in. */
sealed interface AccionPendiente {
    data class Favorito(val codigo: String) : AccionPendiente
    data class Like(val resena: ClaveResena) : AccionPendiente
}

/**
 * Remembers one pending action and performs it when a session with a complete profile (and the terms
 * accepted) starts, within [VIGENCIA]. Leaving the sign-in flow without signing in discards it.
 * Never undoes anything: an already saved favourite or given like is left as it is.
 */
@Singleton
class AccionesPendientes(
    sesionLista: Flow<Boolean>,
    private val esFavorito: suspend (String) -> Boolean,
    private val guardarFavorito: suspend (String) -> Unit,
    private val tieneLike: suspend (ClaveResena) -> Boolean,
    private val darLike: suspend (ClaveResena) -> Unit,
    scope: CoroutineScope,
    private val reloj: () -> Instant = Instant::now,
) {
    @Inject
    constructor(cuentas: CuentaRepository, favoritos: FavoritosRepository, likes: LikesRepository) : this(
        sesionLista = cuentas.sesion.map { s -> s is Sesion.Iniciada && s.perfil?.aceptoCondicionesVigentes == true },
        esFavorito = { codigo -> withTimeoutOrNull(5_000) { favoritos.favoritos.filterNotNull().first() }.orEmpty().contains(codigo) },
        guardarFavorito = { codigo -> favoritos.alternar(codigo) },
        tieneLike = { clave -> withTimeoutOrNull(5_000) { likes.misLikes(listOf(clave.codigo)).first() }.orEmpty().contains(clave) },
        darLike = { clave -> likes.alternar(clave, dar = true) },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    private data class Guardada(val accion: AccionPendiente, val cuando: Instant)

    private val pendiente = AtomicReference<Guardada?>(null)
    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 2)

    /** "Guardado en favoritos." / "Te gusta esta reseña." after signing in. */
    val mensajes: SharedFlow<String> = _mensajes

    init {
        scope.launch {
            sesionLista.distinctUntilChanged().collect { lista -> if (lista) ejecutar() }
        }
    }

    fun guardar(accion: AccionPendiente) = pendiente.set(Guardada(accion, reloj()))

    fun descartar() = pendiente.set(null)

    private suspend fun ejecutar() {
        val g = pendiente.getAndSet(null) ?: return
        if (Duration.between(g.cuando, reloj()) > VIGENCIA) return
        try {
            when (val a = g.accion) {
                is AccionPendiente.Favorito -> if (!esFavorito(a.codigo)) {
                    guardarFavorito(a.codigo)
                    _mensajes.emit("Guardado en favoritos.")
                }
                is AccionPendiente.Like -> if (!tieneLike(a.resena)) {
                    darLike(a.resena)
                    _mensajes.emit("Te gusta esta reseña.")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ExcepcionLike) {
            _mensajes.emit(e.error.mensaje) // e.g. it is your own review
        } catch (_: Exception) {
            _mensajes.emit("No se ha podido completar lo que habías pedido. Inténtalo de nuevo.")
        }
    }

    companion object {
        val VIGENCIA: Duration = Duration.ofMinutes(15)
    }
}
