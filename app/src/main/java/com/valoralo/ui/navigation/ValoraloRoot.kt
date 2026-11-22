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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.DialogProperties
import com.valoralo.ui.components.URL_CONDICIONES
import com.valoralo.ui.compare.CompareViewModel
import com.valoralo.ui.components.AccionesCentroViewModel
import com.valoralo.ui.components.EventoAccion
import kotlinx.coroutines.delay
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
    // Recomputed whenever the current entry changes (backStack is observed above).
    val pestanaActual = backStack?.let {
        val ruta = rutaPestanaDe(navController.currentBackStack.value.map { e -> e.destination.route })
        Pestana.entries.firstOrNull { p -> p.ruta == ruta }
    }
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
                is EventoAccion.IniciarSesion -> {
                    avisar(evento.mensaje)
                    navController.navigate(Rutas.LOGIN)
                }
            }
        }
    }
    // A bookmark or like waits for the sign-in it triggered; leaving the sign-in flow without an account drops it.
    // Right after signing in the session is still "SinSesion" until the profile is read: wait a moment
    // (a new session value cancels this effect) before deciding the sign-in was abandoned.
    LaunchedEffect(backStack?.destination?.route, sesion) {
        val ruta = backStack?.destination?.route ?: return@LaunchedEffect
        if (ruta !in RUTAS_ACCESO && sesion is Sesion.SinSesion) {
            delay(5_000)
            acciones.descartarPendiente()
        }
    }
    val seleccion by acciones.seleccion.collectAsStateWithLifecycle()
    val enComparador = backStack?.destination?.route == Rutas.COMPARAR
    // 1.0.8: the review form is full screen (its own "Publicar" bar at the bottom).
    val pantallaCompleta = backStack?.destination?.route in RUTAS_PANTALLA_COMPLETA

    // Accounts from before the terms of use (or a new version): accept them once to keep using the account.
    val perfilActual = (sesion as? Sesion.Iniciada)?.perfil
    if (perfilActual != null && !perfilActual.aceptoCondicionesVigentes) {
        val errorCondiciones by sesionViewModel.errorCondiciones.collectAsStateWithLifecycle()
        AceptarCondiciones(
            error = errorCondiciones,
            onAceptar = sesionViewModel::aceptarCondiciones,
            onCerrarSesion = sesionViewModel::cerrarSesion,
        )
    }

    fun volverACuenta() = navController.terminarAcceso()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
          if (!pantallaCompleta) Column {
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
                        onClick = {
                            if (seleccionada) navController.volverAInicioDePestana(pestana)
                            else navController.irAPestana(pestana)
                        },
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
            enterTransition = entradaNavegacion,
            exitTransition = salidaNavegacion,
            popEnterTransition = entradaNavegacion,
            popExitTransition = salidaNavegacion,
        ) {
            val irA: (Pestana) -> Unit = { navController.irAPestana(it) }
            composable(Rutas.HOME) {
              RaizPestana(Pestana.INICIO, irA) {
                InicioScreen(
                    acciones = acciones,
                    onVerCentro = { navController.navigate(Rutas.centro(it)) },
                    onBuscar = { navController.irAPestana(Pestana.BUSCAR) },
                    onVerFavoritos = { navController.irAPestana(Pestana.FAVORITOS) },
                    onIniciarSesion = { mensaje ->
                        avisar(mensaje)
                        navController.navigate(Rutas.LOGIN)
                    },
                    onAviso = ::avisar,
                )
              }
            }
            composable(Rutas.SEARCH) {
                RaizPestana(Pestana.BUSCAR, irA) {
                    BuscarScreen(onVerCentro = { navController.navigate(Rutas.centro(it)) }, acciones = acciones)
                }
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
                    onIniciarSesion = { mensaje ->
                        avisar(mensaje)
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
              RaizPestana(Pestana.FAVORITOS, irA) {
                FavoritesScreen(
                    acciones = acciones,
                    onVerCentro = { navController.navigate(Rutas.centro(it)) },
                    onIniciarSesion = { navController.navigate(Rutas.LOGIN) },
                )
              }
            }
            composable(Rutas.CUENTA) {
              RaizPestana(Pestana.CUENTA, irA) {
                AccountScreen(
                    onIniciarSesion = { navController.navigate(Rutas.LOGIN) },
                    onCrearCuenta = { navController.navigate(Rutas.REGISTRO) },
                    onCompletarPerfil = { navController.navigate(Rutas.COMPLETAR_PERFIL) },
                    onAcercaDe = { navController.navigate(Rutas.ACERCA_DE) },
                    onVerificar = { navController.navigate(Rutas.VERIFICAR_DOCENTE) },
                    onAviso = ::avisar,
                )
              }
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

/** One-off acceptance of the terms of use; it can't be dismissed without accepting or signing out. */
@Composable
private fun AceptarCondiciones(error: String?, onAceptar: () -> Unit, onCerrarSesion: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Condiciones de uso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Hemos publicado las condiciones de uso de Valoralo: qué se puede publicar, cómo moderamos " +
                        "y cómo denunciar o bloquear. Para seguir usando tu cuenta, léelas y acéptalas.",
                )
                TextButton(onClick = { runCatching { uriHandler.openUri(URL_CONDICIONES) } }) { Text("Leer las condiciones de uso") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { Button(onClick = onAceptar) { Text("Las acepto") } },
        dismissButton = { TextButton(onClick = onCerrarSesion) { Text("Cerrar sesión") } },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    )
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

