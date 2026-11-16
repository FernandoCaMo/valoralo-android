package com.valoralo.domain.usecase

/** Same rules as functions/src/verificacion/logica.ts. */
enum class ErrorSolicitud(val mensaje: String) {
    NOMBRE("Escribe tu nombre y apellidos tal como aparecen en el documento."),
    CSV("El código CSV no es válido. Cópialo tal como aparece en el documento."),
    PDF_GRANDE("El PDF puede ocupar como máximo 5 MB."),
}

const val TAMANO_MAXIMO_PDF = 5L * 1024 * 1024

private val NOMBRE = "^[\\p{L}][\\p{L} '’.-]*$".toRegex()

fun validarNombreDeclarado(nombre: String): ErrorSolicitud? {
    val n = nombre.trim().replace("\\s+".toRegex(), " ")
    val palabras = n.split(" ")
    return if (n.length > 100 || palabras.size < 2 || palabras.any { it.length < 2 } || !NOMBRE.matches(n)) ErrorSolicitud.NOMBRE else null
}

fun normalizarCsv(csv: String): String = csv.uppercase().replace("[\\s.-]+".toRegex(), "")

fun validarCsv(csv: String): ErrorSolicitud? =
    if ("^[A-Z0-9]{10,64}$".toRegex().matches(normalizarCsv(csv))) null else ErrorSolicitud.CSV

fun validarPdf(bytes: Long?): ErrorSolicitud? =
    if (bytes != null && bytes >= TAMANO_MAXIMO_PDF) ErrorSolicitud.PDF_GRANDE else null
