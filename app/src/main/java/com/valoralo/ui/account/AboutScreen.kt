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
import com.valoralo.ui.components.FUENTE_DATOS
import com.valoralo.ui.components.URL_DATASET_CENTROS
import com.valoralo.ui.components.URL_LICENCIA_CC_BY

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onVolver: () -> Unit) {
    val uriHandler = LocalUriHandler.current
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
            Text("Valoralo ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge)
            Text(
                "Valoraciones de centros educativos públicos de Andalucía: el centro, " +
                    "sus instalaciones y el profesorado por departamento.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text("Datos de los centros", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(
                "Los datos de los centros (nombre, dirección, contacto y enseñanzas) proceden del " +
                    "\"Directorio de centros docentes no universitarios de Andalucía\", publicado por la " +
                    "Consejería de Desarrollo Educativo y Formación Profesional en el portal de datos " +
                    "abiertos de la Junta de Andalucía.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(FUENTE_DATOS, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { runCatching { uriHandler.openUri(URL_DATASET_CENTROS) } }) {
                Text("Ver el conjunto de datos")
            }
            TextButton(onClick = { runCatching { uriHandler.openUri(URL_LICENCIA_CC_BY) } }) {
                Text("Licencia CC BY 4.0")
            }
            Text(
                "Las valoraciones son opiniones de los usuarios y no representan a la Junta de Andalucía.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
