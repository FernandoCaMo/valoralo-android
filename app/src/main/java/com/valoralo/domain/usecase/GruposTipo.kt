package com.valoralo.domain.usecase

/**
 * Types of centro grouped with a name people understand (1.0.8). The filter shows the groups; each
 * group selects all its acronyms. The acronym stays visible in small print.
 * Acronyms of the Junta's directory (all present in the catalogue on 22/11/2026).
 */
enum class GrupoTipo(val nombre: String, val siglas: List<String>) {
    COLEGIOS("Colegios", listOf("CEIP", "CEPR", "CPR")),
    INSTITUTOS("Institutos", listOf("IES", "SES")),
    FP("Formación Profesional", listOf("CIFP")),
    ADULTOS("Personas adultas", listOf("CEPER", "SEP", "IPEP", "SIPEP")),
    IDIOMAS("Idiomas", listOf("EOI")),
    MUSICA_DANZA("Música y danza", listOf("CEM", "CPM", "CSM", "CPD", "CSD")),
    ARTE_DISENO("Arte y diseño", listOf("EA", "EASD", "ESAD")),
    ESPECIAL("Educación especial", listOf("CEEE")),
    ;

    companion object {
        fun de(tipo: String): GrupoTipo? = entries.firstOrNull { tipo in it.siglas }
    }
}

/** A group in the filter: the acronyms present in the current options, in the group's order. */
data class OpcionGrupo(val grupo: GrupoTipo?, val nombre: String, val siglas: List<String>)

/**
 * Groups the available types. Unknown acronyms (a new type in the dataset) get their own option
 * named after the acronym, so nothing becomes impossible to filter.
 */
fun agruparTipos(tipos: List<String>): List<OpcionGrupo> {
    val conocidos = GrupoTipo.entries.mapNotNull { g ->
        val presentes = g.siglas.filter { it in tipos }
        if (presentes.isEmpty()) null else OpcionGrupo(g, g.nombre, presentes)
    }
    val otros = tipos.filter { GrupoTipo.de(it) == null }.sorted().map { OpcionGrupo(null, it, listOf(it)) }
    return conocidos + otros
}
