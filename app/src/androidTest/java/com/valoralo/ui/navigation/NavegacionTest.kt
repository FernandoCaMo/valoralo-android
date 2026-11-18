package com.valoralo.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tab and sign-in navigation with the real helpers and the real routes (screens replaced by labels).
 * Bug 1.0.0: after signing up from Buscar, every tap on Buscar went back to Mi cuenta.
 */
@RunWith(AndroidJUnit4::class)
class NavegacionTest {

    @get:Rule val regla = createComposeRule()
    private lateinit var nav: NavHostController

    @Before
    fun montar() {
        regla.setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = Rutas.HOME) {
                listOf(
                    Rutas.HOME, Rutas.SEARCH, Rutas.FAVORITOS, Rutas.CUENTA, Rutas.LOGIN, Rutas.REGISTRO,
                    Rutas.COMPLETAR_PERFIL, Rutas.ACERCA_DE, Rutas.CENTRO,
                ).forEach { ruta -> composable(ruta) { Text(ruta) } }
            }
        }
    }

    private fun hacer(accion: NavHostController.() -> Unit) {
        regla.runOnIdle { nav.accion() }
        regla.waitForIdle()
    }

    private fun rutaActual(): String? = regla.runOnIdle { nav.currentDestination?.route }

    @Test
    fun registrarseDesdeBuscarNoDejaMiCuentaDentroDeBuscar() {
        hacer { irAPestana(Pestana.BUSCAR) }
        hacer { navigate(Rutas.LOGIN) } // favourite without an account
        hacer { navigate(Rutas.REGISTRO) }
        hacer { terminarAcceso() }
        assertEquals(Rutas.CUENTA, rutaActual())

        hacer { irAPestana(Pestana.BUSCAR) }
        assertEquals(Rutas.SEARCH, rutaActual())
        hacer { irAPestana(Pestana.CUENTA) }
        assertEquals(Rutas.CUENTA, rutaActual())
        hacer { irAPestana(Pestana.BUSCAR) }
        assertEquals(Rutas.SEARCH, rutaActual())
    }

    @Test
    fun completarPerfilDeGoogleDesdeBuscar() {
        hacer { irAPestana(Pestana.BUSCAR) }
        hacer { navigate(Rutas.LOGIN) }
        hacer { popBackStack() } // Google login done
        hacer { navigate(Rutas.COMPLETAR_PERFIL) { launchSingleTop = true } }
        hacer { terminarAcceso() }
        hacer { irAPestana(Pestana.BUSCAR) }
        assertEquals(Rutas.SEARCH, rutaActual())
    }

    @Test
    fun cambiarDePestanaConElLoginAbiertoNoLoGuardaEnLaPestana() {
        hacer { irAPestana(Pestana.BUSCAR) }
        hacer { navigate(Rutas.LOGIN) }
        hacer { irAPestana(Pestana.CUENTA) } // leaves the login screen through the bottom bar
        hacer { irAPestana(Pestana.BUSCAR) }
        assertEquals(Rutas.SEARCH, rutaActual())
    }

    @Test
    fun registrarseDesdeMiCuentaVuelveAMiCuentaYAtrasNoVuelveAlFormulario() {
        hacer { irAPestana(Pestana.CUENTA) }
        hacer { navigate(Rutas.LOGIN) }
        hacer { navigate(Rutas.REGISTRO) }
        hacer { terminarAcceso() }
        assertEquals(Rutas.CUENTA, rutaActual())
        hacer { popBackStack() }
        assertEquals(Rutas.HOME, rutaActual())
    }

    @Test
    fun pulsarLaPestanaSeleccionadaVuelveASuPantallaPrincipal() {
        hacer { irAPestana(Pestana.BUSCAR) }
        hacer { navigate(Rutas.centro("21000011")) }
        hacer { volverAInicioDePestana(Pestana.BUSCAR) }
        assertEquals(Rutas.SEARCH, rutaActual())
        hacer { navigate(Rutas.centro("21000011")) }
        hacer { irAPestana(Pestana.INICIO) }
        hacer { volverAInicioDePestana(Pestana.INICIO) }
        assertEquals(Rutas.HOME, rutaActual())
    }

    @Test
    fun lasPestanasConservanSuPila() {
        hacer { irAPestana(Pestana.BUSCAR) }
        hacer { navigate(Rutas.centro("21000011")) }
        hacer { irAPestana(Pestana.CUENTA) }
        hacer { navigate(Rutas.ACERCA_DE) }
        hacer { irAPestana(Pestana.BUSCAR) }
        assertEquals(Rutas.CENTRO, rutaActual())
        hacer { irAPestana(Pestana.CUENTA) }
        assertEquals(Rutas.ACERCA_DE, rutaActual())
    }
}
