package com.valoralo.domain.usecase

import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.NotaCentro
import com.valoralo.domain.model.NotaInstalaciones
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Stats
import com.valoralo.domain.model.StatsGrupo
import com.valoralo.domain.model.Valoracion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FiltroRolTest {

    private val stats = Stats(
        total = StatsGrupo(n = 10, global = 4.0),
        docente = StatsGrupo(n = 3, global = 4.2),
        estudiante = StatsGrupo(n = 5, global = 3.6),
        otro = StatsGrupo(n = 2, global = 4.5),
    )

    @Test
    fun `cada filtro elige su grupo de stats`() {
        assertEquals(stats.total, stats.para(FiltroRol.TODOS))
        assertEquals(stats.docente, stats.para(FiltroRol.DOCENTES))
        assertEquals(stats.estudiante, stats.para(FiltroRol.ESTUDIANTES))
        assertEquals(stats.otro, stats.para(FiltroRol.OTROS))
    }

    private fun valoracion(
        uid: String,
        rol: Rol,
        fecha: LocalDate,
        estado: EstadoValoracion = EstadoValoracion.PUBLICADA,
        verificado: Boolean = false,
    ) = Valoracion(
        uid = uid,
        alias = uid,
        rolAutor = rol,
        verificado = verificado,
        fecha = fecha,
        centro = NotaCentro(4),
        instalaciones = NotaInstalaciones(4),
        texto = "",
        estado = estado,
    )

    private val docenteAntigua = valoracion("d1", Rol.DOCENTE, LocalDate.of(2025, 1, 10), verificado = true)
    private val estudiante = valoracion("e1", Rol.ESTUDIANTE, LocalDate.of(2025, 6, 1))
    private val docenteReciente = valoracion("d2", Rol.DOCENTE, LocalDate.of(2025, 9, 20), verificado = true)
    private val otroOculta = valoracion("o1", Rol.OTRO, LocalDate.of(2025, 10, 1), EstadoValoracion.OCULTA)
    private val otroRevision = valoracion("o2", Rol.OTRO, LocalDate.of(2025, 10, 2), EstadoValoracion.EN_REVISION)
    private val otro = valoracion("o3", Rol.OTRO, LocalDate.of(2025, 3, 3))
    private val todas = listOf(docenteAntigua, estudiante, docenteReciente, otroOculta, otroRevision, otro)

    @Test
    fun `todos muestra solo las publicadas, de mas reciente a mas antigua`() {
        assertEquals(
            listOf(docenteReciente, estudiante, otro, docenteAntigua),
            todas.filtrarPor(FiltroRol.TODOS),
        )
    }

    @Test
    fun `filtra por rol del autor`() {
        assertEquals(listOf(docenteReciente, docenteAntigua), todas.filtrarPor(FiltroRol.DOCENTES))
        assertEquals(listOf(estudiante), todas.filtrarPor(FiltroRol.ESTUDIANTES))
        assertEquals(listOf(otro), todas.filtrarPor(FiltroRol.OTROS))
    }

    @Test
    fun `insignia solo para docentes verificados`() {
        assertTrue(docenteReciente.esDocenteVerificado)
        assertFalse(docenteReciente.copy(verificado = false).esDocenteVerificado)
        assertFalse(estudiante.copy(verificado = true).esDocenteVerificado)
    }
}
