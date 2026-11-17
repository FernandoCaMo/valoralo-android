package com.valoralo.domain.usecase

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Ensenanza
import com.valoralo.domain.model.Stats
import com.valoralo.domain.model.StatsDepartamento
import com.valoralo.domain.model.StatsGrupo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparacionTest {

    private fun centro(
        codigo: String,
        total: StatsGrupo = StatsGrupo(),
        docente: StatsGrupo = StatsGrupo(),
        niveles: List<String> = emptyList(),
        departamentos: List<String> = emptyList(),
    ) = Centro(
        codigo = codigo, nombre = "Centro $codigo", tipo = "IES", direccion = "", cp = "", municipio = "", provincia = "",
        ensenanzas = niveles.map { Ensenanza(it, it) }, departamentos = departamentos,
        stats = Stats(total = total, docente = docente),
    )

    private fun grupo(n: Int, global: Double?, nProf: Int = n, prof: Double? = global, dep: Map<String, StatsDepartamento> = emptyMap()) =
        StatsGrupo(n = n, nProfesorado = nProf, global = global, centro = global, instalaciones = global, profesorado = prof, porDepartamento = dep)

    @Test
    fun `filas de notas con el numero de resenas de cada celda`() {
        val c = comparar(listOf(centro("1", grupo(10, 4.2, nProf = 6)), centro("2", grupo(3, 3.0))), FiltroRol.TODOS)
        assertEquals(listOf("Nota global", "Centro en general", "Aulas e instalaciones", "Profesorado"), c.notas.map { it.etiqueta })
        assertEquals(Celda(4.2, 10, mejor = true), c.notas[0].celdas[0])
        assertEquals(Celda(3.0, 3), c.notas[0].celdas[1])
        // profesorado uses nProfesorado
        assertEquals(6, c.notas[3].celdas[0].n)
    }

    @Test
    fun `solo se destaca si todos los centros de la fila tienen al menos 3 resenas`() {
        val pocos = comparar(listOf(centro("1", grupo(1, 5.0)), centro("2", grupo(40, 4.1))), FiltroRol.TODOS)
        assertTrue(pocos.notas.all { fila -> fila.celdas.none { it.mejor } })

        val sinNotas = comparar(listOf(centro("1", grupo(5, 4.0)), centro("2", StatsGrupo())), FiltroRol.TODOS)
        assertTrue(sinNotas.notas[0].celdas.none { it.mejor })
    }

    @Test
    fun `los empates se destacan todos`() {
        val c = comparar(listOf(centro("1", grupo(5, 4.0)), centro("2", grupo(7, 4.0)), centro("3", grupo(9, 3.0))), FiltroRol.TODOS)
        assertEquals(listOf(true, true, false), c.notas[0].celdas.map { it.mejor })
    }

    @Test
    fun `el filtro de rol cambia todas las filas`() {
        val c = comparar(
            listOf(centro("1", total = grupo(10, 4.0), docente = grupo(4, 2.0)), centro("2", total = grupo(10, 3.0), docente = grupo(3, 4.5))),
            FiltroRol.DOCENTES,
        )
        assertEquals(listOf(2.0, 4.5), c.notas[0].celdas.map { it.nota })
        assertEquals(listOf(false, true), c.notas[0].celdas.map { it.mejor })
    }

    @Test
    fun `departamentos unidos con media, sin valoraciones o no disponible`() {
        val c = comparar(
            listOf(
                centro("1", grupo(6, 4.0, dep = mapOf("Matemáticas" to StatsDepartamento(4, 4.5))), departamentos = listOf("Matemáticas", "Inglés")),
                centro("2", grupo(6, 4.0, dep = mapOf("Matemáticas" to StatsDepartamento(3, 3.0), "Robótica" to StatsDepartamento(2, 5.0))),
                    departamentos = listOf("Matemáticas")),
            ),
            FiltroRol.TODOS,
        )
        assertEquals(listOf("Inglés", "Matemáticas", "Robótica"), c.departamentos.map { it.etiqueta })
        val ingles = c.departamentos[0].celdas
        assertEquals(Celda(null, 0), ingles[0])                     // offered, no ratings
        assertFalse(ingles[1].disponible)                           // not in centro 2
        val mates = c.departamentos[1].celdas
        assertEquals(listOf(true, false), mates.map { it.mejor })   // both ≥ 3 → 4,5 wins
        val robotica = c.departamentos[2].celdas
        assertTrue(robotica[1].disponible)                          // rated as "Otro departamento"
        assertTrue(robotica.none { it.mejor })
    }

    @Test
    fun `ensenanzas por nivel en el orden habitual`() {
        val c = comparar(listOf(centro("1", niveles = listOf("Bachillerato", "ESO")), centro("2", niveles = listOf("ESO", "FP Grado Medio"))), FiltroRol.TODOS)
        assertEquals(listOf("ESO", "Bachillerato", "FP Grado Medio"), c.ensenanzas.map { it.nivel })
        assertEquals(listOf(true, true), c.ensenanzas[0].tiene)
        assertEquals(listOf(true, false), c.ensenanzas[1].tiene)
        assertEquals(listOf(false, true), c.ensenanzas[2].tiene)
    }
}
