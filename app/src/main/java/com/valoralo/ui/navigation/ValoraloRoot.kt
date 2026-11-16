package com.valoralo.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.valoralo.ui.account.AboutScreen
import com.valoralo.ui.account.AccountScreen
import com.valoralo.ui.detail.DetailScreen
import com.valoralo.ui.detail.DetailViewModel
import com.valoralo.ui.favorites.FavoritesScreen
import com.valoralo.ui.home.HomeScreen
import kotlinx.coroutines.launch

@Composable
fun ValoraloRoot(navController: NavHostController = rememberNavController()) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun avisar(mensaje: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(mensaje)
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val pestanaActual = backStack?.destination?.pestana()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Pestana.entries.forEach { pestana ->
                    val seleccionada = pestana == pestanaActual
                    NavigationBarItem(
                        selected = seleccionada,
                        onClick = { navController.irAPestana(pestana) },
                        icon = {
                            Icon(
                                if (seleccionada) pestana.iconoSeleccionado else pestana.icono,
                                contentDescription = null,
                            )
                        },
                        label = { Text(pestana.etiqueta) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Rutas.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Rutas.HOME) {
                HomeScreen(
                    onVerCentro = { navController.navigate(Rutas.centro(it)) },
                    onComparar = { avisar("El comparador llegará pronto.") },
                )
            }
            composable(Rutas.SEARCH) {
                HomeScreen(
                    onVerCentro = { navController.navigate(Rutas.centro(it)) },
                    onComparar = { avisar("El comparador llegará pronto.") },
                    enfocarBuscador = true,
                )
            }
            composable(
                Rutas.CENTRO,
                arguments = listOf(navArgument(DetailViewModel.ARG_CODIGO) { type = NavType.StringType }),
            ) {
                DetailScreen(
                    onVolver = { navController.popBackStack() },
                    onAnadirValoracion = { avisar("Necesitas una cuenta para valorar. Muy pronto podrás crearla.") },
                )
            }
            composable(Rutas.FAVORITOS) { FavoritesScreen() }
            composable(Rutas.CUENTA) { AccountScreen(onAcercaDe = { navController.navigate(Rutas.ACERCA_DE) }) }
            composable(Rutas.ACERCA_DE) { AboutScreen(onVolver = { navController.popBackStack() }) }
        }
    }
}

/** Centro detail belongs to the tab it was opened from; default to Inicio. */
private fun NavDestination.pestana(): Pestana? =
    Pestana.entries.firstOrNull { it.ruta == route }
        ?: when (route) {
            Rutas.CENTRO -> Pestana.INICIO
            Rutas.ACERCA_DE -> Pestana.CUENTA
            else -> null
        }

private fun NavHostController.irAPestana(pestana: Pestana) {
    navigate(pestana.ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
