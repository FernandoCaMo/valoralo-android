package com.valoralo.domain.usecase

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Stats
import com.valoralo.domain.model.StatsGrupo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 1.0.8: order of Buscar's results and type groups in the filters. */
class OrdenYGruposTest {

    private fun c(nombre: String, nota: Double?, n: Int, tipo: String = "IES") = Centro(
        codigo = nombre, nombre = nombre, tipo = tipo, direccion = "", cp = "", municipio = "Sevilla", provincia = "Sevilla",
        stats = Stats(total = StatsGrupo(n = n, global = nota)),
    )

    private val centros = listOf(
        c("Beta", 4.9, 1), // high but only one review
        c("Alfa", 4.1, 7),
        c("Delta", null, 0),
        c("Gamma", 4.5, 3),
        c("Épsilon", 3.2, 12),
    )

    @Test
    fun mejorValorados_conMenosDeTresResenasDetras_ySinNotaAlFinal() {
        assertEquals(listOf("Gamma", "Alfa", "Épsilon", "Beta", "Delta"), centros.ordenar(OrdenCentros.MEJOR_VALORADOS).map { it.nombre })
    }

    @Test
    fun masResenas() {
        assertEquals(listOf("Épsilon", "Alfa", "Gamma", "Beta", "Delta"), centros.ordenar(OrdenCentros.MAS_RESENAS).map { it.nombre })
    }

    @Test
    fun nombreSinTildesYRelevanciaRespetaElOrdenDeEntrada() {
        assertEquals(listOf("Alfa", "Beta", "Delta", "Épsilon", "Gamma"), centros.ordenar(OrdenCentros.NOMBRE).map { it.nombre })
        assertEquals(centros, centros.ordenar(OrdenCentros.RELEVANCIA))
    }

    @Test
    fun gruposDeTipoConNombreYSiglas() {
        val grupos = agruparTipos(listOf("IES", "CEIP", "SEP", "CEPER", "EOI", "CPR", "XYZ"))
        assertEquals(listOf("Colegios", "Institutos", "Personas adultas", "Idiomas", "XYZ"), grupos.map { it.nombre })
        assertEquals(listOf("CEIP", "CPR"), grupos.first { it.nombre == "Colegios" }.siglas)
        assertEquals(listOf("CEPER", "SEP"), grupos.first { it.nombre == "Personas adultas" }.siglas)
        // An unknown acronym keeps its own option (nothing becomes impossible to filter).
        assertNull(grupos.last().grupo)
    }

    @Test
    fun todasLasSiglasDelCatalogoTienenGrupo() {
        val catalogo = listOf("CEIP", "IES", "SEP", "CEPER", "CPR", "CEPR", "EOI", "CEM", "CPM", "CEEE", "CIFP", "EASD", "IPEP", "EA", "CPD", "CSM", "SES", "ESAD", "SIPEP", "CSD")
        assertEquals(emptyList<String>(), catalogo.filter { GrupoTipo.de(it) == null })
    }
}
