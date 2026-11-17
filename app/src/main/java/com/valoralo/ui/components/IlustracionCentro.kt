package com.valoralo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.valoralo.domain.model.GrupoCentro
import com.valoralo.ui.theme.Estrella

/** Theme colours used by the drawings (they follow light and dark mode). */
private data class Paleta(
    val cielo: Color,
    val suelo: Color,
    val fachada: Color,
    val linea: Color,
    val ventana: Color,
    val puerta: Color,
    val acento: Color,
    val acento2: Color,
)

/**
 * Own illustration of a centro by its type (8 groups + generic). Drawn on a 100×100 grid,
 * centred and scaled to the available size. Decorative: hidden from TalkBack.
 * Never photos or logos from the internet (docs/decisiones.md).
 */
@Composable
fun IlustracionCentro(tipo: String?, modifier: Modifier = Modifier) {
    val grupo = remember(tipo) { GrupoCentro.de(tipo) }
    val c = MaterialTheme.colorScheme
    val paleta = Paleta(
        cielo = c.primaryContainer,
        suelo = c.primary.copy(alpha = 0.18f),
        fachada = c.surface,
        linea = c.primary,
        ventana = c.secondaryContainer,
        puerta = c.primary.copy(alpha = 0.75f),
        acento = c.tertiary,
        acento2 = Estrella,
    )
    Canvas(modifier.clearAndSetSemantics { }) {
        drawRect(paleta.cielo)
        val lado = size.minDimension
        val u = lado / 100f
        translate((size.width - lado) / 2f, (size.height - lado) / 2f) {
            Lienzo(this, u, paleta).dibujar(grupo)
        }
        // The ground runs the full width even when the canvas is not square.
        drawRect(paleta.suelo, topLeft = Offset(0f, (size.height - lado) / 2f + 84 * u), size = Size(size.width, size.height))
    }
}

private class Lienzo(val d: DrawScope, val u: Float, val p: Paleta) {
    fun o(x: Number, y: Number) = Offset(x.toFloat() * u, y.toFloat() * u)
    fun s(w: Number, h: Number) = Size(w.toFloat() * u, h.toFloat() * u)
    private val trazo get() = Stroke(width = 2.2f * u)

    fun rect(color: Color, x: Number, y: Number, w: Number, h: Number, radio: Float = 0f) =
        d.drawRoundRect(color, o(x, y), s(w, h), CornerRadius(radio * u))

    fun borde(x: Number, y: Number, w: Number, h: Number, radio: Float = 0f) =
        d.drawRoundRect(p.linea, o(x, y), s(w, h), CornerRadius(radio * u), style = trazo)

    fun poligono(color: Color, vararg puntos: Pair<Number, Number>, relleno: Boolean = true) {
        val path = Path().apply {
            puntos.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x.toFloat() * u, y.toFloat() * u) else lineTo(x.toFloat() * u, y.toFloat() * u) }
            close()
        }
        if (relleno) d.drawPath(path, color) else d.drawPath(path, color, style = trazo)
    }

    fun circulo(color: Color, x: Number, y: Number, r: Number, contorno: Boolean = false) =
        if (contorno) d.drawCircle(color, r.toFloat() * u, o(x, y), style = trazo) else d.drawCircle(color, r.toFloat() * u, o(x, y))

    /** Facade with a grid of windows and a central door. */
    fun edificio(x: Int, y: Int, w: Int, h: Int, filas: Int, columnas: Int) {
        rect(p.fachada, x, y, w, h)
        borde(x, y, w, h)
        val puertaW = 10
        val puertaH = 14
        val margen = 5f
        val altoUtil = h - puertaH - margen * 2
        val ventanaH = (altoUtil / filas) * 0.6f
        val ventanaW = ((w - margen * 2) / columnas) * 0.55f
        for (fila in 0 until filas) for (col in 0 until columnas) {
            val cx = x + margen + (w - margen * 2) * (col + 0.5f) / columnas
            val cy = y + margen + altoUtil * (fila + 0.5f) / filas
            rect(p.ventana, cx - ventanaW / 2, cy - ventanaH / 2, ventanaW, ventanaH, 1f)
            borde(cx - ventanaW / 2, cy - ventanaH / 2, ventanaW, ventanaH, 1f)
        }
        rect(p.puerta, x + w / 2f - puertaW / 2f, y + h - puertaH, puertaW, puertaH, 1.5f)
    }

    fun dibujar(grupo: GrupoCentro) = when (grupo) {
        GrupoCentro.INFANTIL_PRIMARIA -> infantil()
        GrupoCentro.SECUNDARIA -> secundaria()
        GrupoCentro.FP -> fp()
        GrupoCentro.ADULTOS -> adultos()
        GrupoCentro.IDIOMAS -> idiomas()
        GrupoCentro.MUSICA_DANZA -> musica()
        GrupoCentro.ARTE -> arte()
        GrupoCentro.EDUCACION_ESPECIAL -> especial()
        GrupoCentro.OTRO -> generico()
    }

    /** Low school with a pitched roof, the sun and a ball. */
    private fun infantil() {
        circulo(p.acento2, 18, 20, 8)
        poligono(p.linea, 14 to 52, 50 to 28, 86 to 52)
        edificio(20, 52, 60, 32, filas = 1, columnas = 3)
        circulo(p.acento, 88, 80, 4)
    }

    /** Tall building with a flat roof, a clock and a flag. */
    private fun secundaria() {
        rect(p.linea, 22, 30, 56, 4)
        edificio(24, 34, 52, 50, filas = 2, columnas = 3)
        circulo(p.fachada, 50, 24, 6)
        circulo(p.linea, 50, 24, 6, contorno = true)
        d.drawLine(p.linea, o(50, 24), o(50, 20), strokeWidth = 1.6f * u)
        d.drawLine(p.linea, o(50, 24), o(53, 24), strokeWidth = 1.6f * u)
        d.drawLine(p.linea, o(84, 84), o(84, 18), strokeWidth = 1.8f * u)
        poligono(p.acento, 84 to 18, 96 to 22, 84 to 26)
    }

    /** Workshop with a saw-tooth roof and a gear. */
    private fun fp() {
        poligono(p.linea, 12 to 50, 12 to 40, 26 to 50, 26 to 40, 40 to 50, 40 to 40, 54 to 50, 54 to 40, 68 to 50)
        edificio(12, 50, 56, 34, filas = 1, columnas = 3)
        val cx = 82f
        val cy = 34f
        for (i in 0 until 8) {
            d.rotate(i * 45f, o(cx, cy)) { rect(p.acento, cx - 2.5f, cy - 13f, 5, 6, 1f) }
        }
        circulo(p.acento, cx, cy, 9)
        circulo(p.cielo, cx, cy, 3.5f)
    }

    /** Building and an open book (adult education). */
    private fun adultos() {
        edificio(14, 42, 52, 42, filas = 2, columnas = 3)
        rect(p.linea, 12, 38, 56, 4)
        poligono(p.fachada, 80 to 22, 66 to 18, 66 to 38, 80 to 42)
        poligono(p.fachada, 80 to 22, 94 to 18, 94 to 38, 80 to 42)
        poligono(p.linea, 80 to 22, 66 to 18, 66 to 38, 80 to 42, relleno = false)
        poligono(p.linea, 80 to 22, 94 to 18, 94 to 38, 80 to 42, relleno = false)
        d.drawLine(p.acento, o(70, 26), o(77, 28), strokeWidth = 1.4f * u)
        d.drawLine(p.acento, o(70, 31), o(77, 33), strokeWidth = 1.4f * u)
        d.drawLine(p.acento, o(83, 28), o(90, 26), strokeWidth = 1.4f * u)
        d.drawLine(p.acento, o(83, 33), o(90, 31), strokeWidth = 1.4f * u)
    }

    /** Building and two speech bubbles (languages). */
    private fun idiomas() {
        edificio(12, 44, 54, 40, filas = 2, columnas = 3)
        rect(p.linea, 10, 40, 58, 4)
        rect(p.acento, 64, 12, 28, 16, 5f)
        poligono(p.acento, 70 to 27, 70 to 34, 77 to 27)
        rect(p.fachada, 56, 30, 24, 13, 5f)
        borde(56, 30, 24, 13, 5f)
        for (i in 0 until 3) circulo(p.linea, 62 + i * 6, 36.5f, 1.4f)
    }

    /** Classical conservatory (pediment and columns) and a music note. */
    private fun musica() {
        poligono(p.linea, 14 to 40, 46 to 24, 78 to 40)
        rect(p.fachada, 18, 40, 56, 44)
        borde(18, 40, 56, 44)
        for (i in 0 until 4) rect(p.ventana, 23 + i * 13, 46, 6, 28, 1f)
        rect(p.linea, 16, 80, 60, 4)
        rect(p.puerta, 41, 70, 10, 14, 1.5f)
        circulo(p.acento, 84, 40, 4.5f)
        d.drawLine(p.acento, o(88, 40), o(88, 18), strokeWidth = 2f * u)
        poligono(p.acento, 88 to 18, 95 to 22, 95 to 27, 88 to 23)
    }

    /** Building and a palette with paint drops (art schools). */
    private fun arte() {
        edificio(12, 44, 52, 40, filas = 2, columnas = 3)
        rect(p.linea, 10, 40, 56, 4)
        d.drawOval(p.fachada, o(66, 14), s(28, 22))
        d.drawOval(p.linea, o(66, 14), s(28, 22), style = trazo)
        circulo(p.acento, 73, 21, 2.6f)
        circulo(p.acento2, 81, 19, 2.6f)
        circulo(p.linea, 87, 25, 2.6f)
        circulo(p.cielo, 74, 30, 3f)
    }

    /** Low, wide building with a ramp and a heart (special education). */
    private fun especial() {
        rect(p.linea, 12, 50, 66, 4)
        edificio(14, 54, 62, 30, filas = 1, columnas = 4)
        poligono(p.ventana, 76 to 84, 92 to 84, 76 to 76)
        poligono(p.linea, 76 to 84, 92 to 84, 76 to 76, relleno = false)
        val corazon = Path().apply {
            moveTo(80 * u, 40 * u)
            cubicTo(66 * u, 30 * u, 72 * u, 16 * u, 80 * u, 24 * u)
            cubicTo(88 * u, 16 * u, 94 * u, 30 * u, 80 * u, 40 * u)
            close()
        }
        d.drawPath(corazon, p.acento)
    }

    private fun generico() {
        poligono(p.linea, 16 to 44, 50 to 26, 84 to 44)
        edificio(20, 44, 60, 40, filas = 2, columnas = 3)
    }
}
