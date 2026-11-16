package com.valoralo.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidacionRegistroTest {

    private val hoy = LocalDate.of(2026, 11, 16)

    @Test
    fun `edad exacta en anos cumplidos`() {
        assertEquals(14, edad(LocalDate.of(2012, 11, 16), hoy)) // cumple hoy
        assertEquals(13, edad(LocalDate.of(2012, 11, 17), hoy)) // cumple mañana
        assertEquals(40, edad(LocalDate.of(1986, 1, 1), hoy))
    }

    @Test
    fun `nacido el 29 de febrero`() {
        assertEquals(13, edad(LocalDate.of(2012, 2, 29), LocalDate.of(2026, 2, 28)))
        assertEquals(14, edad(LocalDate.of(2012, 2, 29), LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun `fecha de nacimiento`() {
        assertNull(validarFechaNacimiento(LocalDate.of(2012, 11, 16), hoy))
        assertEquals(ErrorRegistro.MENOR_DE_14, validarFechaNacimiento(LocalDate.of(2012, 11, 17), hoy))
        assertEquals(ErrorRegistro.FECHA_NO_VALIDA, validarFechaNacimiento(LocalDate.of(2027, 1, 1), hoy))
        assertEquals(ErrorRegistro.FECHA_NO_VALIDA, validarFechaNacimiento(LocalDate.of(1900, 1, 1), hoy))
        assertEquals(ErrorRegistro.FALTA_FECHA, validarFechaNacimiento(null, hoy))
    }

    @Test
    fun `alias valido`() {
        assertNull(validarAlias("Lucía R."))
        assertNull(validarAlias("  ana_g-2  ")) // se recorta
        assertEquals("ana_g-2", normalizarAlias("  ana_g-2  "))
        assertEquals("Ana García", normalizarAlias("Ana    García"))
    }

    @Test
    fun `alias no valido`() {
        assertEquals(ErrorRegistro.ALIAS_CORTO, validarAlias("Lu"))
        assertEquals(ErrorRegistro.ALIAS_CORTO, validarAlias("   "))
        assertEquals(ErrorRegistro.ALIAS_LARGO, validarAlias("x".repeat(31)))
        // No emails, links or odd symbols: the alias is public in reviews.
        assertEquals(ErrorRegistro.ALIAS_CARACTERES, validarAlias("ana@correo.es"))
        assertEquals(ErrorRegistro.ALIAS_CARACTERES, validarAlias("www.ejemplo.com/x"))
        assertEquals(ErrorRegistro.ALIAS_CARACTERES, validarAlias("<script>"))
    }

    @Test
    fun `correo`() {
        assertNull(validarCorreo("ana@ejemplo.es"))
        assertNull(validarCorreo("  ana.g+valoralo@ejemplo.com "))
        assertEquals(ErrorRegistro.CORREO_NO_VALIDO, validarCorreo("ana"))
        assertEquals(ErrorRegistro.CORREO_NO_VALIDO, validarCorreo("ana@"))
        assertEquals(ErrorRegistro.CORREO_NO_VALIDO, validarCorreo(""))
    }

    @Test
    fun `contrasena de al menos 8 caracteres con letras y numeros`() {
        assertNull(validarContrasena("valoralo26"))
        assertEquals(ErrorRegistro.CONTRASENA_DEBIL, validarContrasena("corta1"))
        assertEquals(ErrorRegistro.CONTRASENA_DEBIL, validarContrasena("sololetras"))
        assertEquals(ErrorRegistro.CONTRASENA_DEBIL, validarContrasena("12345678"))
    }

    @Test
    fun `todos los errores tienen mensaje en espanol`() {
        ErrorRegistro.entries.forEach { assertTrue(it.mensaje.isNotBlank()) }
        assertFalse(ErrorRegistro.MENOR_DE_14.mensaje.contains("error", ignoreCase = true))
    }
}
