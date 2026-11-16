package com.valoralo.data.repository.fake

import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.NotaCentro
import com.valoralo.domain.model.NotaDepartamento
import com.valoralo.domain.model.NotaInstalaciones
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.StatsDepartamento
import com.valoralo.domain.model.StatsGrupo
import com.valoralo.domain.model.Valoracion
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsDePruebaTest {

    private fun v(
        rol: Rol,
        centro: Int,
        instalaciones: Int,
        deptos: Map<String, Int> = emptyMap(),
        estado: EstadoValoracion = EstadoValoracion.PUBLICADA,
    ) = Valoracion(
        uid = "u", alias = "a", rolAutor = rol, verificado = rol == Rol.DOCENTE,
        fecha = LocalDate.of(2025, 1, 1),
        centro = NotaCentro(centro), instalaciones = NotaInstalaciones(instalaciones),
        profesorado = deptos.map { NotaDepartamento(it.key, it.value) },
        texto = "", estado = estado,
    )

    @Test
    fun `sin valoraciones todo vacio`() {
        val stats = calcularStatsDePrueba(emptyList())
        assertEquals(StatsGrupo(), stats.total)
        assertEquals(emptyMap<String, StatsDepartamento>(), stats.total.porDepartamento)
    }

    @Test
    fun `agrega por rol ignorando las no publicadas`() {
        val stats = calcularStatsDePrueba(
            listOf(
                // profesorado (5+3)/2 = 4 → global (5+3+4)/3 = 4
                v(Rol.DOCENTE, 5, 3, mapOf("Matemáticas" to 5, "Lengua" to 3)),
                // no rates departments → no profesorado; global (3+2)/2 = 2.5
                v(Rol.ESTUDIANTE, 3, 2),
                v(Rol.OTRO, 1, 1, mapOf("Matemáticas" to 1), EstadoValoracion.OCULTA),
            ),
        )
        assertEquals(
            StatsGrupo(
                n = 1, global = 4.0, centro = 5.0, instalaciones = 3.0, profesorado = 4.0,
                porDepartamento = mapOf("Matemáticas" to StatsDepartamento(1, 5.0), "Lengua" to StatsDepartamento(1, 3.0)),
            ),
            stats.docente,
        )
        assertEquals(StatsGrupo(n = 1, global = 2.5, centro = 3.0, instalaciones = 2.0, profesorado = null), stats.estudiante)
        assertEquals(StatsGrupo(), stats.otro)
        // Profesorado only averages reviews that have it (just the teacher's).
        assertEquals(
            StatsGrupo(
                n = 2, global = 3.25, centro = 4.0, instalaciones = 2.5, profesorado = 4.0,
                porDepartamento = mapOf("Matemáticas" to StatsDepartamento(1, 5.0), "Lengua" to StatsDepartamento(1, 3.0)),
            ),
            stats.total,
        )
    }

    @Test
    fun `el desglose por departamento es distinto en cada rol`() {
        val stats = calcularStatsDePrueba(
            listOf(
                v(Rol.DOCENTE, 4, 4, mapOf("Inglés" to 5)),
                v(Rol.ESTUDIANTE, 4, 4, mapOf("Inglés" to 2)),
                v(Rol.ESTUDIANTE, 4, 4, mapOf("Inglés" to 3, "Dibujo" to 4)),
            ),
        )
        assertEquals(mapOf("Inglés" to StatsDepartamento(1, 5.0)), stats.docente.porDepartamento)
        assertEquals(
            mapOf("Inglés" to StatsDepartamento(2, 2.5), "Dibujo" to StatsDepartamento(1, 4.0)),
            stats.estudiante.porDepartamento,
        )
        assertEquals(
            mapOf("Inglés" to StatsDepartamento(3, 10.0 / 3), "Dibujo" to StatsDepartamento(1, 4.0)),
            stats.total.porDepartamento,
        )
    }

    @Test
    fun `las medias se guardan sin redondear`() {
        val stats = calcularStatsDePrueba(
            listOf(v(Rol.OTRO, 5, 4), v(Rol.OTRO, 4, 4), v(Rol.OTRO, 4, 4)),
        )
        assertEquals(13.0 / 3, stats.otro.centro!!, 0.0)
    }
}
