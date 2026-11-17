package com.valoralo.domain.usecase

import com.valoralo.domain.model.NotaCentro
import com.valoralo.domain.model.NotaInstalaciones
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Valoracion
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LikesTest {

    @Test
    fun `el contador cambia al momento y no cuenta dos veces cuando llega el del servidor`() {
        // Loaded with 12 likes, not mine. I tap: 13 until the server count arrives.
        assertEquals(13, contadorVisible(base = 12, dadoAlCargar = false, dadoAhora = true))
        // Server count arrives (13, already with my like) and is loaded with my like: still 13.
        assertEquals(13, contadorVisible(base = 13, dadoAlCargar = true, dadoAhora = true))
        // I take it back.
        assertEquals(12, contadorVisible(base = 13, dadoAlCargar = true, dadoAhora = false))
        assertEquals(12, contadorVisible(base = 12, dadoAlCargar = false, dadoAhora = false))
        // Never negative.
        assertEquals(0, contadorVisible(base = 0, dadoAlCargar = true, dadoAhora = false))
    }

    @Test
    fun `estado del corazon con el ajuste local hasta que llega el contador del servidor`() {
        // No tap: what the server says.
        assertEquals(EstadoLike(dado = false, n = 12), estadoLike(nServidor = 12, dadoServidor = false, ajuste = null))
        // Tapped with 12: filled and 13 at once, while the server still says 12.
        val ajuste = AjusteLike(base = 12, dadoAntes = false, dadoAhora = true)
        assertEquals(EstadoLike(dado = true, n = 13), estadoLike(12, dadoServidor = false, ajuste = ajuste))
        assertEquals(EstadoLike(dado = true, n = 13), estadoLike(12, dadoServidor = true, ajuste = ajuste))
        // The server count arrives (13): the adjustment no longer applies.
        assertEquals(EstadoLike(dado = true, n = 13), estadoLike(13, dadoServidor = true, ajuste = ajuste))
        assertTrueAjusteCaduca(ajuste)
    }

    private fun assertTrueAjusteCaduca(ajuste: AjusteLike) {
        assertEquals(true, ajuste.caducado(nServidor = 13))
        assertEquals(false, ajuste.caducado(nServidor = 12))
    }

    @Test
    fun `descripcion para TalkBack`() {
        assertEquals("Dar like, 12 likes", descripcionLike(12, dado = false, propia = false))
        assertEquals("Quitar like, 1 like", descripcionLike(1, dado = true, propia = false))
        assertEquals("Dar like, sin likes", descripcionLike(0, dado = false, propia = false))
        assertEquals("12 likes", descripcionLike(12, dado = false, propia = true))
    }

    private fun resena(uid: String, dia: Int, likes: Int) = Valoracion(
        uid = uid, alias = uid, rolAutor = Rol.OTRO, verificado = false, fecha = LocalDate.of(2026, 11, dia),
        centro = NotaCentro(4), instalaciones = NotaInstalaciones(4), texto = "", nLikes = likes,
    )

    @Test
    fun `ordenar por mas recientes o mas populares`() {
        val a = resena("a", 1, 9)
        val b = resena("b", 10, 2)
        val c = resena("c", 5, 9)
        assertEquals(listOf(b, c, a), listOf(a, b, c).ordenadas(OrdenResenas.RECIENTES))
        // Most likes first; tie → the most recent.
        assertEquals(listOf(c, a, b), listOf(a, b, c).ordenadas(OrdenResenas.POPULARES))
    }

    @Test
    fun `contador por hora como lo exigen las reglas`() {
        val hora = 3_600_000L
        assertEquals(PasoLimite.Empezar, siguientePasoLimite(null, ahoraMs = 10 * hora, max = 60))
        assertEquals(PasoLimite.Sumar, siguientePasoLimite(LimiteLikes(inicioMs = 10 * hora, n = 5), ahoraMs = 10 * hora + 1000, max = 60))
        assertEquals(PasoLimite.Lleno, siguientePasoLimite(LimiteLikes(inicioMs = 10 * hora, n = 60), ahoraMs = 10 * hora + 1000, max = 60))
        // An hour later it starts again, even if it was full.
        assertEquals(PasoLimite.Empezar, siguientePasoLimite(LimiteLikes(inicioMs = 10 * hora, n = 60), ahoraMs = 11 * hora, max = 60))
    }
}
