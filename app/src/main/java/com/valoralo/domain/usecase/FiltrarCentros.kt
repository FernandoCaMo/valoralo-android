package com.valoralo.domain.usecase

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.PROVINCIAS_ANDALUCIA
import java.text.Normalizer

/**
 * Filters of Buscar, all multiple choice. An empty set means "any".
 * Order of the sections: provincia → ciudad (municipio) → localidad → tipo → enseñanzas.
 */
data class FiltroCentros(
    val provincias: Set<String> = emptySet(),
    val municipios: Set<String> = emptySet(),
    val localidades: Set<String> = emptySet(),
    val tipos: Set<String> = emptySet(),
    /** Nivel de enseñanza: ESO, Bachillerato, FP Grado Medio… */
    val niveles: Set<String> = emptySet(),
) {
    val activos: Int get() = provincias.size + municipios.size + localidades.size + tipos.size + niveles.size
    val hayActivos: Boolean get() = activos > 0

    /** Drops the municipios and localidades that no longer fit what is chosen above them. */
    fun coherente(centros: List<Centro>): FiltroCentros {
        val enProvincias = centros.filter { provincias.isEmpty() || it.provincia in provincias }
        val municipiosValidos = municipios.intersect(enProvincias.map { it.municipio }.toSet())
        val enMunicipios = enProvincias.filter { municipiosValidos.isEmpty() || it.municipio in municipiosValidos }
        return copy(
            municipios = municipiosValidos,
            localidades = localidades.intersect(enMunicipios.map { it.localidadOMunicipio }.toSet()),
        )
    }

    /** With a ciudad chosen and no provincia, the provincias it belongs to (for the summary). */
    fun provinciasDeducidas(centros: List<Centro>): Set<String> =
        if (provincias.isNotEmpty() || municipios.isEmpty()) emptySet()
        else centros.filter { it.municipio in municipios }.map { it.provincia }.toCollection(LinkedHashSet())
}

/** A centro without its own localidad is in its municipio's (e.g. the capital). */
val Centro.localidadOMunicipio: String get() = localidad.ifBlank { municipio }

/** Search by nombre, localidad or municipio (case- and accent-insensitive) plus the filters. */
fun List<Centro>.filtrar(texto: String, filtro: FiltroCentros): List<Centro> {
    val consulta = texto.normalizado()
    fun Set<String>.admite(valor: String) = isEmpty() || valor in this
    return filter { centro ->
        (consulta.isEmpty() ||
            centro.nombre.normalizado().contains(consulta) ||
            centro.localidad.normalizado().contains(consulta) ||
            centro.municipio.normalizado().contains(consulta)) &&
            filtro.provincias.admite(centro.provincia) &&
            filtro.municipios.admite(centro.municipio) &&
            filtro.localidades.admite(centro.localidadOMunicipio) &&
            filtro.tipos.admite(centro.tipo) &&
            (filtro.niveles.isEmpty() || centro.ensenanzas.any { it.nivel in filtro.niveles })
    }
}

data class OpcionesFiltro(
    val provincias: List<String> = PROVINCIAS_ANDALUCIA,
    /** Municipios of the chosen provincias (all if none). */
    val municipios: List<String> = emptyList(),
    /** Localidades of the chosen provincias and municipios. */
    val localidades: List<String> = emptyList(),
    val tipos: List<String> = emptyList(),
    val niveles: List<String> = emptyList(),
)

fun opcionesFiltro(centros: List<Centro>, filtro: FiltroCentros): OpcionesFiltro {
    val enProvincias = centros.filter { filtro.provincias.isEmpty() || it.provincia in filtro.provincias }
    val enMunicipios = enProvincias.filter { filtro.municipios.isEmpty() || it.municipio in filtro.municipios }
    return OpcionesFiltro(
        municipios = enProvincias.map { it.municipio }.distinct().sortedWith(ORDEN_ALFABETICO),
        localidades = enMunicipios.map { it.localidadOMunicipio }.distinct().sortedWith(ORDEN_ALFABETICO),
        tipos = enMunicipios.map { it.tipo }.distinct().sorted(),
        niveles = enMunicipios.flatMap { c -> c.ensenanzas.map { it.nivel } }.distinct()
            .sortedBy { ORDEN_NIVELES.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE },
    )
}

fun Set<String>.alternar(opcion: String): Set<String> = if (opcion in this) this - opcion else this + opcion

/** Summary of a closed section: null if nothing, the value if one, how many otherwise. */
fun resumenSeleccion(seleccion: Set<String>): String? = when (seleccion.size) {
    0 -> null
    1 -> seleccion.first()
    else -> seleccion.size.toString()
}

/** Options search inside a section (long lists: ciudades, localidades). */
fun List<String>.buscar(texto: String): List<String> {
    val q = texto.normalizado()
    return if (q.isEmpty()) this else filter { it.normalizado().contains(q) }
}

private val ORDEN_NIVELES = listOf(
    "Infantil", "Primaria", "ESO", "Bachillerato",
    "FP Grado Básico", "FP Grado Medio", "FP Grado Superior",
)

/** Alphabetical ignoring accents (Écija next to Estepa, not at the end). */
private val ORDEN_ALFABETICO = compareBy<String> { it.normalizado() }

private val MARCAS_DIACRITICAS = "\\p{Mn}+".toRegex()

internal fun String.normalizado(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD)
        .replace(MARCAS_DIACRITICAS, "")
