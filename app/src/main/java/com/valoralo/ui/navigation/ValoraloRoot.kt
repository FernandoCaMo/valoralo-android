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
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoralo.domain.model.Sesion
import com.valoralo.ui.account.AboutScreen
import com.valoralo.ui.account.AccountScreen
import com.valoralo.ui.account.LoginScreen
import com.valoralo.ui.account.RegistroScreen
import com.valoralo.ui.account.SesionViewModel
import com.valoralo.ui.detail.DetailScreen
import com.valoralo.ui.detail.DetailViewModel
import com.valoralo.ui.favorites.FavoritesScreen
import com.valoralo.ui.home.HomeScreen
import kotlinx.coroutines.launch

@Composable
fun ValoraloRoot(
    navController: NavHostController = rememberNavController(),
    sesionViewModel: SesionViewModel = hiltViewModel(),
) {
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
    val sesion by sesionViewModel.sesion.collectAsStateWithLifecycle()

    // First Google login: the account exists but usuarios/{uid} doesn't yet → complete the profile.
    LaunchedEffect(sesion, backStack?.destination?.route) {
        val s = sesion
        val ruta = backStack?.destination?.route
        if (s is Sesion.Iniciada && s.perfil == null && s.conGoogle && ruta != null && ruta != Rutas.COMPLETAR_PERFIL) {
            navController.navigate(Rutas.COMPLETAR_PERFIL) { launchSingleTop = true }
        }
    }

    fun volverACuenta() {
        navController.navigate(Rutas.CUENTA) {
            popUpTo(Rutas.CUENTA) { inclusive = false }
            launchSingleTop = true
        }
    }

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
                    onAnadirValoracion = {
                        val s = sesion
                        if (s is Sesion.Iniciada && s.perfil != null) {
                            avisar("El formulario de valoración llegará muy pronto.")
                        } else {
                            avisar("Inicia sesión para valorar centros.")
                            navController.navigate(Rutas.LOGIN)
                        }
                    },
                )
            }
            composable(Rutas.FAVORITOS) { FavoritesScreen() }
            composable(Rutas.CUENTA) {
                AccountScreen(
                    onIniciarSesion = { navController.navigate(Rutas.LOGIN) },
                    onCrearCuenta = { navController.navigate(Rutas.REGISTRO) },
                    onCompletarPerfil = { navController.navigate(Rutas.COMPLETAR_PERFIL) },
                    onAcercaDe = { navController.navigate(Rutas.ACERCA_DE) },
                )
            }
            composable(Rutas.ACERCA_DE) { AboutScreen(onVolver = { navController.popBackStack() }) }
            composable(Rutas.LOGIN) {
                LoginScreen(
                    onVolver = { navController.popBackStack() },
                    onCrearCuenta = { navController.navigate(Rutas.REGISTRO) },
                    // Back to where the user came from (Mi cuenta or a centro).
                    onTerminado = { navController.popBackStack() },
                )
            }
            composable(Rutas.REGISTRO) {
                RegistroScreen(
                    soloPerfil = false,
                    onVolver = { navController.popBackStack() },
                    onTerminado = {
                        avisar("¡Te damos la bienvenida a Valoralo!")
                        volverACuenta()
                    },
                )
            }
            composable(Rutas.COMPLETAR_PERFIL) {
                RegistroScreen(
                    soloPerfil = true,
                    onVolver = { navController.popBackStack() },
                    onTerminado = {
                        avisar("¡Te damos la bienvenida a Valoralo!")
                        volverACuenta()
                    },
                )
            }
        }
    }
}

/** Centro detail belongs to the tab it was opened from; default to Inicio. */
private fun NavDestination.pestana(): Pestana? =
    Pestana.entries.firstOrNull { it.ruta == route }
        ?: when (route) {
            Rutas.CENTRO -> Pestana.INICIO
            Rutas.ACERCA_DE, Rutas.LOGIN, Rutas.REGISTRO, Rutas.COMPLETAR_PERFIL -> Pestana.CUENTA
            else -> null
        }

private fun NavHostController.irAPestana(pestana: Pestana) {
    navigate(pestana.ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
