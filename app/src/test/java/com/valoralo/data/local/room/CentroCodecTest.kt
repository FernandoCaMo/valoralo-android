package com.valoralo.data.local.room

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Ensenanza
import com.valoralo.domain.model.Fuente
import com.valoralo.domain.model.Stats
import com.valoralo.domain.model.StatsDepartamento
import com.valoralo.domain.model.StatsGrupo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CentroCodecTest {

    private val centro = Centro(
        codigo = "21000693", nombre = "IES Delgado Hernández", tipo = "IES", direccion = "C/ Dalí, 1",
        cp = "21710", localidad = "Bollullos Par del Condado", municipio = "Bollullos Par del Condado",
        codMunicipio = "21013", provincia = "Huelva", telefono = "959439921", email = null,
        ensenanzas = listOf(Ensenanza("ESO", "Educación Secundaria Obligatoria")),
        departamentos = listOf("Matemáticas", "Informática y Comunicaciones"),
        departamentosFuente = "plantilla",
        stats = Stats(total = StatsGrupo(n = 3, global = 10.0 / 3, porDepartamento = mapOf("Matemáticas" to StatsDepartamento(2, 4.5)))),
        fuente = Fuente("directorio-de-centros-docentes-de-andalucia", "2024/2025"),
    )

    @Test
    fun `ida y vuelta sin perder datos ni decimales`() {
        assertEquals(centro, CentroCodec.decodificar(CentroCodec.codificar(centro)))
    }

    @Test
    fun `un JSON corrupto devuelve null`() {
        assertNull(CentroCodec.decodificar("{no es json"))
    }

    @Test
    fun `ignora campos desconocidos de versiones futuras`() {
        val json = CentroCodec.codificar(centro).replaceFirst("{", "{\"campoNuevo\":1,")
        assertEquals(centro, CentroCodec.decodificar(json))
    }
}
