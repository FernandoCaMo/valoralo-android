package com.valoralo.data.auth

import com.valoralo.domain.model.PerfilNuevo
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Sesion
import kotlinx.coroutines.flow.StateFlow

/** How the user confirms their identity before deleting the account. */
sealed interface Reautenticacion {
    data class ConContrasena(val contrasena: String) : Reautenticacion
    data class ConGoogle(val idToken: String) : Reautenticacion
}

/** Accounts (Firebase Auth + usuarios/{uid}). Operations throw [ErrorCuenta]. */
interface CuentaRepository {
    val sesion: StateFlow<Sesion>

    suspend fun iniciarSesion(correo: String, contrasena: String)

    /** Creates the Auth account and usuarios/{uid}; age is checked before calling this. */
    suspend fun registrar(correo: String, contrasena: String, perfil: PerfilNuevo)

    suspend fun iniciarConGoogle(idToken: String)

    /** Completes sign-up after a first Google login. */
    suspend fun crearPerfil(perfil: PerfilNuevo)

    /** First Google login of someone under 14: deletes the just-created Auth account. */
    suspend fun cancelarRegistro()

    suspend fun cambiarAlias(alias: String)

    /** Accounts from before the terms of use (or after a new version): accept the ones in force. */
    suspend fun aceptarCondiciones()

    /** Only between ESTUDIANTE and OTRO; reviews already written keep their rolAutor. */
    suspend fun cambiarRol(rol: Rol)
    suspend fun recuperarContrasena(correo: String)
    suspend fun cerrarSesion()

    /** Deletes usuarios/{uid} and the Auth account after re-authenticating. */
    suspend fun eliminarCuenta(reautenticacion: Reautenticacion)
}
