package com.valoralo.data.auth

import com.valoralo.BuildConfig
import com.valoralo.data.red.MENSAJE_ERROR_GENERICO
import com.valoralo.data.red.MENSAJE_SIN_CONEXION

enum class TipoErrorCuenta(val mensaje: String) {
    CREDENCIALES("El correo o la contraseña no son correctos."),
    CORREO_EN_USO("Ya hay una cuenta con ese correo. Inicia sesión o recupera la contraseña."),
    CORREO_NO_VALIDO("Escribe un correo electrónico válido."),
    CONTRASENA_DEBIL("La contraseña es demasiado débil."),
    DEMASIADOS_INTENTOS("Demasiados intentos. Espera unos minutos y vuelve a probar."),
    CUENTA_DESACTIVADA("Esta cuenta está desactivada."),
    REQUIERE_LOGIN_RECIENTE("Por seguridad, cierra sesión, vuelve a entrar y repite la operación."),
    SIN_CONEXION(MENSAJE_SIN_CONEXION),
    /** Rejected by our functions (e.g. the alias): the server's own text is shown. */
    RECHAZADO_SERVIDOR(MENSAJE_ERROR_GENERICO),
    /** Connection lost while saving and we couldn't check whether it was saved. */
    INCIERTO("Se ha cortado la conexión y no sabemos si se ha guardado. Compruébalo cuando vuelvas a tener red."),
    GOOGLE_NO_CONFIGURADO("El inicio de sesión con Google todavía no está disponible."),
    GOOGLE_CANCELADO("Has cancelado el inicio de sesión con Google."),
    GOOGLE_FALLO("No se ha podido iniciar sesión con Google. Inténtalo de nuevo o entra con tu correo."),
    MENOR_DE_14("Valoralo es para mayores de 14 años. Hemos eliminado la cuenta."),
    DESCONOCIDO(MENSAJE_ERROR_GENERICO),
}

/**
 * The message is always the user text: [mensajeUsuario] (a text our functions wrote for the user) or the
 * type's. Only debug builds ([mostrarDetalle]) append the technical cause; in release it goes to
 * Crashlytics ([InformeErrores]) and never to the screen. [campo] = form field the server rejected ("alias").
 */
class ErrorCuenta(
    val tipo: TipoErrorCuenta,
    causa: Throwable? = null,
    mostrarDetalle: Boolean = BuildConfig.DEBUG,
    mensajeUsuario: String? = null,
    val campo: String? = null,
) : Exception(textoError(tipo, causa, mostrarDetalle, mensajeUsuario), causa) {
    val detalle: DetalleError? = causa?.let(::detalleError)
}

internal fun textoError(tipo: TipoErrorCuenta, causa: Throwable?, mostrarDetalle: Boolean, mensajeUsuario: String? = null): String {
    val texto = mensajeUsuario ?: tipo.mensaje
    // Errors our server wrote for the user, and the network ones, carry no technical cause worth showing.
    val sinDetalle = tipo in setOf(TipoErrorCuenta.GOOGLE_CANCELADO, TipoErrorCuenta.RECHAZADO_SERVIDOR, TipoErrorCuenta.INCIERTO)
    if (!mostrarDetalle || causa == null || sinDetalle) return texto
    val d = detalleError(causa)
    return "$texto\n\n[debug] ${d.codigoVisible}\n${d.resumen}"
}

/** Technical cause of an account error, for Crashlytics and debug builds. Never personal data. */
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
