package com.valoralo.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.valoralo.BuildConfig
import com.valoralo.ui.components.CORREO_CONTACTO
import com.valoralo.ui.components.FUENTE_DATOS
import com.valoralo.ui.components.URL_CONDICIONES
import com.valoralo.ui.components.URL_DATASET_CENTROS
import com.valoralo.ui.components.URL_ELIMINAR_CUENTA
import com.valoralo.ui.components.URL_LICENCIA_CC_BY
import com.valoralo.ui.components.URL_PRIVACIDAD

/** What a user needs: version, legal links, contact and the data attribution. Nothing technical. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onVolver: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val abrir: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acerca de") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Valoralo", style = MaterialTheme.typography.titleLarge)
            Text("Versión ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Valoraciones de centros educativos públicos de Andalucía: el centro, " +
                    "sus instalaciones y el profesorado por departamento.",
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider()
            Titulo("Información legal")
            TextButton(onClick = { abrir(URL_CONDICIONES) }) { Text("Condiciones de uso") }
            TextButton(onClick = { abrir(URL_PRIVACIDAD) }) { Text("Política de privacidad") }
            TextButton(onClick = { abrir(URL_ELIMINAR_CUENTA) }) { Text("Cómo eliminar tu cuenta") }

            HorizontalDivider()
            Titulo("Contacto")
            Text(
                "Para dudas, sugerencias o para pedir que retiremos un contenido, escríbenos.",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = { abrir("mailto:$CORREO_CONTACTO") }) { Text(CORREO_CONTACTO) }

            HorizontalDivider()
            Titulo("Datos de los centros")
            Text(
                "Los datos de los centros (nombre, dirección, contacto y enseñanzas) proceden del " +
                    "\"Directorio de centros docentes no universitarios de Andalucía\", publicado por la " +
                    "Consejería de Desarrollo Educativo y Formación Profesional en el portal de datos " +
                    "abiertos de la Junta de Andalucía.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(FUENTE_DATOS, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { abrir(URL_DATASET_CENTROS) }) { Text("Ver el conjunto de datos") }
            TextButton(onClick = { abrir(URL_LICENCIA_CC_BY) }) { Text("Licencia CC BY 4.0") }
            Text(
                "Valoralo es un proyecto independiente: no está vinculado a la Junta de Andalucía ni a ningún centro. " +
                    "Las valoraciones son opiniones de los usuarios.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}
