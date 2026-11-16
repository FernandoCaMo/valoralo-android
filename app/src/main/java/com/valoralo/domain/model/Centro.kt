package com.valoralo.domain.model

/** Centro educativo público. Mirrors `centros/{codigo}` in Firestore (doc 02). */
data class Centro(
    val codigo: String,
    val nombre: String,
    val tipo: String,
    val direccion: String,
    val cp: String,
    /** Población del centro; puede no coincidir con el municipio (Aguadulce → Roquetas de Mar). */
    val localidad: String = "",
    val municipio: String,
    /** Código INE del municipio. */
    val codMunicipio: String = "",
    val provincia: String,
    val comunidad: String = "Andalucía",
    val telefono: String? = null,
    val email: String? = null,
    val web: String? = null,
    val fotoUrl: String? = null,
    val ensenanzas: List<Ensenanza> = emptyList(),
    val departamentos: List<String> = emptyList(),
    val stats: Stats = Stats(),
    val fuente: Fuente? = null,
)

/** Origin of the catalogue data (CC BY 4.0 requires attribution). */
data class Fuente(
    val dataset: String,
    val curso: String,
)

data class Ensenanza(
    /** ESO, Bachillerato, FP Grado Medio, FP Grado Superior, Infantil, Primaria… */
    val nivel: String,
    val nombre: String,
)

val PROVINCIAS_ANDALUCIA = listOf(
    "Almería", "Cádiz", "Córdoba", "Granada", "Huelva", "Jaén", "Málaga", "Sevilla",
)
