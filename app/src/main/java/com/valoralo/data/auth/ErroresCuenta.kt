package com.valoralo.data.auth

import com.valoralo.BuildConfig

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
    GOOGLE_FALLO("Google no ha permitido iniciar sesión. Inténtalo más tarde o entra con tu correo."),
    MENOR_DE_14("Valoralo es para mayores de 14 años. Hemos eliminado la cuenta."),
    DESCONOCIDO("No se ha podido completar la operación. Inténtalo de nuevo."),
}

/** How much of the technical cause the on-screen message includes. */
enum class NivelDetalle {
    /** Only the user text. */
    NINGUNO,

    /** Plus "Código 16 · GetCredentialCancellationException": no personal data, so also in release (Google sign-in). */
    CODIGO,

    /** Plus the technical message (debug builds). */
    COMPLETO,
}

private val NIVEL_POR_DEFECTO = if (BuildConfig.DEBUG) NivelDetalle.COMPLETO else NivelDetalle.NINGUNO

class ErrorCuenta(
    val tipo: TipoErrorCuenta,
    causa: Throwable? = null,
    nivel: NivelDetalle = NIVEL_POR_DEFECTO,
) : Exception(textoError(tipo, causa, nivel), causa) {
    val detalle: DetalleError? = causa?.let(::detalleError)

    /** Same error, showing its code and type even in release (Google sign-in, so testers can report it). */
    fun conCodigoVisible(): ErrorCuenta =
        ErrorCuenta(tipo, cause, if (BuildConfig.DEBUG) NivelDetalle.COMPLETO else NivelDetalle.CODIGO)
}

internal fun textoError(tipo: TipoErrorCuenta, causa: Throwable?, nivel: NivelDetalle): String {
    if (nivel == NivelDetalle.NINGUNO || causa == null || tipo == TipoErrorCuenta.GOOGLE_CANCELADO) return tipo.mensaje
    val d = detalleError(causa)
    return when (nivel) {
        NivelDetalle.CODIGO -> "${tipo.mensaje}\n\n${d.codigoVisible}"
        else -> "${tipo.mensaje}\n\n${d.codigoVisible}\n[debug] ${d.resumen}"
    }
}

/**
 * Technical cause of an account error: what Crashlytics receives and the screen shows. Never personal data:
 * [codigoVisible] only has the status/error code and the exception class (kept unobfuscated by proguard-rules.pro).
 */
data class DetalleError(val tipo: String, val codigo: String?, val mensaje: String?, val clase: String = tipo) {
    val resumen: String get() = listOfNotNull(tipo, codigo?.let { "código $it" }, mensaje).joinToString(" · ")
    val codigoVisible: String get() = listOfNotNull(codigo?.let { "Código $it" }, clase).joinToString(" · ")
}

/**
 * Type and code of the most specific known cause:
 * - Credential Manager: the stable `type` string (class names are renamed by R8) and the Play services
 *   status code that Google Identity puts in brackets in the message ("[28444] Developer console…").
 * - Firebase Auth: its errorCode ("ERROR_INVALID_CREDENTIAL").
 * - Play services ApiException: its statusCode (10 = DEVELOPER_ERROR: package/SHA-1 not registered).
 */
fun detalleError(e: Throwable): DetalleError {
    val cadena = generateSequence(e) { it.cause.takeIf { c -> c !== it } }.take(8).toList()
    cadena.firstNotNullOfOrNull { c ->
        when (c) {
            is ErrorCuenta -> null
            is androidx.credentials.exceptions.GetCredentialException -> DetalleError(
                c.type, codigoEntreCorchetes(c.errorMessage?.toString()) ?: codigoEnCausas(c), c.errorMessage?.toString(), clase(c),
            )
            is com.google.firebase.auth.FirebaseAuthException -> DetalleError("FirebaseAuth", c.errorCode, c.message, clase(c))
            is com.google.android.gms.common.api.ApiException -> DetalleError("ApiException", c.statusCode.toString(), c.message, clase(c))
            else -> null
        }
    }?.let { return it }
    val raiz = cadena.last()
    return DetalleError(clase(raiz), codigoEntreCorchetes(raiz.message), raiz.message)
}

private fun clase(e: Throwable): String = e.javaClass.simpleName.ifEmpty { e.javaClass.name }

private fun codigoEnCausas(e: Throwable): String? =
    generateSequence(e.cause) { it.cause.takeIf { c -> c !== it } }.take(8)
        .firstNotNullOfOrNull { (it as? com.google.android.gms.common.api.ApiException)?.statusCode?.toString() ?: codigoEntreCorchetes(it.message) }

/**
 * Credential Manager reports some Google Identity failures as a cancellation (e.g. "16: [16] Account
 * reauth failed" when the app's package/SHA-1 is not accepted). A real user cancellation has no status code.
 */
fun tipoCancelacionGoogle(mensaje: String?): TipoErrorCuenta =
    if (codigoEntreCorchetes(mensaje) != null) TipoErrorCuenta.GOOGLE_FALLO else TipoErrorCuenta.GOOGLE_CANCELADO

internal fun codigoEntreCorchetes(texto: String?): String? =
    texto?.let { Regex("""\[(\d+)]""").find(it)?.groupValues?.get(1) ?: Regex("""^(\d+):""").find(it.trim())?.groupValues?.get(1) }

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
