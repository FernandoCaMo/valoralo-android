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
    ALIAS_CONTACTO("El alias no puede llevar teléfonos, correos ni enlaces."),
    ALIAS_RESERVADO("Ese alias puede confundirse con un docente verificado, la moderación o la app. Elige otro."),
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

const val ALIAS_MAX = 30

/**
 * Same words as functions/src/alias.ts (keep in step): compared on the alias reduced to letters,
 * without accents, capitals, separators or leet ("D0c-énte" → "docente").
 */
val PALABRAS_RESERVADAS_ALIAS = listOf(
    "docente", "verificad", "moderador", "moderacion", "admin", "valoralo",
    "juntadeandalucia", "consejeria", "soporte", "oficial",
)
private const val MAX_DIGITOS_ALIAS = 5
private val ENLACE_ALIAS =
    "(^|[^\\p{L}\\p{N}])www([^\\p{L}\\p{N}]|$)|https?|[\\p{L}\\p{N}]\\.(com|es|net|org|eu|info|io|me|app|tk|xyz|link|ly)([^\\p{L}]|$)"
        .toRegex(RegexOption.IGNORE_CASE)
private val LEET = mapOf('0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's', '7' to 't', '@' to 'a', '$' to 's')

private fun soloLetras(alias: String): String =
    java.text.Normalizer.normalize(alias.lowercase(), java.text.Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .map { LEET[it] ?: it }.joinToString("")
        .replace("[^a-zñ]".toRegex(), "")

/**
 * The alias is public (shown in reviews), so no contact data, markup or words that pass for a role,
 * the moderation or the app. The server checks the same and also insults (functions/src/alias.ts).
 */
fun validarAlias(alias: String): ErrorRegistro? {
    val a = normalizarAlias(alias)
    return when {
        a.length < 3 -> ErrorRegistro.ALIAS_CORTO
        a.length > ALIAS_MAX -> ErrorRegistro.ALIAS_LARGO
        !CARACTERES_ALIAS.matches(a) -> ErrorRegistro.ALIAS_CARACTERES
        a.count { it.isDigit() } > MAX_DIGITOS_ALIAS || ENLACE_ALIAS.containsMatchIn(a) -> ErrorRegistro.ALIAS_CONTACTO
        PALABRAS_RESERVADAS_ALIAS.any { soloLetras(a).contains(it) } -> ErrorRegistro.ALIAS_RESERVADO
        else -> null
    }
}

private val CORREO = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$".toRegex()

fun validarCorreo(correo: String): ErrorRegistro? =
    if (CORREO.matches(correo.trim())) null else ErrorRegistro.CORREO_NO_VALIDO

fun validarContrasena(contrasena: String): ErrorRegistro? =
    if (contrasena.length >= 8 && contrasena.any { it.isLetter() } && contrasena.any { it.isDigit() }) null
    else ErrorRegistro.CONTRASENA_DEBIL
