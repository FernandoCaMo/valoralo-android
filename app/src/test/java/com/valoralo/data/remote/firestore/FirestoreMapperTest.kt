package com.valoralo.data.remote.firestore

import com.valoralo.domain.model.Ensenanza
import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.Fuente
import com.valoralo.domain.model.NotaDepartamento
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.StatsDepartamento
import com.valoralo.domain.model.StatsGrupo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class FirestoreMapperTest {

    // As the import script writes it (numbers from Firestore arrive as Long or Double).
    private val documentoCentro: Map<String, Any?> = mapOf(
        "codigo" to "21000693",
        "nombre" to "IES Delgado Hernández",
        "tipo" to "IES",
        "titularidad" to "PUBLICA",
        "direccion" to "C/ Dalí, 1",
        "cp" to "21710",
        "localidad" to "Bollullos Par del Condado",
        "municipio" to "Bollullos Par del Condado",
        "codMunicipio" to "21013",
        "provincia" to "Huelva",
        "comunidad" to "Andalucía",
        "telefono" to "959439921",
        "email" to "21000693.edu@juntadeandalucia.es",
        "ensenanzas" to listOf(
            mapOf("nivel" to "ESO", "nombre" to "Educación Secundaria Obligatoria"),
            mapOf("nivel" to "Bachillerato", "nombre" to "Bachillerato"),
        ),
        "departamentos" to emptyList<String>(),
        "fuente" to mapOf("dataset" to "directorio-de-centros-docentes-de-andalucia", "curso" to "2024/2025"),
        "stats" to mapOf(
            "total" to mapOf(
                "n" to 2L, "global" to 3.5, "centro" to 4L, "instalaciones" to 3.0, "profesorado" to null,
                "porDepartamento" to mapOf("Matemáticas" to mapOf("n" to 1L, "media" to 4L)),
            ),
            "docente" to mapOf("n" to 0L, "global" to null, "porDepartamento" to emptyMap<String, Any>()),
        ),
    )

    @Test
    fun `convierte un centro importado`() {
        val centro = documentoCentro.aCentro("21000693")
        assertEquals("21000693", centro.codigo)
        assertEquals("IES Delgado Hernández", centro.nombre)
        assertEquals("IES", centro.tipo)
        assertEquals("Bollullos Par del Condado", centro.localidad)
        assertEquals("21013", centro.codMunicipio)
        assertEquals("Huelva", centro.provincia)
        assertEquals("959439921", centro.telefono)
        assertNull(centro.web)
        assertEquals(
            listOf(Ensenanza("ESO", "Educación Secundaria Obligatoria"), Ensenanza("Bachillerato", "Bachillerato")),
            centro.ensenanzas,
        )
        assertEquals(Fuente("directorio-de-centros-docentes-de-andalucia", "2024/2025"), centro.fuente)
        assertEquals(
            StatsGrupo(
                n = 2, global = 3.5, centro = 4.0, instalaciones = 3.0, profesorado = null,
                porDepartamento = mapOf("Matemáticas" to StatsDepartamento(1, 4.0)),
            ),
            centro.stats.total,
        )
        assertEquals(StatsGrupo(), centro.stats.docente)
        assertEquals(StatsGrupo(), centro.stats.otro) // missing group → empty
    }

    @Test
    fun `el id del documento manda y los campos ausentes no rompen`() {
        val centro = mapOf<String, Any?>("nombre" to "CEIP X").aCentro("21000001")
        assertEquals("21000001", centro.codigo)
        assertEquals("", centro.provincia)
        assertEquals(emptyList<Ensenanza>(), centro.ensenanzas)
        assertNull(centro.fuente)
        assertEquals(StatsGrupo(), centro.stats.total)
    }

    @Test
    fun `convierte un documento de catalogo resumido`() {
        val documento = mapOf<String, Any?>(
            "provincia" to "Huelva", "parte" to 1L, "partes" to 1L,
            "centros" to listOf(
                mapOf(
                    "codigo" to "21000048", "nombre" to "CEIP Profesor Tierno Galván", "tipo" to "CEIP",
                    "localidad" to "Corrales", "municipio" to "Aljaraque",
                    "niveles" to listOf("Infantil", "Primaria"), "nota" to 4.25, "resenas" to 4L,
                ),
                mapOf("codigo" to "21000001", "nombre" to "IES Sin Notas", "tipo" to "IES",
                    "localidad" to "Huelva", "municipio" to "Huelva", "niveles" to emptyList<String>(),
                    "nota" to null, "resenas" to 0L),
                mapOf("nombre" to "sin código, se descarta"),
            ),
        )
        val centros = documento.aCentrosDeCatalogo()
        assertEquals(listOf("21000048", "21000001"), centros.map { it.codigo })
        val ceip = centros.first()
        assertEquals("CEIP Profesor Tierno Galván", ceip.nombre)
        assertEquals("CEIP", ceip.tipo)
        assertEquals("Corrales", ceip.localidad)
        assertEquals("Aljaraque", ceip.municipio)
        assertEquals("Huelva", ceip.provincia)
        assertEquals(listOf(Ensenanza("Infantil", "Infantil"), Ensenanza("Primaria", "Primaria")), ceip.ensenanzas)
        assertEquals(StatsGrupo(n = 4, global = 4.25), ceip.stats.total)
        assertEquals(StatsGrupo(), centros[1].stats.total)
    }

    @Test
    fun `convierte una valoracion`() {
        val fecha = Date.from(LocalDate.of(2026, 3, 9).atStartOfDay(ZoneId.of("Europe/Madrid")).plusHours(10).toInstant())
        val valoracion = mapOf<String, Any?>(
            "uid" to "u1", "alias" to "Ana G.", "rolAutor" to "DOCENTE", "verificado" to true,
            "fecha" to fecha, "editada" to false,
            "centro" to mapOf("nota" to 5L, "gestion" to 4L),
            "instalaciones" to mapOf("nota" to 3L),
            "profesorado" to listOf(mapOf("departamento" to "Matemáticas", "nota" to 4L)),
            "texto" to "Buen ambiente.", "estado" to "PUBLICADA",
        ).aValoracion("u1")!!
        assertEquals(Rol.DOCENTE, valoracion.rolAutor)
        assertEquals(LocalDate.of(2026, 3, 9), valoracion.fecha)
        assertEquals(5, valoracion.centro.nota)
        assertEquals(4, valoracion.centro.gestion)
        assertEquals(3, valoracion.instalaciones.nota)
        assertEquals(listOf(NotaDepartamento("Matemáticas", 4)), valoracion.profesorado)
        assertEquals(EstadoValoracion.PUBLICADA, valoracion.estado)
    }

    @Test
    fun `una valoracion mal formada se descarta`() {
        assertNull(mapOf<String, Any?>("rolAutor" to "PIRATA").aValoracion("x"))
        assertNull(mapOf<String, Any?>("rolAutor" to "OTRO").aValoracion("x")) // sin notas
    }
}
