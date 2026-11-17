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
import com.valoralo.ui.home.InicioScreen
import com.valoralo.ui.search.BuscarScreen
import com.valoralo.ui.review.ReviewScreen
import com.valoralo.ui.verify.VerificarDocenteScreen
import com.valoralo.ui.review.ReviewViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.valoralo.ui.compare.CompareScreen
import com.valoralo.ui.compare.CompareViewModel
import com.valoralo.ui.components.AccionesCentroViewModel
import com.valoralo.ui.components.EventoAccion
import kotlinx.coroutines.launch

@Composable
fun ValoraloRoot(
    navController: NavHostController = rememberNavController(),
    sesionViewModel: SesionViewModel = hiltViewModel(),
    acciones: AccionesCentroViewModel = hiltViewModel(),
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

    // Favourite / compare actions: messages, and login when a favourite needs an account.
    LaunchedEffect(acciones) {
        acciones.eventos.collect { evento ->
            when (evento) {
                is EventoAccion.Aviso -> avisar(evento.texto)
                EventoAccion.IniciarSesion -> {
                    avisar("Inicia sesión para guardar centros en favoritos.")
                    navController.navigate(Rutas.LOGIN)
                }
            }
        }
    }
    val seleccion by acciones.seleccion.collectAsStateWithLifecycle()
    val enComparador = backStack?.destination?.route == Rutas.COMPARAR

    fun volverACuenta() {
        navController.navigate(Rutas.CUENTA) {
            popUpTo(Rutas.CUENTA) { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
          Column {
            if (seleccion.isNotEmpty() && !enComparador) {
                BandejaComparador(
                    n = seleccion.size,
                    onVer = { navController.navigate(Rutas.comparar(seleccion)) },
                    onVaciar = acciones::vaciarComparador,
                )
            }
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
          }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Rutas.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Rutas.HOME) {
                InicioScreen(
                    acciones = acciones,
                    onVerCentro = { navController.navigate(Rutas.centro(it)) },
                    onBuscar = { navController.irAPestana(Pestana.BUSCAR) },
                    onVerFavoritos = { navController.irAPestana(Pestana.FAVORITOS) },
                )
            }
            composable(Rutas.SEARCH) {
                BuscarScreen(onVerCentro = { navController.navigate(Rutas.centro(it)) }, acciones = acciones)
            }
            composable(
                Rutas.COMPARAR,
                arguments = listOf(navArgument(CompareViewModel.ARG_IDS) { type = NavType.StringType; defaultValue = "" }),
            ) {
                CompareScreen(
                    onVolver = { navController.popBackStack() },
                    onVerCentro = { navController.navigate(Rutas.centro(it)) },
                )
            }
            composable(
                Rutas.CENTRO,
                arguments = listOf(navArgument(DetailViewModel.ARG_CODIGO) { type = NavType.StringType }),
            ) {
                DetailScreen(
                    onVolver = { navController.popBackStack() },
                    onAnadirValoracion = { centro ->
                        val s = sesion
                        if (s is Sesion.Iniciada && s.perfil != null) {
                            navController.navigate(Rutas.valorar(centro.codigo))
                        } else {
                            avisar("Inicia sesión para valorar centros.")
                            navController.navigate(Rutas.LOGIN)
                        }
                    },
                    onIniciarSesion = {
                        avisar("Inicia sesión para denunciar una reseña.")
                        navController.navigate(Rutas.LOGIN)
                    },
                    onAviso = ::avisar,
                    acciones = acciones,
                )
            }
            composable(
                Rutas.VALORAR,
                arguments = listOf(navArgument(ReviewViewModel.ARG_CODIGO) { type = NavType.StringType }),
            ) {
                ReviewScreen(
                    onVolver = { navController.popBackStack() },
                    onTerminado = { mensaje ->
                        avisar(mensaje)
                        navController.popBackStack()
                    },
                )
            }
            composable(Rutas.FAVORITOS) {
                FavoritesScreen(
                    acciones = acciones,
                    onVerCentro = { navController.navigate(Rutas.centro(it)) },
                    onIniciarSesion = { navController.navigate(Rutas.LOGIN) },
                )
            }
            composable(Rutas.CUENTA) {
                AccountScreen(
                    onIniciarSesion = { navController.navigate(Rutas.LOGIN) },
                    onCrearCuenta = { navController.navigate(Rutas.REGISTRO) },
                    onCompletarPerfil = { navController.navigate(Rutas.COMPLETAR_PERFIL) },
                    onAcercaDe = { navController.navigate(Rutas.ACERCA_DE) },
                    onVerificar = { navController.navigate(Rutas.VERIFICAR_DOCENTE) },
                )
            }
            composable(Rutas.VERIFICAR_DOCENTE) { VerificarDocenteScreen(onVolver = { navController.popBackStack() }) }
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

/** "Comparar (n)" tray above the bottom bar while centros are selected. */
@Composable
private fun BandejaComparador(n: Int, onVer: () -> Unit, onVaciar: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (n == 1) "1 centro para comparar: elige otro" else "$n centros para comparar",
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onVaciar) { Text("Vaciar") }
            Button(onClick = onVer, enabled = n >= 2) { Text("Comparar") }
        }
    }
}

/** Centro detail belongs to the tab it was opened from; default to Inicio. */
private fun NavDestination.pestana(): Pestana? =
    Pestana.entries.firstOrNull { it.ruta == route }
        ?: when (route) {
            Rutas.CENTRO, Rutas.VALORAR, Rutas.COMPARAR -> Pestana.INICIO
            Rutas.ACERCA_DE, Rutas.LOGIN, Rutas.REGISTRO, Rutas.COMPLETAR_PERFIL, Rutas.VERIFICAR_DOCENTE -> Pestana.CUENTA
            else -> null
        }

private fun NavHostController.irAPestana(pestana: Pestana) {
    navigate(pestana.ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
