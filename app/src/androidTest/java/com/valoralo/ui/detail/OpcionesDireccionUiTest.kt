package com.valoralo.ui.detail

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valoralo.domain.model.Centro
import com.valoralo.domain.usecase.EnlacesDireccion
import com.valoralo.domain.usecase.enlacesDireccion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 1.0.7: the address as a button with options (directions, copy, share). */
@RunWith(AndroidJUnit4::class)
class OpcionesDireccionUiTest {

    @get:Rule val regla = createComposeRule()

    private val centro = Centro(
        codigo = "41999901", nombre = "IES Monte Claro", tipo = "IES", direccion = "Avenida de Monte Claro, s/n",
        cp = "41020", localidad = "Sevilla", municipio = "Sevilla", provincia = "Sevilla", lat = 37.389123, lng = -5.984568,
    )
    private val direccion = "Avenida de Monte Claro, s/n, 41020 Sevilla"

    private class Falsas(var hayApp: Boolean = true) : AccionesDireccion {
        var ruta: EnlacesDireccion? = null
        var copiado: String? = null
        var compartido: Pair<EnlacesDireccion, String>? = null
        override fun comoLlegar(enlaces: EnlacesDireccion): Boolean { ruta = enlaces; return hayApp }
        override fun copiar(texto: String) { copiado = texto }
        override fun compartir(enlaces: EnlacesDireccion, nombreCentro: String) { compartido = enlaces to nombreCentro }
    }

    private val acciones = Falsas()
    private val avisos = mutableListOf<String>()

    @Before
    fun montar() {
        regla.setContent { DireccionConOpciones(centro, "Avenida de Monte Claro, s/n\n41020 Sevilla", { avisos += it }, acciones) }
    }

    private fun abrirOpciones() {
        regla.onNodeWithContentDescription("Dirección del centro: $direccion").performClick()
        regla.waitForIdle()
    }

    @Test
    fun paraTalkBackEsUnBotonQueAbreOpciones() {
        regla.onNodeWithContentDescription("Dirección del centro: $direccion")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher("acción «abrir opciones»") { it.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == "abrir opciones" })
    }

    @Test
    fun abreLaHojaConLasTresOpciones() {
        abrirOpciones()
        regla.onNodeWithText("Cómo llegar en Google Maps").assertExists()
        regla.onNodeWithText("Copiar dirección").assertExists()
        regla.onNodeWithText("Compartir dirección").assertExists()
        regla.onNodeWithText(direccion).assertExists()
    }

    @Test
    fun comoLlegarUsaLasCoordenadas() {
        abrirOpciones()
        regla.onNodeWithText("Cómo llegar en Google Maps").performClick()
        regla.waitForIdle()
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=37.389123%2C-5.984568", acciones.ruta?.rutaGoogleMaps)
        regla.onNodeWithText("Copiar dirección").assertDoesNotExist() // the sheet closes
    }

    @Test
    fun sinNingunaAppDeMapasAvisa() {
        acciones.hayApp = false
        abrirOpciones()
        regla.onNodeWithText("Cómo llegar en Google Maps").performClick()
        regla.waitForIdle()
        assertEquals(listOf("No hay ninguna app para abrir mapas en este móvil."), avisos)
    }

    @Test
    fun copiarDejaLaDireccionCompleta() {
        abrirOpciones()
        regla.onNodeWithText("Copiar dirección").performClick()
        regla.waitForIdle()
        assertEquals(direccion, acciones.copiado)
        // Android 13+ (the test emulator) confirms copies itself: no notice of ours.
        assertEquals(if (avisarAlCopiar()) listOf("Dirección copiada.") else emptyList<String>(), avisos)
    }

    @Test
    fun compartirLlevaNombreDireccionYEnlace() {
        abrirOpciones()
        regla.onNodeWithText("Compartir dirección").performClick()
        regla.waitForIdle()
        assertEquals("IES Monte Claro", acciones.compartido?.second)
        assertEquals(
            "IES Monte Claro\n$direccion\nhttps://www.google.com/maps/search/?api=1&query=37.389123%2C-5.984568",
            acciones.compartido?.first?.textoCompartir,
        )
    }

    @Test
    fun intentsDeComoLlegar_googleMapsLuegoOtraAppDeMapasLuegoNavegador() {
        val (maps, otraApp, navegador) = intentsComoLlegar(enlacesDireccion(centro))
        assertEquals(PAQUETE_GOOGLE_MAPS, maps.`package`)
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=37.389123%2C-5.984568", maps.dataString)
        assertNull(otraApp.`package`)
        assertEquals("geo:0,0?q=37.389123,-5.984568(IES%20Monte%20Claro)", otraApp.dataString)
        assertNull(navegador.`package`)
        assertEquals(maps.dataString, navegador.dataString)
        listOf(maps, otraApp, navegador).forEach { assertEquals(Intent.ACTION_VIEW, it.action) }
    }

    @Test
    fun intentDeCompartir() {
        val chooser = intentCompartir(enlacesDireccion(centro), "IES Monte Claro")
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val envio = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, envio.action)
        assertEquals("text/plain", envio.type)
        assertEquals("IES Monte Claro", envio.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals(enlacesDireccion(centro).textoCompartir, envio.getStringExtra(Intent.EXTRA_TEXT))
    }
}
