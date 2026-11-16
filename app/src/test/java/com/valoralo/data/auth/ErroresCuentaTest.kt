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
    fun `todos los tipos tienen mensaje`() {
        TipoErrorCuenta.entries.forEach { assert(it.mensaje.isNotBlank()) }
    }
}
