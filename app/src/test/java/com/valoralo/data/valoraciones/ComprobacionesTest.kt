package com.valoralo.data.valoraciones

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** After a lost connection: is the saved review what was sent? (QA 1.0.5, finding 12) */
class ComprobacionesTest {

    private val enviado: Map<String, Any?> = mapOf(
        "codigo" to "41999901",
        "centro" to mapOf("nota" to 4, "gestion" to 5),
        "instalaciones" to mapOf("nota" to 3),
        "profesorado" to listOf(mapOf("departamento" to "Matemáticas", "nota" to 5), mapOf("departamento" to "Robótica", "nota" to 4, "otro" to true)),
        "texto" to "Primera línea.\n\n\n\n\nSegunda.   ",
    )

    // As Firestore returns it: Long numbers, other order, the server's tidied text, extra fields.
    private val guardado: Map<String, Any?> = mapOf(
        "centro" to mapOf("gestion" to 5L, "nota" to 4L),
        "instalaciones" to mapOf("nota" to 3L),
        "profesorado" to listOf(mapOf("departamento" to "Robótica", "nota" to 4L, "otro" to true), mapOf("departamento" to "Matemáticas", "nota" to 5L)),
        "texto" to "Primera línea.\n\nSegunda.",
        "estado" to "PUBLICADA", "alias" to "Familia de 5º B", "nLikes" to 3L,
    )

    @Test
    fun coincideAunqueFirestoreDevuelvaLongYOtroOrden() {
        assertTrue(coincideConGuardada(enviado, guardado))
    }

    @Test
    fun noCoincideSiCambiaAlgo() {
        assertFalse(coincideConGuardada(enviado, guardado + ("texto" to "Otro texto.")))
        assertFalse(coincideConGuardada(enviado, guardado + ("centro" to mapOf("nota" to 3L, "gestion" to 5L))))
        assertFalse(coincideConGuardada(enviado, guardado + ("profesorado" to emptyList<Any>())))
        assertFalse(coincideConGuardada(enviado, null))
    }

    @Test
    fun textoComoElServidor() {
        assertEquals("Uno.\n\nDos.", textoComoServidor("  Uno.\r\n\r\n\r\n\r\nDos.  "))
        assertEquals("Lista:\n- a\n- b", textoComoServidor("Lista:   \n- a\n- b"))
        assertEquals("", textoComoServidor(null))
    }
}
