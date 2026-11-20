package com.valoralo.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
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

/** Swiping between tabs with the real RaizPestana, routes and transitions (screens replaced by simple content). */
@RunWith(AndroidJUnit4::class)
class DeslizarPestanasUiTest {

    @get:Rule val regla = createComposeRule()
    private lateinit var nav: NavHostController
    private var botonPulsado = false

    @Before
    fun montar() {
        regla.setContent {
            nav = rememberNavController()
            val irA: (Pestana) -> Unit = { nav.irAPestana(it) }
            NavHost(
                nav, startDestination = Rutas.HOME,
                enterTransition = entradaNavegacion, exitTransition = salidaNavegacion,
                popEnterTransition = entradaNavegacion, popExitTransition = salidaNavegacion,
            ) {
                composable(Rutas.HOME) {
                    RaizPestana(Pestana.INICIO, irA) {
                        Column(Modifier.fillMaxSize().testTag("pantalla")) {
                            Text(Rutas.HOME)
                            // Stands for the reviews carousel.
                            HorizontalPager(rememberPagerState { 3 }, Modifier.fillMaxWidth().height(200.dp).testTag("carrusel")) {
                                Text("Reseña $it")
                            }
                        }
                    }
                }
                composable(Rutas.SEARCH) {
                    RaizPestana(Pestana.BUSCAR, irA) {
                        var texto by rememberSaveable { mutableStateOf("") }
                        Column(Modifier.fillMaxSize().testTag("pantalla")) {
                            Text(Rutas.SEARCH)
                            TextField(texto, { texto = it }, Modifier.testTag("busqueda"))
                        }
                    }
                }
                composable(Rutas.FAVORITOS) {
                    // A full-width button, like "Iniciar sesión" in Favoritos without an account.
                    RaizPestana(Pestana.FAVORITOS, irA) {
                        Button(onClick = { botonPulsado = true }, Modifier.fillMaxSize().testTag("pantalla")) { Text(Rutas.FAVORITOS) }
                    }
                }
                composable(Rutas.CUENTA) { RaizPestana(Pestana.CUENTA, irA) { Text(Rutas.CUENTA, Modifier.fillMaxSize().testTag("pantalla")) } }
                // Not a tab main screen: no RaizPestana.
                composable(Rutas.CENTRO) { Text(Rutas.CENTRO, Modifier.fillMaxSize().testTag("pantalla")) }
            }
        }
    }

    private fun rutaActual(): String? = regla.runOnIdle { nav.currentDestination?.route }

    /** From 80 % to 20 % of the width, away from the system back-gesture edges. */
    private fun deslizarAIzquierda(tag: String = "pantalla") {
        regla.onNodeWithTag(tag, useUnmergedTree = true).performTouchInput { swipeLeft(startX = width * 0.8f, endX = width * 0.2f) }
        regla.waitForIdle()
    }

    private fun deslizarADerecha() {
        regla.onNodeWithTag("pantalla", useUnmergedTree = true).performTouchInput { swipeRight(startX = width * 0.2f, endX = width * 0.8f) }
        regla.waitForIdle()
    }

    @Test
    fun deslizarRecorreLasPestanasEnOrden() {
        deslizarAIzquierda()
        assertEquals(Rutas.SEARCH, rutaActual())
        deslizarAIzquierda()
        assertEquals(Rutas.FAVORITOS, rutaActual())
        deslizarAIzquierda()
        assertEquals(Rutas.CUENTA, rutaActual())
        deslizarAIzquierda() // last tab: nothing further
        assertEquals(Rutas.CUENTA, rutaActual())
        deslizarADerecha()
        assertEquals(Rutas.FAVORITOS, rutaActual())
    }

    @Test
    fun deslizarSobreUnBotonNoLoPulsa() {
        regla.runOnIdle { nav.irAPestana(Pestana.FAVORITOS) }
        regla.waitForIdle()
        deslizarADerecha()
        assertEquals(Rutas.SEARCH, rutaActual())
        assertEquals(false, botonPulsado)
    }

    @Test
    fun enElCarruselSeMueveElCarruselYNoLaPestana() {
        deslizarAIzquierda("carrusel")
        assertEquals(Rutas.HOME, rutaActual())
        regla.onNodeWithText("Reseña 1").assertExists()
    }

    @Test
    fun enElDetalleDeUnCentroNoSeCambiaDePestana() {
        regla.runOnIdle { nav.irAPestana(Pestana.BUSCAR) }
        regla.runOnIdle { nav.navigate(Rutas.centro("41999901")) }
        regla.waitForIdle()
        deslizarAIzquierda()
        assertEquals(Rutas.CENTRO, rutaActual())
    }

    @Test
    fun laPestanaConservaSuEstadoAlIrYVolverDeslizando() {
        deslizarAIzquierda()
        regla.onNodeWithTag("busqueda").performTextInput("Monte Claro")
        deslizarAIzquierda()
        assertEquals(Rutas.FAVORITOS, rutaActual())
        deslizarADerecha()
        assertEquals(Rutas.SEARCH, rutaActual())
        regla.onNodeWithText("Monte Claro").assertExists()
    }
}
