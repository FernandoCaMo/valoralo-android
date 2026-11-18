package com.valoralo.data.pendientes

import com.valoralo.data.likes.ClaveResena
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AccionesPendientesTest {
    private val sesion = MutableStateFlow(false)
    private val favoritos = mutableSetOf<String>()
    private val likes = mutableSetOf<ClaveResena>()
    private var ahora = Instant.parse("2026-11-18T10:00:00Z")
    private val mensajes = mutableListOf<String>()

    private fun TestScope.crear(): AccionesPendientes {
        val p = AccionesPendientes(
            sesionLista = sesion,
            esFavorito = { it in favoritos },
            guardarFavorito = { favoritos += it },
            tieneLike = { it in likes },
            darLike = { likes += it },
            scope = backgroundScope,
            reloj = { ahora },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { p.mensajes.collect { mensajes += it } }
        return p
    }

    @Test
    fun `el favorito pedido sin cuenta se guarda al iniciar sesion`() = runTest(UnconfinedTestDispatcher()) {
        val p = crear()
        p.guardar(AccionPendiente.Favorito("41000661"))
        sesion.value = true
        assertEquals(setOf("41000661"), favoritos)
        assertEquals(listOf("Guardado en favoritos."), mensajes)
    }

    @Test
    fun `el like pedido sin cuenta se da al iniciar sesion y solo una vez`() = runTest(UnconfinedTestDispatcher()) {
        val p = crear()
        val clave = ClaveResena("41000661", "autor")
        p.guardar(AccionPendiente.Like(clave))
        sesion.value = true
        sesion.value = false
        sesion.value = true
        assertEquals(setOf(clave), likes)
        assertEquals(1, mensajes.size)
    }

    @Test
    fun `no deshace lo que ya estaba hecho`() = runTest(UnconfinedTestDispatcher()) {
        favoritos += "41000661"
        val p = crear()
        p.guardar(AccionPendiente.Favorito("41000661"))
        sesion.value = true
        assertEquals(setOf("41000661"), favoritos)
        assertEquals(emptyList<String>(), mensajes)
    }

    @Test
    fun `descartada o caducada no se hace`() = runTest(UnconfinedTestDispatcher()) {
        val p = crear()
        p.guardar(AccionPendiente.Favorito("A"))
        p.descartar()
        sesion.value = true
        sesion.value = false
        p.guardar(AccionPendiente.Favorito("B"))
        ahora = ahora.plus(AccionesPendientes.VIGENCIA).plusSeconds(1)
        sesion.value = true
        assertEquals(emptySet<String>(), favoritos)
    }
}
