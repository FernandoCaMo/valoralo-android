package com.valoralo.ui.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoralo.data.auth.CuentaRepository
import com.valoralo.data.auth.ErrorCuenta
import com.valoralo.data.auth.GoogleSignIn
import com.valoralo.data.auth.Reautenticacion
import com.valoralo.data.auth.TipoErrorCuenta
import com.valoralo.domain.model.PerfilNuevo
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Sesion
import com.valoralo.domain.usecase.validarAlias
import com.valoralo.domain.usecase.validarContrasena
import com.valoralo.domain.usecase.validarCorreo
import com.valoralo.domain.usecase.validarFechaNacimiento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

private val ZONA = ZoneId.of("Europe/Madrid")

/** Session for the whole app (bottom bar, redirects, "Añadir valoración"). */
@HiltViewModel
class SesionViewModel @Inject constructor(repo: CuentaRepository) : ViewModel() {
    val sesion: StateFlow<Sesion> = repo.sesion
}

// ---------- Login ----------

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val enviando: Boolean = false,
    val error: String? = null,
    val aviso: String? = null,
    val terminado: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val repo: CuentaRepository) : ViewModel() {
    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun onCorreo(v: String) = _estado.update { it.copy(correo = v, error = null) }
    fun onContrasena(v: String) = _estado.update { it.copy(contrasena = v, error = null) }

    fun entrar() {
        val s = _estado.value
        validarCorreo(s.correo)?.let { e -> _estado.update { it.copy(error = e.mensaje) }; return }
        if (s.contrasena.isEmpty()) { _estado.update { it.copy(error = "Escribe tu contraseña.") }; return }
        ejecutar { repo.iniciarSesion(s.correo, s.contrasena) }
    }

    fun entrarConGoogle(context: Context) = ejecutar { repo.iniciarConGoogle(GoogleSignIn.obtenerIdToken(context)) }

    fun recuperarContrasena(correo: String) {
        validarCorreo(correo)?.let { e -> _estado.update { it.copy(error = e.mensaje) }; return }
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, error = null) }
            try {
                repo.recuperarContrasena(correo)
            } catch (_: ErrorCuenta) {
                // Same answer whether or not the address exists (avoids account enumeration).
            }
            _estado.update {
                it.copy(enviando = false, aviso = "Si hay una cuenta con ese correo, te hemos enviado un enlace para crear una contraseña nueva.")
            }
        }
    }

    fun avisoMostrado() = _estado.update { it.copy(aviso = null) }

    private fun ejecutar(accion: suspend () -> Unit) {
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, error = null) }
            try {
                accion()
                _estado.update { it.copy(enviando = false, terminado = true) }
            } catch (e: ErrorCuenta) {
                val error = if (e.tipo == TipoErrorCuenta.GOOGLE_CANCELADO) null else e.message
                _estado.update { it.copy(enviando = false, error = error) }
            }
        }
    }
}

// ---------- Registro (correo) y completar perfil (Google) ----------

data class RegistroUiState(
    val correo: String = "",
    val contrasena: String = "",
    val alias: String = "",
    val fechaNacimiento: LocalDate? = null,
    val rol: Rol = Rol.ESTUDIANTE,
    val aceptaPrivacidad: Boolean = false,
    val errores: Map<CampoRegistro, String> = emptyMap(),
    val error: String? = null,
    val enviando: Boolean = false,
    val terminado: Boolean = false,
)

enum class CampoRegistro { CORREO, CONTRASENA, ALIAS, FECHA, PRIVACIDAD }

@HiltViewModel
class RegistroViewModel @Inject constructor(private val repo: CuentaRepository) : ViewModel() {
    private val _estado = MutableStateFlow(RegistroUiState())
    val estado: StateFlow<RegistroUiState> = _estado.asStateFlow()

    fun onCorreo(v: String) = _estado.update { it.copy(correo = v, errores = it.errores - CampoRegistro.CORREO) }
    fun onContrasena(v: String) = _estado.update { it.copy(contrasena = v, errores = it.errores - CampoRegistro.CONTRASENA) }
    fun onAlias(v: String) = _estado.update { it.copy(alias = v, errores = it.errores - CampoRegistro.ALIAS) }
    fun onFecha(v: LocalDate?) = _estado.update { it.copy(fechaNacimiento = v, errores = it.errores - CampoRegistro.FECHA) }
    fun onRol(v: Rol) = _estado.update { it.copy(rol = v) }
    fun onPrivacidad(v: Boolean) = _estado.update { it.copy(aceptaPrivacidad = v, errores = it.errores - CampoRegistro.PRIVACIDAD) }

    /** [conCredenciales] = false when completing the profile after a Google login. */
    private fun validar(conCredenciales: Boolean): Boolean {
        val s = _estado.value
        val errores = buildMap {
            if (conCredenciales) {
                validarCorreo(s.correo)?.let { put(CampoRegistro.CORREO, it.mensaje) }
                validarContrasena(s.contrasena)?.let { put(CampoRegistro.CONTRASENA, it.mensaje) }
            }
            validarAlias(s.alias)?.let { put(CampoRegistro.ALIAS, it.mensaje) }
            validarFechaNacimiento(s.fechaNacimiento, LocalDate.now(ZONA))?.let { put(CampoRegistro.FECHA, it.mensaje) }
            if (!s.aceptaPrivacidad) put(CampoRegistro.PRIVACIDAD, "Tienes que aceptar la política de privacidad.")
        }
        _estado.update { it.copy(errores = errores, error = null) }
        return errores.isEmpty()
    }

    fun registrar() {
        if (!validar(conCredenciales = true)) return
        val s = _estado.value
        ejecutar { repo.registrar(s.correo, s.contrasena, PerfilNuevo(s.alias, s.rol)) }
    }

    fun completarPerfil() {
        val s = _estado.value
        val esMenor = s.fechaNacimiento != null &&
            validarFechaNacimiento(s.fechaNacimiento, LocalDate.now(ZONA)) == com.valoralo.domain.usecase.ErrorRegistro.MENOR_DE_14
        if (esMenor) {
            // The Google account already exists in Firebase Auth: delete it (doc 02: no account for under 14).
            ejecutar(errorFinal = TipoErrorCuenta.MENOR_DE_14.mensaje) { repo.cancelarRegistro() }
            return
        }
        if (!validar(conCredenciales = false)) return
        ejecutar { repo.crearPerfil(PerfilNuevo(s.alias, s.rol)) }
    }

    fun cancelar() = ejecutar { repo.cancelarRegistro() }

    private fun ejecutar(errorFinal: String? = null, accion: suspend () -> Unit) {
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, error = null) }
            try {
                accion()
                _estado.update { it.copy(enviando = false, terminado = errorFinal == null, error = errorFinal) }
            } catch (e: ErrorCuenta) {
                _estado.update { it.copy(enviando = false, error = e.message) }
            }
        }
    }
}

// ---------- Mi cuenta ----------

data class MiCuentaUiState(
    val enviando: Boolean = false,
    val error: String? = null,
    val aviso: String? = null,
)

@HiltViewModel
class MiCuentaViewModel @Inject constructor(private val repo: CuentaRepository) : ViewModel() {
    val sesion: StateFlow<Sesion> = repo.sesion
    private val _estado = MutableStateFlow(MiCuentaUiState())
    val estado: StateFlow<MiCuentaUiState> = _estado.asStateFlow()

    /** Returns the validation error, or null if the change was sent. */
    fun cambiarAlias(alias: String): String? {
        validarAlias(alias)?.let { return it.mensaje }
        ejecutar("Alias actualizado.") { repo.cambiarAlias(alias) }
        return null
    }

    fun cerrarSesion(context: Context) = viewModelScope.launch {
        repo.cerrarSesion()
        GoogleSignIn.olvidar(context)
    }

    fun eliminarConContrasena(contrasena: String) =
        ejecutar("Tu cuenta se ha eliminado.") { repo.eliminarCuenta(Reautenticacion.ConContrasena(contrasena)) }

    fun eliminarConGoogle(context: Context) = ejecutar("Tu cuenta se ha eliminado.") {
        repo.eliminarCuenta(Reautenticacion.ConGoogle(GoogleSignIn.obtenerIdToken(context)))
        GoogleSignIn.olvidar(context)
    }

    fun mensajesMostrados() = _estado.update { it.copy(error = null, aviso = null) }

    private fun ejecutar(avisoOk: String, accion: suspend () -> Unit) {
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, error = null) }
            try {
                accion()
                _estado.update { it.copy(enviando = false, aviso = avisoOk) }
            } catch (e: ErrorCuenta) {
                val error = if (e.tipo == TipoErrorCuenta.GOOGLE_CANCELADO) null else e.message
                _estado.update { it.copy(enviando = false, error = error) }
            }
        }
    }
}
