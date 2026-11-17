package com.valoralo.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/** 4.25 → "4,3"; null → "–". */
fun formatearNota(nota: Double?): String {
    if (nota == null) return "–"
    return BigDecimal.valueOf(nota).setScale(1, RoundingMode.HALF_UP)
        .toPlainString().replace('.', ',')
}

/** 0 → "sin reseñas", 1 → "1 reseña", 1240 → "1.240 reseñas". */
fun textoResenas(n: Int): String = when (n) {
    0 -> "sin reseñas"
    1 -> "1 reseña"
    else -> miles(n) + " reseñas"
}

/** Button of the filters: "Ver 1 centro", "Ver 3.711 centros". */
fun textoVerCentros(n: Int): String = if (n == 1) "Ver 1 centro" else "Ver ${miles(n)} centros"

// Always group thousands with a dot (es-ES alone skips it for 4-digit numbers).
private fun miles(n: Int): String =
    DecimalFormat("#,##0", DecimalFormatSymbols().apply { groupingSeparator = '.' }).format(n)
