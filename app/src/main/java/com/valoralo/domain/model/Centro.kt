package com.valoralo.domain.model

/** Centro educativo público. Mirrors `centros/{codigo}` in Firestore (doc 02). */
data class Centro(
    val codigo: String,
    val nombre: String,
    val tipo: String,
    val direccion: String,
    val cp: String,
    val municipio: String,
    val provincia: String,
    val comunidad: String = "Andalucía",
    val telefono: String? = null,
    val email: String? = null,
    val web: String? = null,
    val fotoUrl: String? = null,
    val ensenanzas: List<Ensenanza> = emptyList(),
    val departamentos: List<String> = emptyList(),
    val stats: Stats = Stats(),
)

data class Ensenanza(
    /** ESO, Bachillerato, FP Grado Medio, FP Grado Superior, Infantil, Primaria… */
    val nivel: String,
    val nombre: String,
)

val PROVINCIAS_ANDALUCIA = listOf(
    "Almería", "Cádiz", "Córdoba", "Granada", "Huelva", "Jaén", "Málaga", "Sevilla",
)
