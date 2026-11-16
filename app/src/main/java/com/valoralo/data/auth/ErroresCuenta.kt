package com.valoralo.data.auth

enum class TipoErrorCuenta(val mensaje: String) {
    CREDENCIALES("El correo o la contraseña no son correctos."),
    CORREO_EN_USO("Ya hay una cuenta con ese correo. Inicia sesión o recupera la contraseña."),
    CORREO_NO_VALIDO("Escribe un correo electrónico válido."),
    CONTRASENA_DEBIL("La contraseña es demasiado débil."),
    DEMASIADOS_INTENTOS("Demasiados intentos. Espera unos minutos y vuelve a probar."),
    CUENTA_DESACTIVADA("Esta cuenta está desactivada."),
    REQUIERE_LOGIN_RECIENTE("Por seguridad, cierra sesión, vuelve a entrar y repite la operación."),
    SIN_CONEXION("No hay conexión. Comprueba tu red e inténtalo de nuevo."),
    GOOGLE_NO_CONFIGURADO("El inicio de sesión con Google todavía no está disponible."),
    GOOGLE_CANCELADO("Has cancelado el inicio de sesión con Google."),
    MENOR_DE_14("Valoralo es para mayores de 14 años. Hemos eliminado la cuenta."),
    DESCONOCIDO("No se ha podido completar la operación. Inténtalo de nuevo."),
}

class ErrorCuenta(val tipo: TipoErrorCuenta, causa: Throwable? = null) : Exception(tipo.mensaje, causa)

/** Maps FirebaseAuthException.errorCode to our error types. */
fun tipoErrorAuth(codigo: String?): TipoErrorCuenta = when (codigo) {
    "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL" -> TipoErrorCuenta.CREDENCIALES
    "ERROR_EMAIL_ALREADY_IN_USE" -> TipoErrorCuenta.CORREO_EN_USO
    "ERROR_INVALID_EMAIL" -> TipoErrorCuenta.CORREO_NO_VALIDO
    "ERROR_WEAK_PASSWORD" -> TipoErrorCuenta.CONTRASENA_DEBIL
    "ERROR_TOO_MANY_REQUESTS" -> TipoErrorCuenta.DEMASIADOS_INTENTOS
    "ERROR_USER_DISABLED" -> TipoErrorCuenta.CUENTA_DESACTIVADA
    "ERROR_REQUIRES_RECENT_LOGIN" -> TipoErrorCuenta.REQUIERE_LOGIN_RECIENTE
    else -> TipoErrorCuenta.DESCONOCIDO
}
