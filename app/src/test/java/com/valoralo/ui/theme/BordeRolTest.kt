package com.valoralo.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.valoralo.domain.model.Rol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class BordeRolTest {

    /** WCAG 2.x relative luminance and contrast ratio, from the ARGB value. */
    private fun luminancia(c: Color): Double {
        val argb = c.toArgb()
        fun canal(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * canal(argb shr 16 and 0xFF) + 0.7152 * canal(argb shr 8 and 0xFF) + 0.0722 * canal(argb and 0xFF)
    }

    private fun contraste(a: Color, b: Color): Double {
        val la = luminancia(a)
        val lb = luminancia(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    // Card surfaces: light/dark background, and the tinted elevated card of the carousel.
    private val fondosClaros = listOf(Neutro99, Color(0xFFF4EFF4))
    private val fondosOscuros = listOf(Neutro10, Color(0xFF2A2C30))

    @Test
    fun docenteYEstudianteSuperanTresAUnoEnClaroYOscuro() {
        for (rol in listOf(Rol.DOCENTE, Rol.ESTUDIANTE)) {
            fondosClaros.forEach { f ->
                val c = contraste(colorBordeRol(rol, oscuro = false), f)
                assertTrue("$rol claro: $c", c >= 3.0)
            }
            fondosOscuros.forEach { f ->
                val c = contraste(colorBordeRol(rol, oscuro = true), f)
                assertTrue("$rol oscuro: $c", c >= 3.0)
            }
        }
    }

    @Test
    fun comoTextoDelCarruselSuperanCuatroYMedioAUno() {
        // The carousel role label uses the border colour for Docente and Estudiante.
        for (rol in listOf(Rol.DOCENTE, Rol.ESTUDIANTE)) {
            fondosClaros.forEach { f -> assertTrue("$rol claro", contraste(colorBordeRol(rol, false), f) >= 4.5) }
            fondosOscuros.forEach { f -> assertTrue("$rol oscuro", contraste(colorBordeRol(rol, true), f) >= 4.5) }
        }
    }

    @Test
    fun cadaRolTieneSuColor() {
        for (oscuro in listOf(false, true)) {
            val colores = Rol.entries.map { colorBordeRol(it, oscuro) }
            assertEquals(colores.size, colores.toSet().size)
        }
        assertNotEquals(colorBordeRol(Rol.ESTUDIANTE, false), Estrella)
    }

    @Test
    fun otroConservaElBordeSuaveDeSiempre() {
        assertEquals(NeutroVar80, colorBordeRol(Rol.OTRO, oscuro = false))
        assertEquals(NeutroVar30, colorBordeRol(Rol.OTRO, oscuro = true))
        assertEquals(1.dp, grosorBordeRol(Rol.OTRO))
        assertEquals(2.dp, grosorBordeRol(Rol.DOCENTE))
        assertEquals(2.dp, grosorBordeRol(Rol.ESTUDIANTE))
    }
}
