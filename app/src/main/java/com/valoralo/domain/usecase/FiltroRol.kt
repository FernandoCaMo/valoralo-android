package com.valoralo.domain.usecase

import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Stats
import com.valoralo.domain.model.StatsGrupo
import com.valoralo.domain.model.Valoracion

/** Role chips on the centro detail: Todos · Docentes · Estudiantes · Otros. */
enum class FiltroRol(val rol: Rol?) {
    TODOS(null),
    DOCENTES(Rol.DOCENTE),
    ESTUDIANTES(Rol.ESTUDIANTE),
    OTROS(Rol.OTRO),
}

fun Stats.para(filtro: FiltroRol): StatsGrupo = when (filtro) {
    FiltroRol.TODOS -> total
    FiltroRol.DOCENTES -> docente
    FiltroRol.ESTUDIANTES -> estudiante
    FiltroRol.OTROS -> otro
}

/** Published reviews of the selected role, newest first. */
fun List<Valoracion>.filtrarPor(filtro: FiltroRol): List<Valoracion> =
    filter { it.estado == EstadoValoracion.PUBLICADA }
        .filter { filtro.rol == null || it.rolAutor == filtro.rol }
        .sortedByDescending { it.fecha }
