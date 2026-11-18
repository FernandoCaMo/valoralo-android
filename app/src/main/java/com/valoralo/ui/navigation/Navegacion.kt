package com.valoralo.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/** Sign-in flow screens: they can be opened from any tab but never belong to one. */
val RUTAS_ACCESO = setOf(Rutas.LOGIN, Rutas.REGISTRO, Rutas.COMPLETAR_PERFIL)

/**
 * Bottom-bar tab switch: each tab keeps its own back stack (saved and restored).
 * The sign-in flow is dropped first, so it is never saved inside a tab (1.0.0 bug: after signing up
 * from Buscar, Mi cuenta stayed in the saved Buscar stack and every tap on Buscar showed Mi cuenta).
 */
fun NavHostController.irAPestana(pestana: Pestana) {
    quitarFlujoDeAcceso()
    navigate(pestana.ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Root route of each bottom-bar tab. */
val RUTAS_PESTANA = listOf(Rutas.HOME, Rutas.SEARCH, Rutas.FAVORITOS, Rutas.CUENTA)

/**
 * Tab a screen belongs to: the last tab root below it in the back stack (bottom → top). A centro or the
 * comparator opened from Buscar belongs to Buscar, not to Inicio (1.0.1 bug).
 */
fun rutaPestanaDe(pila: List<String?>): String? = pila.lastOrNull { it in RUTAS_PESTANA }

/** Tapping the tab that is already selected goes back to its main screen. */
fun NavHostController.volverAInicioDePestana(pestana: Pestana) {
    popBackStack(pestana.ruta, inclusive = false)
}

/** After signing up or completing the profile: drop the sign-in flow and show the Mi cuenta tab. */
fun NavHostController.terminarAcceso() = irAPestana(Pestana.CUENTA)

/** Pops everything from the first sign-in screen upwards, without saving it. */
private fun NavHostController.quitarFlujoDeAcceso() {
    val primera = currentBackStack.value.firstOrNull { it.destination.route in RUTAS_ACCESO } ?: return
    popBackStack(primera.destination.route!!, inclusive = true, saveState = false)
}
