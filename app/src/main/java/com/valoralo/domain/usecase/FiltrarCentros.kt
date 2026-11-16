package com.valoralo.domain.usecase

import com.valoralo.domain.model.Centro
import java.text.Normalizer

/** Filters from the "Filtros" sheet on Inicio. Null means "any". */
data class FiltroCentros(
    val provincia: String? = null,
    val municipio: String? = null,
    val tipo: String? = null,
    /** Nivel de enseñanza: ESO, Bachillerato, FP Grado Medio… */
    val nivel: String? = null,
) {
    val activos: Int get() = listOfNotNull(provincia, municipio, tipo, nivel).size
    val hayActivos: Boolean get() = activos > 0

    /** A municipio only makes sense inside its provincia, so it is reset. */
    fun conProvincia(provincia: String?): FiltroCentros =
        copy(provincia = provincia, municipio = null)
}

/** Search by nombre or municipio (case- and accent-insensitive) plus the filters. */
fun List<Centro>.filtrar(texto: String, filtro: FiltroCentros): List<Centro> {
    val consulta = texto.normalizado()
    return filter { centro ->
        (consulta.isEmpty() ||
            centro.nombre.normalizado().contains(consulta) ||
            centro.municipio.normalizado().contains(consulta)) &&
            (filtro.provincia == null || centro.provincia == filtro.provincia) &&
            (filtro.municipio == null || centro.municipio == filtro.municipio) &&
            (filtro.tipo == null || centro.tipo == filtro.tipo) &&
            (filtro.nivel == null || centro.ensenanzas.any { it.nivel == filtro.nivel })
    }
}

private val MARCAS_DIACRITICAS = "\\p{Mn}+".toRegex()

internal fun String.normalizado(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD)
        .replace(MARCAS_DIACRITICAS, "")
