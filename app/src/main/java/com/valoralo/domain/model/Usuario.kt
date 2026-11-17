package com.valoralo.domain.model

/** usuarios/{uid} (doc 02). rol = DOCENTE and verificado are written only by the backend. */
data class Usuario(
    val uid: String,
    val alias: String,
    val rol: Rol,
    val verificado: Boolean,
    /** Version of the terms of use the user accepted (null = none yet: accounts from before). */
    val condiciones: String? = CONDICIONES_VIGENTES,
) {
    val aceptoCondicionesVigentes: Boolean get() = condiciones == CONDICIONES_VIGENTES
}

/** Data asked at sign-up. The birth date is checked but never stored (only mayorDe14). */
data class PerfilNuevo(
    val alias: String,
    val rol: Rol,
) {
    init {
        require(rol == Rol.ESTUDIANTE || rol == Rol.OTRO) { "Al registrarse solo se puede elegir ESTUDIANTE u OTRO" }
    }
}

/** Authentication state. */
sealed interface Sesion {
    data object Cargando : Sesion
    data object SinSesion : Sesion

    /** [perfil] is null while the user has not completed sign-up (e.g. first Google login). */
    data class Iniciada(
        val uid: String,
        val correo: String?,
        val perfil: Usuario?,
        val conGoogle: Boolean,
    ) : Sesion
}
