package com.valoralo.data.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class ErroresCuentaTest {

    @Test
    fun `traduce los codigos de Firebase Auth`() {
        assertEquals(TipoErrorCuenta.CORREO_EN_USO, tipoErrorAuth("ERROR_EMAIL_ALREADY_IN_USE"))
        assertEquals(TipoErrorCuenta.CREDENCIALES, tipoErrorAuth("ERROR_WRONG_PASSWORD"))
        assertEquals(TipoErrorCuenta.CREDENCIALES, tipoErrorAuth("ERROR_USER_NOT_FOUND"))
        assertEquals(TipoErrorCuenta.CREDENCIALES, tipoErrorAuth("ERROR_INVALID_CREDENTIAL"))
        assertEquals(TipoErrorCuenta.CORREO_NO_VALIDO, tipoErrorAuth("ERROR_INVALID_EMAIL"))
        assertEquals(TipoErrorCuenta.CONTRASENA_DEBIL, tipoErrorAuth("ERROR_WEAK_PASSWORD"))
        assertEquals(TipoErrorCuenta.DEMASIADOS_INTENTOS, tipoErrorAuth("ERROR_TOO_MANY_REQUESTS"))
        assertEquals(TipoErrorCuenta.CUENTA_DESACTIVADA, tipoErrorAuth("ERROR_USER_DISABLED"))
        assertEquals(TipoErrorCuenta.REQUIERE_LOGIN_RECIENTE, tipoErrorAuth("ERROR_REQUIRES_RECENT_LOGIN"))
        assertEquals(TipoErrorCuenta.DESCONOCIDO, tipoErrorAuth("ERROR_LO_QUE_SEA"))
        assertEquals(TipoErrorCuenta.DESCONOCIDO, tipoErrorAuth(null))
    }

    @Test
    fun `los mensajes no revelan si el correo existe`() {
        // Same message for wrong password and unknown user (avoids account enumeration).
        assertEquals(
            ErrorCuenta(TipoErrorCuenta.CREDENCIALES).message,
            ErrorCuenta(tipoErrorAuth("ERROR_USER_NOT_FOUND")).message,
        )
    }

    @Test
    fun `el detalle de Credential Manager usa el tipo estable y el codigo entre corchetes`() {
        val e = androidx.credentials.exceptions.GetCredentialUnknownException(
            "During begin sign in, failure response from one tap: 10: [28444] Developer console is not set up correctly.",
        )
        val d = detalleError(ErrorCuenta(TipoErrorCuenta.DESCONOCIDO, e).cause!!)
        assertEquals(e.type, d.tipo)
        assert(d.tipo.contains("UNKNOWN")) { d.tipo }
        assertEquals("28444", d.codigo)
    }

    @Test
    fun `el detalle busca la causa conocida dentro de la cadena`() {
        val sinCredencial = androidx.credentials.exceptions.NoCredentialException("No credentials available")
        val d = detalleError(RuntimeException("envoltorio", sinCredencial))
        assertEquals(sinCredencial.type, d.tipo)
        assertEquals("No credentials available", d.mensaje)
        assertEquals(null, d.codigo)
    }

    @Test
    fun `sin causa conocida usa la clase y el codigo del mensaje`() {
        val d = detalleError(IllegalStateException("16: Account reauth failed."))
        assertEquals("IllegalStateException", d.tipo)
        assertEquals("16", d.codigo)
    }

    @Test
    fun `el detalle depende del nivel y nunca se muestra al cancelar`() {
        val causa = androidx.credentials.exceptions.NoCredentialException("No credentials available")
        assertEquals(TipoErrorCuenta.DESCONOCIDO.mensaje, textoError(TipoErrorCuenta.DESCONOCIDO, causa, NivelDetalle.NINGUNO))
        val completo = textoError(TipoErrorCuenta.DESCONOCIDO, causa, NivelDetalle.COMPLETO)
        assert(completo.startsWith(TipoErrorCuenta.DESCONOCIDO.mensaje)) { completo }
        assert("[debug]" in completo && "No credentials available" in completo) { completo }
        assertEquals(TipoErrorCuenta.GOOGLE_CANCELADO.mensaje, textoError(TipoErrorCuenta.GOOGLE_CANCELADO, causa, NivelDetalle.COMPLETO))
        assertEquals(TipoErrorCuenta.CREDENCIALES.mensaje, textoError(TipoErrorCuenta.CREDENCIALES, null, NivelDetalle.COMPLETO))
    }

    @Test
    fun `en release el error de Google muestra solo el codigo y la clase`() {
        val causa = androidx.credentials.exceptions.GetCredentialCancellationException(
            "During get sign-in intent, failure response from one tap: 16: [16] Account reauth failed.",
        )
        val texto = textoError(TipoErrorCuenta.GOOGLE_FALLO, causa, NivelDetalle.CODIGO)
        assertEquals("${TipoErrorCuenta.GOOGLE_FALLO.mensaje}\n\nCódigo 16 · GetCredentialCancellationException", texto)
    }

    @Test
    fun `el codigo de Firebase Auth tambien se muestra`() {
        assertEquals("Código ERROR_INVALID_CREDENTIAL · FirebaseAuthInvalidCredentialsException",
            DetalleError("FirebaseAuth", "ERROR_INVALID_CREDENTIAL", "x", "FirebaseAuthInvalidCredentialsException").codigoVisible)
        assertEquals("IllegalStateException", detalleError(IllegalStateException("sin código")).codigoVisible)
    }

    @Test
    fun `una cancelacion con codigo de Play services es un fallo, no el usuario`() {
        // Package/SHA-1 not accepted by Google: Credential Manager reports it as a "cancellation".
        assertEquals(TipoErrorCuenta.GOOGLE_FALLO, tipoCancelacionGoogle("During get sign-in intent, failure response from one tap: 16: [16] Account reauth failed."))
        assertEquals(TipoErrorCuenta.GOOGLE_FALLO, tipoCancelacionGoogle("10: Developer error"))
        assertEquals(TipoErrorCuenta.GOOGLE_CANCELADO, tipoCancelacionGoogle("activity is cancelled by the user."))
        assertEquals(TipoErrorCuenta.GOOGLE_CANCELADO, tipoCancelacionGoogle(null))
    }

    @Test
    fun `todos los tipos tienen mensaje`() {
        TipoErrorCuenta.entries.forEach { assert(it.mensaje.isNotBlank()) }
    }
}
