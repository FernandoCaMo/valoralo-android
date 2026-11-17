package com.valoralo.domain.usecase

import com.valoralo.domain.model.Valoracion

/** Default of config/likes.maxPorHora (the rules use the same default). */
const val MAX_LIKES_POR_HORA = 60

/**
 * The count shown next to the heart. [base] is the review's nLikes as last read and
 * [dadoAlCargar] whether my like was there at that moment: tapping changes the number at once,
 * and when the server count arrives (already including my like) it is not counted twice.
 */
fun contadorVisible(base: Int, dadoAlCargar: Boolean, dadoAhora: Boolean): Int = maxOf(
    0,
    base + when {
        dadoAhora && !dadoAlCargar -> 1
        !dadoAhora && dadoAlCargar -> -1
        else -> 0
    },
)

/** What the heart shows. */
data class EstadoLike(val dado: Boolean, val n: Int)

/**
 * A tap not yet reflected in the server count: [base] is the count when it was tapped.
 * As soon as the review's count changes (alCambiarLike has run), it no longer applies.
 */
data class AjusteLike(val base: Int, val dadoAntes: Boolean, val dadoAhora: Boolean) {
    fun caducado(nServidor: Int): Boolean = nServidor != base
}

fun estadoLike(nServidor: Int, dadoServidor: Boolean, ajuste: AjusteLike?): EstadoLike =
    if (ajuste == null || ajuste.caducado(nServidor)) EstadoLike(dadoServidor, nServidor)
    else EstadoLike(ajuste.dadoAhora, contadorVisible(ajuste.base, ajuste.dadoAntes, ajuste.dadoAhora))

private fun textoLikes(n: Int) = when (n) {
    0 -> "sin likes"
    1 -> "1 like"
    else -> "$n likes"
}

/** Not visible: what TalkBack reads on the heart ("Dar like, 12 likes"). */
fun descripcionLike(n: Int, dado: Boolean, propia: Boolean): String = when {
    propia -> textoLikes(n)
    dado -> "Quitar like, ${textoLikes(n)}"
    else -> "Dar like, ${textoLikes(n)}"
}

enum class OrdenResenas(val etiqueta: String) {
    RECIENTES("Más recientes"),
    POPULARES("Más populares"),
}

fun List<Valoracion>.ordenadas(orden: OrdenResenas): List<Valoracion> = when (orden) {
    OrdenResenas.RECIENTES -> sortedByDescending { it.fecha }
    OrdenResenas.POPULARES -> sortedWith(compareByDescending<Valoracion> { it.nLikes }.thenByDescending { it.fecha })
}

/** usuarios/{uid}/limites/likes, written with each like (the rules check it). */
data class LimiteLikes(val inicioMs: Long, val n: Int)

sealed interface PasoLimite {
    /** No counter or the hour is over: start a new hour at 1. */
    data object Empezar : PasoLimite
    data object Sumar : PasoLimite
    data object Lleno : PasoLimite
}

fun siguientePasoLimite(actual: LimiteLikes?, ahoraMs: Long, max: Int): PasoLimite = when {
    actual == null || ahoraMs >= actual.inicioMs + 3_600_000L -> PasoLimite.Empezar
    actual.n >= max -> PasoLimite.Lleno
    else -> PasoLimite.Sumar
}
