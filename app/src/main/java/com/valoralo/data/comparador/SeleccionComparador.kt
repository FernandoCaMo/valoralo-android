package com.valoralo.data.comparador

import com.valoralo.domain.usecase.MAX_CENTROS_COMPARADOR
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

enum class ResultadoSeleccion { ANADIDO, QUITADO, LLENO }

/** Centros chosen to compare (2–3). Kept in memory while the app is open; no account needed. */
@Singleton
class SeleccionComparador @Inject constructor() {
    private val _seleccion = MutableStateFlow<List<String>>(emptyList())
    val seleccion: StateFlow<List<String>> = _seleccion.asStateFlow()

    fun alternar(codigo: String): ResultadoSeleccion {
        val actual = _seleccion.value
        return when {
            codigo in actual -> ResultadoSeleccion.QUITADO.also { _seleccion.update { it - codigo } }
            actual.size >= MAX_CENTROS_COMPARADOR -> ResultadoSeleccion.LLENO
            else -> ResultadoSeleccion.ANADIDO.also { _seleccion.update { it + codigo } }
        }
    }

    fun vaciar() = _seleccion.update { emptyList() }
}
