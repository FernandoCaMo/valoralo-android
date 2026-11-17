package com.valoralo.domain.model

import java.time.LocalDate

/** One featured review on Inicio (destacadas/actual, written by calcularDestacadas). */
data class Destacada(
    val codigo: String,
    val nombreCentro: String,
    val municipio: String?,
    val tipo: String?,
    val alias: String,
    val rol: Rol,
    val verificado: Boolean,
    /** Overall note of this review (mean of its dimensions), one decimal. */
    val nota: Double,
    val extracto: String,
    val fecha: LocalDate?,
    /** Id of the review (= its author's uid, already public as the document id), to like it. Null in old data. */
    val autorUid: String? = null,
    /** Likes when destacadas/actual was last calculated (at most an hour ago). */
    val nLikes: Int = 0,
)

data class Destacadas(
    val resenas: List<Destacada> = emptyList(),
    /** From config/destacadas: below this, Inicio shows the welcome text instead. */
    val minimoParaMostrar: Int = 3,
) {
    val mostrarCarrusel: Boolean get() = resenas.isNotEmpty() && resenas.size >= minimoParaMostrar
}
