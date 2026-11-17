package com.valoralo.ui.home

/** Time each featured review stays before the carousel moves on by itself. */
const val MS_POR_RESENA = 9_000L

/**
 * The carousel turns by itself only with 2+ reviews, while nobody has touched it on this visit,
 * and never with animations turned off or a screen reader on (accessibility settings).
 */
fun debeRotarSolo(n: Int, tocado: Boolean, animacionesDesactivadas: Boolean, lectorDePantalla: Boolean): Boolean =
    n >= 2 && !tocado && !animacionesDesactivadas && !lectorDePantalla

/**
 * The pager has many virtual pages so that it always moves forward (12 → 1 without
 * rewinding through all of them). Start in the middle, on a page showing review 0.
 */
fun paginasVirtuales(n: Int): Int = if (n <= 1) n else n * 1_000

fun paginaInicial(n: Int): Int = if (n <= 1) 0 else paginasVirtuales(n) / 2 - (paginasVirtuales(n) / 2) % n

fun resenaDePagina(pagina: Int, n: Int): Int = if (n == 0) 0 else Math.floorMod(pagina, n)
