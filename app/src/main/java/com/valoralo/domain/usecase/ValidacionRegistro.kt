package com.valoralo.domain.usecase

import java.time.LocalDate
import java.time.Period

/** Minimum age to create an account (doc 01: under 14 need parental consent in Spain). */
const val EDAD_MINIMA = 14

enum class ErrorRegistro(val mensaje: String) {
    FALTA_FECHA("Indica tu fecha de nacimiento."),
    FECHA_NO_VALIDA("La fecha de nacimiento no es válida."),
    MENOR_DE_14("Valoralo es para mayores de 14 años. No podemos crear tu cuenta."),
    ALIAS_CORTO("El alias debe tener al menos 3 caracteres."),
    ALIAS_LARGO("El alias puede tener como máximo 30 caracteres."),
    ALIAS_CARACTERES("El alias solo puede llevar letras, números, espacios y . _ -"),
    CORREO_NO_VALIDO("Escribe un correo electrónico válido."),
    CONTRASENA_DEBIL("La contraseña debe tener al menos 8 caracteres, con letras y números."),
}

fun edad(fechaNacimiento: LocalDate, hoy: LocalDate): Int = Period.between(fechaNacimiento, hoy).years

fun validarFechaNacimiento(fecha: LocalDate?, hoy: LocalDate): ErrorRegistro? = when {
    fecha == null -> ErrorRegistro.FALTA_FECHA
    fecha.isAfter(hoy) || edad(fecha, hoy) > 120 -> ErrorRegistro.FECHA_NO_VALIDA
    edad(fecha, hoy) < EDAD_MINIMA -> ErrorRegistro.MENOR_DE_14
    else -> null
}

private val ESPACIOS = "\\s+".toRegex()
private val CARACTERES_ALIAS = "^[\\p{L}\\p{N} ._-]+$".toRegex()

/** Trims and collapses inner spaces. */
fun normalizarAlias(alias: String): String = alias.trim().replace(ESPACIOS, " ")

/** The alias is public (shown in reviews), so no emails, links or markup. */
fun validarAlias(alias: String): ErrorRegistro? {
    val a = normalizarAlias(alias)
    return when {
        a.length < 3 -> ErrorRegistro.ALIAS_CORTO
        a.length > 30 -> ErrorRegistro.ALIAS_LARGO
        !CARACTERES_ALIAS.matches(a) -> ErrorRegistro.ALIAS_CARACTERES
        else -> null
    }
}

private val CORREO = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$".toRegex()

fun validarCorreo(correo: String): ErrorRegistro? =
    if (CORREO.matches(correo.trim())) null else ErrorRegistro.CORREO_NO_VALIDO

fun validarContrasena(contrasena: String): ErrorRegistro? =
    if (contrasena.length >= 8 && contrasena.any { it.isLetter() } && contrasena.any { it.isDigit() }) null
    else ErrorRegistro.CONTRASENA_DEBIL
