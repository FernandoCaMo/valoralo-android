package com.valoralo.ui.verify

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoralo.data.auth.CuentaRepository
import com.valoralo.data.valoraciones.ErrorPublicacion
import com.valoralo.data.verificacion.VerificacionRepository
import com.valoralo.domain.model.MiVerificacion
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Sesion
import com.valoralo.domain.usecase.validarCsv
import com.valoralo.domain.usecase.validarNombreDeclarado
import com.valoralo.domain.usecase.validarPdf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PdfElegido(val uri: Uri, val nombre: String, val bytes: Long?)

data class FormularioVerificacion(
    val nombre: String = "",
    val csv: String = "",
    val pdf: PdfElegido? = null,
    val errorNombre: String? = null,
    val errorCsv: String? = null,
    val errorPdf: String? = null,
    val error: String? = null,
    val enviando: Boolean = false,
)

data class VerificarUiState(
    val cargando: Boolean = true,
    val yaVerificado: Boolean = false,
    val solicitud: MiVerificacion? = null,
    val formulario: FormularioVerificacion = FormularioVerificacion(),
)

@HiltViewModel
class VerificarDocenteViewModel @Inject constructor(
    cuentas: CuentaRepository,
    private val verificaciones: VerificacionRepository,
) : ViewModel() {

    private val formulario = MutableStateFlow(FormularioVerificacion())

    val estado: StateFlow<VerificarUiState> = combine(
        cuentas.sesion,
        verificaciones.observarMiVerificacion().onStart { emit(null) },
        formulario,
    ) { sesion, solicitud, form ->
        val perfil = (sesion as? Sesion.Iniciada)?.perfil
        VerificarUiState(
            cargando = sesion is Sesion.Cargando,
            yaVerificado = perfil?.rol == Rol.DOCENTE && perfil.verificado,
            solicitud = solicitud,
            formulario = form,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VerificarUiState())

    fun onNombre(v: String) = formulario.update { it.copy(nombre = v, errorNombre = null, error = null) }
    fun onCsv(v: String) = formulario.update { it.copy(csv = v.take(80), errorCsv = null, error = null) }
    fun onPdf(pdf: PdfElegido?) = formulario.update {
        it.copy(pdf = pdf, errorPdf = validarPdf(pdf?.bytes)?.mensaje, error = null)
    }

    fun enviar() {
        val f = formulario.value
        val errorNombre = validarNombreDeclarado(f.nombre)?.mensaje
        val errorCsv = validarCsv(f.csv)?.mensaje
        val errorPdf = validarPdf(f.pdf?.bytes)?.mensaje
        if (errorNombre != null || errorCsv != null || errorPdf != null) {
            formulario.update { it.copy(errorNombre = errorNombre, errorCsv = errorCsv, errorPdf = errorPdf) }
            return
        }
        viewModelScope.launch {
            formulario.update { it.copy(enviando = true, error = null) }
            try {
                verificaciones.solicitar(f.nombre, f.csv, f.pdf?.uri)
                // Keep nothing on the screen once sent.
                formulario.value = FormularioVerificacion()
            } catch (e: ErrorPublicacion) {
                formulario.update { it.copy(enviando = false, error = e.message) }
            }
        }
    }
}
