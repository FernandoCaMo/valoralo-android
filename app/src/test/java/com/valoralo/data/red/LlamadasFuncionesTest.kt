package com.valoralo.data.red

import com.valoralo.data.valoraciones.ErrorPublicacion
import com.valoralo.data.valoraciones.TipoErrorPublicacion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/** QA 1.0.5, findings 11 and 12: honest messages when the network fails during a call. */
class LlamadasFuncionesTest {

    private val incierto = "No sabemos si se ha guardado."

    private suspend fun llamar(
        conectado: Boolean = true,
        vuelveLaRed: Boolean = true,
        llamada: suspend () -> Any? = { "ok" },
        comprobar: (suspend () -> Comprobacion)? = null,
        conectadoTrasFallo: Boolean = conectado,
    ): Any? {
        var fallado = false
        return llamarConComprobacion(
            conectado = { if (fallado) conectadoTrasFallo else conectado },
            esperarRed = { vuelveLaRed },
            llamada = { try { llamada() } catch (e: Exception) { fallado = true; throw e } },
            comprobar = comprobar,
            mensajeIncierto = incierto,
        )
    }

    private suspend fun error(bloque: suspend () -> Any?): ErrorPublicacion {
        try {
            bloque()
        } catch (e: ErrorPublicacion) {
            return e
        }
        fail("debía fallar")
        error("inalcanzable")
    }

    @Test
    fun conRedDevuelveLaRespuesta() = runTest {
        assertEquals("ok", llamar())
    }

    @Test
    fun sinRedNoSeEnviaNada() = runTest {
        var llamado = false
        val e = error { llamar(conectado = false, llamada = { llamado = true }) }
        assertEquals(TipoErrorPublicacion.SIN_CONEXION, e.tipo)
        assertEquals(MENSAJE_SIN_CONEXION, e.message)
        assertFalse(llamado)
    }

    @Test
    fun corteYSeHabiaHecho_esUnExito() = runTest {
        val r = llamar(llamada = { throw IOException("corte") }, comprobar = { Comprobacion.Hecha("guardada") })
        assertEquals("guardada", r)
    }

    @Test
    fun corteYNoSeHabiaHecho_loDiceClaro() = runTest {
        val e = error { llamar(llamada = { throw IOException("corte") }, comprobar = { Comprobacion.NoHecha }) }
        assertEquals(TipoErrorPublicacion.CORTE_SIN_HACER, e.tipo)
        assertTrue(e.message!!.contains("no se ha completado"))
    }

    @Test
    fun corteSinPoderComprobar_diceQueNoLoSabemos() = runTest {
        // The network doesn't come back in time.
        var comprobado = false
        val e1 = error {
            llamar(llamada = { throw IOException("corte") }, vuelveLaRed = false, comprobar = { comprobado = true; Comprobacion.NoHecha })
        }
        assertEquals(TipoErrorPublicacion.INCIERTO, e1.tipo)
        assertEquals(incierto, e1.message)
        assertFalse(comprobado)
        // The check itself fails.
        val e2 = error { llamar(llamada = { throw IOException("corte") }, comprobar = { throw IOException("otra vez") }) }
        assertEquals(TipoErrorPublicacion.INCIERTO, e2.tipo)
    }

    @Test
    fun corteSinComprobacion_tambienEsIncierto() = runTest {
        assertEquals(TipoErrorPublicacion.INCIERTO, error { llamar(llamada = { throw IOException("corte") }) }.tipo)
    }

    @Test
    fun unErrorQueNoEsDeRed_noSeComprueba() = runTest {
        var comprobado = false
        val e = error {
            llamar(llamada = { throw IllegalStateException("fallo") }, comprobar = { comprobado = true; Comprobacion.Hecha() })
        }
        assertEquals(TipoErrorPublicacion.OTRO, e.tipo)
        assertFalse(comprobado)
    }

    @Test
    fun siSePierdeLaRedDuranteLaLlamada_cuentaComoCorte() = runTest {
        // A generic failure while the device has just lost the network.
        val r = llamar(llamada = { throw IllegalStateException("sin respuesta") }, conectadoTrasFallo = false, comprobar = { Comprobacion.Hecha("sí") })
        assertEquals("sí", r)
    }

    @Test
    fun esFalloDeRed() {
        assertTrue(esFalloDeRed(IOException("x"), conectado = true))
        assertTrue(esFalloDeRed(RuntimeException("envoltorio", IOException("x")), conectado = true))
        assertFalse(esFalloDeRed(IllegalStateException("x"), conectado = true))
        assertTrue(esFalloDeRed(IllegalStateException("x"), conectado = false))
        assertFalse(esFalloDeRed(ErrorPublicacion(TipoErrorPublicacion.OTRO), conectado = false))
    }
}
