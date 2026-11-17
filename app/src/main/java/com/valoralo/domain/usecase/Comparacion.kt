package com.valoralo.domain.usecase

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.StatsGrupo

/** The best value of a row is highlighted only if every centro in it has at least this many reviews. */
const val MIN_RESENAS_PARA_DESTACAR = 3
const val MAX_CENTROS_COMPARADOR = 3

/** One cell: average and number of reviews. [disponible] = false when the centro doesn't have that department. */
data class Celda(val nota: Double?, val n: Int, val mejor: Boolean = false, val disponible: Boolean = true)

data class Fila(val etiqueta: String, val celdas: List<Celda>)

data class FilaEnsenanza(val nivel: String, val tiene: List<Boolean>)

data class Comparacion(val notas: List<Fila>, val departamentos: List<Fila>, val ensenanzas: List<FilaEnsenanza>)

private val ORDEN_NIVELES = listOf(
    "Infantil", "Primaria", "ESO", "Bachillerato", "FP Grado Básico", "FP Grado Medio", "FP Grado Superior",
    "Educación Especial", "Personas Adultas", "Idiomas", "Música", "Danza", "Artes Plásticas y Diseño",
    "Enseñanzas Artísticas Superiores", "Arte Dramático", "Enseñanzas Deportivas",
)

private fun destacar(celdas: List<Celda>): List<Celda> {
    val todasComparables = celdas.size >= 2 && celdas.all { it.disponible && it.nota != null && it.n >= MIN_RESENAS_PARA_DESTACAR }
    if (!todasComparables) return celdas
    val mejor = celdas.maxOf { it.nota!! }
    return celdas.map { it.copy(mejor = kotlin.math.abs(it.nota!! - mejor) < 1e-9) }
}

private fun fila(etiqueta: String, grupos: List<StatsGrupo>, valor: (StatsGrupo) -> Pair<Double?, Int>) =
    Fila(etiqueta, destacar(grupos.map { g -> valor(g).let { (nota, n) -> Celda(nota, n) } }))

fun comparar(centros: List<Centro>, filtro: FiltroRol): Comparacion {
    val grupos = centros.map { it.stats.para(filtro) }

    val notas = listOf(
        fila("Nota global", grupos) { it.global to it.n },
        fila("Centro en general", grupos) { it.centro to it.n },
        fila("Aulas e instalaciones", grupos) { it.instalaciones to it.n },
        fila("Profesorado", grupos) { it.profesorado to it.nProfesorado },
    )

    // Departments of any of the centros, plus those rated as "Otro departamento".
    val nombres = centros.zip(grupos).flatMap { (c, g) -> c.departamentos + g.porDepartamento.keys }
        .distinct().sortedBy { it.normalizado() }
    val departamentos = nombres.map { d ->
        Fila(d, destacar(centros.zip(grupos).map { (c, g) ->
            val stats = g.porDepartamento[d]
            when {
                stats != null -> Celda(stats.media, stats.n)
                d in c.departamentos -> Celda(null, 0)
                else -> Celda(null, 0, disponible = false)
            }
        }))
    }

    val niveles = centros.flatMap { c -> c.ensenanzas.map { it.nivel } }.distinct()
        .sortedWith(compareBy<String> { ORDEN_NIVELES.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenBy { it })
    val ensenanzas = niveles.map { nivel -> FilaEnsenanza(nivel, centros.map { c -> c.ensenanzas.any { it.nivel == nivel } }) }

    return Comparacion(notas, departamentos, ensenanzas)
}
