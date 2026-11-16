package com.valoralo.domain.model

/**
 * Aggregated ratings of a centro. Written only by the backend
 * (`onValoracionWrite` Cloud Function); the app only reads them.
 */
data class Stats(
    val total: StatsGrupo = StatsGrupo(),
    val docente: StatsGrupo = StatsGrupo(),
    val estudiante: StatsGrupo = StatsGrupo(),
    val otro: StatsGrupo = StatsGrupo(),
)

/**
 * Averages for one group of authors, unrounded (see doc 02, "Cálculo de las notas").
 * An average is null when no review of the group has that data.
 */
data class StatsGrupo(
    val n: Int = 0,
    val global: Double? = null,
    val centro: Double? = null,
    val instalaciones: Double? = null,
    val profesorado: Double? = null,
    val porDepartamento: Map<String, StatsDepartamento> = emptyMap(),
)

data class StatsDepartamento(
    val n: Int,
    val media: Double,
)
