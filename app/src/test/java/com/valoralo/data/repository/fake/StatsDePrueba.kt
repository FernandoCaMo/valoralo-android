package com.valoralo.data.repository.fake

import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Stats
import com.valoralo.domain.model.StatsDepartamento
import com.valoralo.domain.model.StatsGrupo
import com.valoralo.domain.model.Valoracion

/**
 * Computes [Stats] for the sample data so averages match the sample reviews,
 * following doc 02 ("Cálculo de las notas"). In production the stats are
 * written by the `onValoracionWrite` Cloud Function (Phase 5).
 */
internal fun calcularStatsDePrueba(valoraciones: List<Valoracion>): Stats {
    val publicadas = valoraciones.filter { it.estado == EstadoValoracion.PUBLICADA }
    return Stats(
        total = grupo(publicadas),
        docente = grupo(publicadas.filter { it.rolAutor == Rol.DOCENTE }),
        estudiante = grupo(publicadas.filter { it.rolAutor == Rol.ESTUDIANTE }),
        otro = grupo(publicadas.filter { it.rolAutor == Rol.OTRO }),
    )
}

private fun grupo(valoraciones: List<Valoracion>): StatsGrupo {
    if (valoraciones.isEmpty()) return StatsGrupo()
    // Per review: profesorado = mean of its departments (null if none);
    // global = mean of its dimensions, equal weight.
    val profesorado = valoraciones.map { v -> v.profesorado.map { it.nota.toDouble() }.mediaOrNull() }
    val global = valoraciones.zip(profesorado) { v, prof ->
        listOfNotNull(v.centro.nota.toDouble(), v.instalaciones.nota.toDouble(), prof).average()
    }
    // Per centro: each average only over the reviews that have that data.
    return StatsGrupo(
        n = valoraciones.size,
        global = global.mediaOrNull(),
        centro = valoraciones.map { it.centro.nota.toDouble() }.mediaOrNull(),
        instalaciones = valoraciones.map { it.instalaciones.nota.toDouble() }.mediaOrNull(),
        profesorado = profesorado.filterNotNull().mediaOrNull(),
        porDepartamento = valoraciones
            .flatMap { it.profesorado }
            .groupBy { it.departamento }
            .mapValues { (_, notas) -> StatsDepartamento(notas.size, notas.map { it.nota }.average()) },
    )
}

private fun List<Double>.mediaOrNull(): Double? = if (isEmpty()) null else average()
