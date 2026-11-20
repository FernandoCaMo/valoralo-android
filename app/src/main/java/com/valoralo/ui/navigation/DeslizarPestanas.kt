package com.valoralo.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import kotlin.math.abs

/** Minimum horizontal travel for a swipe, or a shorter one if it is fast. */
private val DISTANCIA_MINIMA = 56.dp
private val DISTANCIA_MINIMA_RAPIDO = 24.dp
private val VELOCIDAD_RAPIDO = 800.dp // per second
private const val DURACION_MS = 300

/**
 * Tab a horizontal swipe leads to, or null. [dx] < 0 = finger moved left → next tab (to the right in the bar).
 * Only clearly horizontal gestures count (|dx| > 2·|dy|), long enough, or shorter but fast.
 */
fun pestanaAlDeslizar(
    actual: Pestana,
    dx: Float,
    dy: Float,
    velocidadX: Float,
    distanciaMinima: Float,
    distanciaMinimaRapido: Float,
    velocidadRapido: Float,
): Pestana? {
    if (dx == 0f || abs(dx) <= 2 * abs(dy)) return null
    val basta = abs(dx) >= distanciaMinima ||
        (abs(dx) >= distanciaMinimaRapido && abs(velocidadX) >= velocidadRapido && velocidadX * dx > 0)
    if (!basta) return null
    return Pestana.entries.getOrNull(actual.ordinal + if (dx < 0) 1 else -1)
}

/** Android's back gesture starts in these side strips (system gesture insets): leave them to the system. */
fun empiezaEnBordeDelSistema(x: Float, ancho: Float, bordeIzquierdo: Float, bordeDerecho: Float): Boolean =
    x < bordeIzquierdo || x > ancho - bordeDerecho

/**
 * Main screen of a tab: swiping left/right changes tab. Children that already move sideways (the reviews
 * carousel, any pager or horizontal list) consume the drag, and then the tab does not change.
 */
@Composable
fun RaizPestana(pestana: Pestana, onCambiar: (Pestana) -> Unit, content: @Composable () -> Unit) {
    val cambiar by rememberUpdatedState(onCambiar)
    val densidad = LocalDensity.current
    val direccion = LocalLayoutDirection.current
    val gestos = WindowInsets.systemGestures
    val bordeIzquierdo = gestos.getLeft(densidad, direccion).toFloat()
    val bordeDerecho = gestos.getRight(densidad, direccion).toFloat()
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(pestana, bordeIzquierdo, bordeDerecho) {
                val distancia = DISTANCIA_MINIMA.toPx()
                val distanciaRapido = DISTANCIA_MINIMA_RAPIDO.toPx()
                val velocidad = VELOCIDAD_RAPIDO.toPx()
                awaitEachGesture {
                    // Main pass runs from the children up: the carousel, lists… have already seen (and maybe
                    // consumed) each event when it reaches this parent.
                    val abajo = awaitFirstDown(requireUnconsumed = false)
                    if (empiezaEnBordeDelSistema(abajo.position.x, size.width.toFloat(), bordeIzquierdo, bordeDerecho)) {
                        return@awaitEachGesture
                    }
                    val seguimiento = VelocityTracker().apply { addPosition(abajo.uptimeMillis, abajo.position) }
                    var otroLoMueve = false
                    var mio = false
                    var ultimo = abajo.position
                    while (true) {
                        val evento = awaitPointerEvent()
                        if (evento.changes.size > 1) return@awaitEachGesture // pinch, two fingers…
                        val cambio = evento.changes.firstOrNull { it.id == abajo.id } ?: return@awaitEachGesture
                        if (cambio.pressed) {
                            if (!mio && cambio.isConsumed) otroLoMueve = true
                            seguimiento.addPosition(cambio.uptimeMillis, cambio.position)
                            ultimo = cambio.position
                            val d = ultimo - abajo.position
                            if (!otroLoMueve && !mio && abs(d.x) > viewConfiguration.touchSlop && abs(d.x) > 2 * abs(d.y)) {
                                mio = true
                            }
                            // Claim the horizontal drag: a button under the finger then cancels its press
                            // instead of being tapped when the finger lifts.
                            if (mio) cambio.consume()
                        } else {
                            if (otroLoMueve) return@awaitEachGesture
                            val desplazamiento = ultimo - abajo.position
                            pestanaAlDeslizar(
                                pestana, desplazamiento.x, desplazamiento.y, seguimiento.calculateVelocity().x,
                                distancia, distanciaRapido, velocidad,
                            )?.let(cambiar)
                            return@awaitEachGesture
                        }
                    }
                }
            },
    ) { content() }
}

private fun indicePestana(entrada: NavBackStackEntry) = RUTAS_PESTANA.indexOf(entrada.destination.route)

/** Between two tab main screens: slide in the bar's direction. Anything else: the NavHost default fade. */
val entradaNavegacion: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    val de = indicePestana(initialState)
    val a = indicePestana(targetState)
    if (de >= 0 && a >= 0 && de != a) {
        slideInHorizontally(tween(DURACION_MS, easing = FastOutSlowInEasing)) { ancho -> if (a > de) ancho else -ancho } +
            fadeIn(tween(DURACION_MS))
    } else {
        fadeIn(tween(700))
    }
}

val salidaNavegacion: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    val de = indicePestana(initialState)
    val a = indicePestana(targetState)
    if (de >= 0 && a >= 0 && de != a) {
        slideOutHorizontally(tween(DURACION_MS, easing = FastOutSlowInEasing)) { ancho -> if (a > de) -ancho else ancho } +
            fadeOut(tween(DURACION_MS))
    } else {
        fadeOut(tween(700))
    }
}
