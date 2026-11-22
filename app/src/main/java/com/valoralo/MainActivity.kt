package com.valoralo

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.valoralo.ui.navigation.ValoraloRoot
import com.valoralo.ui.theme.ValoraloTheme
import com.valoralo.ui.theme.Apariencia
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint

/** The app's language (all its texts are Spanish). */
val LOCALE_APP: Locale = Locale.forLanguageTag("es-ES")

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * The app is only in Spanish: system components (Material's date picker, date formats) follow it too,
     * whatever the phone's language (QA 1.0.5, finding 7: "Select date", MM/DD/YYYY in an English phone).
     */
    override fun attachBaseContext(base: Context) {
        val config = Configuration(base.resources.configuration).apply { setLocale(LOCALE_APP) }
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen (Theme.Valoralo.Inicio) until the first frame.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Apariencia.cargar(this)
        enableEdgeToEdge()
        setContent {
            val tema by Apariencia.tema.collectAsState()
            val oscuro = Apariencia.oscuro(tema, isSystemInDarkTheme())
            // System bar icons follow the app's theme, not only the system's (Mi cuenta → Tema).
            DisposableEffect(oscuro) {
                val estilo = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
                onDispose {}
            }
            ValoraloTheme(darkTheme = oscuro) {
                ValoraloRoot()
            }
        }
    }
}
