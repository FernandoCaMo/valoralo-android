package com.valoralo.ui.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoralo.data.repository.CentroRepository
import com.valoralo.data.valoraciones.ErrorPublicacion
import com.valoralo.data.valoraciones.ResultadoPublicacion
import com.valoralo.data.valoraciones.ValoracionRepository
import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.Fragmento
import com.valoralo.domain.model.Valoracion
import com.valoralo.domain.usecase.FormularioValoracion
import com.valoralo.domain.usecase.OtroDepartamento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val cargando: Boolean = true,
    val centro: Centro? = null,
    val existente: Valoracion? = null,
    val formulario: FormularioValoracion = FormularioValoracion(),
    val enviando: Boolean = false,
    val error: String? = null,
    /** Fragments to highlight in the text (blocked, or under review). */
    val fragmentos: List<Fragmento> = emptyList(),
    val enRevision: Boolean = false,
    val terminado: String? = null,
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    centros: CentroRepository,
    private val valoraciones: ValoracionRepository,
) : ViewModel() {

    private val codigo: String = checkNotNull(savedStateHandle[ARG_CODIGO])
    private val _estado = MutableStateFlow(ReviewUiState())
    val estado: StateFlow<ReviewUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val centro = centros.observarCentro(codigo).filterNotNull().first()
            val mia = valoraciones.observarMiValoracion(codigo).first()
            _estado.update {
                it.copy(
                    cargando = false,
                    centro = centro,
                    existente = mia,
                    formulario = mia?.let { v -> FormularioValoracion.desde(v, centro.departamentos) } ?: FormularioValoracion(),
                    fragmentos = mia?.revision?.fragmentos.orEmpty(),
                    enRevision = mia?.estado == EstadoValoracion.EN_REVISION,
                )
            }
        }
    }

    fun cambiar(transformar: (FormularioValoracion) -> FormularioValoracion) =
        _estado.update { it.copy(formulario = transformar(it.formulario), error = null) }

    fun onTexto(texto: String) = _estado.update {
        // Positions of marked fragments no longer apply once the text changes.
        it.copy(formulario = it.formulario.copy(texto = texto), error = null, fragmentos = it.fragmentos.filter { f -> f.inicio < 0 })
    }

    fun anadirOtro() = cambiar { it.copy(otros = it.otros + OtroDepartamento("", null)) }
    fun cambiarOtro(i: Int, otro: OtroDepartamento) = cambiar { f -> f.copy(otros = f.otros.toMutableList().also { it[i] = otro }) }
    fun quitarOtro(i: Int) = cambiar { f -> f.copy(otros = f.otros.toMutableList().also { it.removeAt(i) }) }

    fun publicar() {
        val s = _estado.value
        val centro = s.centro ?: return
        s.formulario.error(centro.departamentos)?.let { e -> _estado.update { it.copy(error = e.mensaje) }; return }
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, error = null) }
            // The server trims the text; keep the field identical so fragment positions match.
            val formulario = s.formulario.copy(texto = s.formulario.texto.trim())
            try {
                when (val r = valoraciones.publicar(codigo, formulario)) {
                    ResultadoPublicacion.Publicada -> _estado.update {
                        it.copy(enviando = false, terminado = if (s.existente != null) "Valoración actualizada." else "¡Gracias! Tu valoración está publicada.")
                    }
                    is ResultadoPublicacion.EnRevision -> _estado.update {
                        it.copy(enviando = false, formulario = formulario, fragmentos = r.fragmentos, enRevision = true)
                    }
                }
            } catch (e: ErrorPublicacion) {
                _estado.update { it.copy(enviando = false, formulario = formulario, error = e.message, fragmentos = e.fragmentos) }
            }
        }
    }

    fun borrar() {
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, error = null) }
            try {
                valoraciones.borrar(codigo)
                _estado.update { it.copy(enviando = false, terminado = "Tu valoración se ha borrado.") }
            } catch (e: ErrorPublicacion) {
                _estado.update { it.copy(enviando = false, error = e.message) }
            }
        }
    }

    fun revisionVista() = _estado.update { it.copy(enRevision = false) }

    companion object {
        const val ARG_CODIGO = "codigo"
    }
}
