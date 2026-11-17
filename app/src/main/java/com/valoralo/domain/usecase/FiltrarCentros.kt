package com.valoralo.domain.usecase

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.PROVINCIAS_ANDALUCIA
import java.text.Normalizer

/**
 * A place in the "Municipio o localidad" filter. Nobody knows whether their town is a
 * municipio or a localidad, so both go in one list, each with its context. A municipio
 * includes all its localidades; a localidad (e.g. Aguadulce, in Roquetas de Mar) only itself.
 */
data class Lugar(
    val nombre: String,
    val municipio: String,
    val provincia: String,
    val esLocalidad: Boolean,
) {
    /** "Gines · Sevilla", "Aguadulce · Roquetas de Mar, Almería". */
    val etiqueta: String get() = if (esLocalidad) "$nombre · $municipio, $provincia" else "$nombre · $provincia"

    fun contiene(c: Centro): Boolean =
        c.provincia == provincia && c.municipio == municipio && (!esLocalidad || c.localidadOMunicipio == nombre)

    companion object {
        fun municipio(nombre: String, provincia: String) = Lugar(nombre, nombre, provincia, esLocalidad = false)
        fun localidad(nombre: String, municipio: String, provincia: String) = Lugar(nombre, municipio, provincia, esLocalidad = true)
    }
}

/**
 * Filters of Buscar, all multiple choice. An empty set means "any".
 * Order of the sections: provincia → municipio o localidad → tipo → enseñanzas.
 */
data class FiltroCentros(
    val provincias: Set<String> = emptySet(),
    val lugares: Set<Lugar> = emptySet(),
    val tipos: Set<String> = emptySet(),
    /** Nivel de enseñanza: ESO, Bachillerato, FP Grado Medio… */
    val niveles: Set<String> = emptySet(),
) {
    val activos: Int get() = provincias.size + lugares.size + tipos.size + niveles.size
    val hayActivos: Boolean get() = activos > 0

    /** Drops the places outside the chosen provincias. */
    fun coherente(): FiltroCentros =
        if (provincias.isEmpty()) this else copy(lugares = lugares.filter { it.provincia in provincias }.toSet())

    /** With places chosen and no provincia, the provincias they are in (for the summary). */
    fun provinciasDeducidas(): Set<String> =
        if (provincias.isNotEmpty()) emptySet() else lugares.map { it.provincia }.toCollection(LinkedHashSet())
}

/** A centro without its own localidad is in its municipio's (e.g. the capital). */
val Centro.localidadOMunicipio: String get() = localidad.ifBlank { municipio }

/**
 * Search by nombre, localidad or municipio (case- and accent-insensitive) plus the filters.
 * With text, the centros of the municipio or localidad searched come first ("Gines" → the
 * centros in Gines before "CEIP San Ginés" elsewhere).
 */
fun List<Centro>.filtrar(texto: String, filtro: FiltroCentros): List<Centro> {
    val consulta = texto.normalizado()
    val resultado = filter { centro ->
        (consulta.isEmpty() ||
            centro.nombre.normalizado().contains(consulta) ||
            centro.localidad.normalizado().contains(consulta) ||
            centro.municipio.normalizado().contains(consulta)) &&
            (filtro.provincias.isEmpty() || centro.provincia in filtro.provincias) &&
            (filtro.lugares.isEmpty() || filtro.lugares.any { it.contiene(centro) }) &&
            (filtro.tipos.isEmpty() || centro.tipo in filtro.tipos) &&
            (filtro.niveles.isEmpty() || centro.ensenanzas.any { it.nivel in filtro.niveles })
    }
    if (consulta.isEmpty()) return resultado
    // Stable sort: same rank keeps the catalogue order.
    return resultado.sortedBy { c ->
        val lugar = c.municipio.normalizado().startsWith(consulta) || c.localidad.normalizado().startsWith(consulta)
        if (lugar) 0 else 1
    }
}

data class OpcionesFiltro(
    val provincias: List<String> = PROVINCIAS_ANDALUCIA,
    /** Municipios and localidades (those different from their municipio) of the chosen provincias. */
    val lugares: List<Lugar> = emptyList(),
    val tipos: List<String> = emptyList(),
    val niveles: List<String> = emptyList(),
)

fun opcionesFiltro(centros: List<Centro>, filtro: FiltroCentros): OpcionesFiltro {
    val enProvincias = centros.filter { filtro.provincias.isEmpty() || it.provincia in filtro.provincias }
    val enLugares = enProvincias.filter { c -> filtro.lugares.isEmpty() || filtro.lugares.any { it.contiene(c) } }
    val lugares = enProvincias.flatMap { c ->
        val municipio = Lugar.municipio(c.municipio, c.provincia)
        val localidad = c.localidadOMunicipio
        if (localidad == c.municipio) listOf(municipio) else listOf(municipio, Lugar.localidad(localidad, c.municipio, c.provincia))
    }.distinct().sortedWith(compareBy<Lugar>({ it.nombre.normalizado() }, { it.esLocalidad }, { it.provincia }))
    return OpcionesFiltro(
        lugares = lugares,
        tipos = enLugares.map { it.tipo }.distinct().sorted(),
        niveles = enLugares.flatMap { c -> c.ensenanzas.map { it.nivel } }.distinct()
            .sortedBy { ORDEN_NIVELES.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE },
    )
}

/**
 * Places matching what is typed: name starting with it first, then a word inside the name
 * starting with it ("mar" → Roquetas de Mar), then any other match. Empty text: all.
 */
fun List<Lugar>.sugerir(texto: String): List<Lugar> {
    val q = texto.normalizado()
    if (q.isEmpty()) return this
    return mapNotNull { lugar ->
        val n = lugar.nombre.normalizado()
        when {
            n.startsWith(q) -> 0 to lugar
            n.split(' ', '-').any { it.startsWith(q) } -> 1 to lugar
            n.contains(q) -> 2 to lugar
            else -> null
        }
    }.sortedBy { it.first }.map { it.second }
}

fun <T> Set<T>.alternar(opcion: T): Set<T> = if (opcion in this) this - opcion else this + opcion

/** Summary of a closed section: null if nothing, the value if one, how many otherwise. */
fun resumenSeleccion(seleccion: Set<String>): String? = when (seleccion.size) {
    0 -> null
    1 -> seleccion.first()
    else -> seleccion.size.toString()
}

private val ORDEN_NIVELES = listOf(
    "Infantil", "Primaria", "ESO", "Bachillerato",
    "FP Grado Básico", "FP Grado Medio", "FP Grado Superior",
)

private val MARCAS_DIACRITICAS = "\\p{Mn}+".toRegex()

internal fun String.normalizado(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD)
        .replace(MARCAS_DIACRITICAS, "")
