package com.valoralo.ui.detail

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.valoralo.domain.model.Centro
import com.valoralo.domain.usecase.EnlacesDireccion
import com.valoralo.domain.usecase.enlacesDireccion

/** Google Maps' package: tried first for "Cómo llegar". */
const val PAQUETE_GOOGLE_MAPS = "com.google.android.apps.maps"

/** What the address options do. An interface so the UI test can check them without opening other apps. */
interface AccionesDireccion {
    /** Returns false if there is no app to open it (no maps app and no browser). */
    fun comoLlegar(enlaces: EnlacesDireccion): Boolean
    fun copiar(texto: String)
    fun compartir(enlaces: EnlacesDireccion, nombreCentro: String)
}

/**
 * "Cómo llegar": Google Maps (route preview to the centro); without it, any map app (geo: with the
 * centro's name); without any, the browser. No permissions: nothing asks for the user's location, and
 * each intent is just tried (no package-visibility queries in the manifest).
 */
fun intentsComoLlegar(enlaces: EnlacesDireccion): List<Intent> = listOf(
    Intent(Intent.ACTION_VIEW, Uri.parse(enlaces.rutaGoogleMaps)).setPackage(PAQUETE_GOOGLE_MAPS),
    Intent(Intent.ACTION_VIEW, Uri.parse(enlaces.geo)),
    Intent(Intent.ACTION_VIEW, Uri.parse(enlaces.rutaGoogleMaps)),
)

/** Android's share sheet with the centro's name, the address and the Maps link. */
fun intentCompartir(enlaces: EnlacesDireccion, nombreCentro: String): Intent = Intent.createChooser(
    Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, nombreCentro)
        .putExtra(Intent.EXTRA_TEXT, enlaces.textoCompartir),
    "Compartir dirección",
)

/** Since Android 13 the system confirms copies itself: our notice only up to Android 12 (no double notice). */
fun avisarAlCopiar(sdk: Int = Build.VERSION.SDK_INT): Boolean = sdk <= Build.VERSION_CODES.S_V2

class AccionesDireccionAndroid(private val context: Context) : AccionesDireccion {
    override fun comoLlegar(enlaces: EnlacesDireccion): Boolean =
        intentsComoLlegar(enlaces).any { intent ->
            try {
                context.startActivity(intent)
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }

    override fun copiar(texto: String) {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Dirección", texto))
    }

    override fun compartir(enlaces: EnlacesDireccion, nombreCentro: String) {
        runCatching { context.startActivity(intentCompartir(enlaces, nombreCentro)) }
    }
}

/**
 * The centro's address as a button: opens a bottom sheet with "Cómo llegar en Google Maps",
 * "Copiar dirección" and "Compartir dirección". TalkBack: "Dirección del centro: …, botón,
 * toca dos veces para abrir opciones".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DireccionConOpciones(
    centro: Centro,
    textoVisible: String,
    onAviso: (String) -> Unit,
    acciones: AccionesDireccion = AccionesDireccionAndroid(LocalContext.current),
) {
    val enlaces = remember(centro) { enlacesDireccion(centro) }
    var abierta by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("direccion")
            .clearAndSetSemantics {
                contentDescription = "Dirección del centro: ${enlaces.direccionCompleta}"
                role = Role.Button
                onClick(label = "abrir opciones") { abierta = true; true }
            }
            .clickable { abierta = true }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(textoVisible, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }

    if (abierta) {
        ModalBottomSheet(onDismissRequest = { abierta = false }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    centro.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() },
                )
                Text(
                    enlaces.direccionCompleta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                Opcion(Icons.Outlined.Directions, "Cómo llegar en Google Maps") {
                    abierta = false
                    if (!acciones.comoLlegar(enlaces)) onAviso("No hay ninguna app para abrir mapas en este móvil.")
                }
                Opcion(Icons.Outlined.ContentCopy, "Copiar dirección") {
                    abierta = false
                    acciones.copiar(enlaces.direccionCompleta)
                    if (avisarAlCopiar()) onAviso("Dirección copiada.")
                }
                Opcion(Icons.Outlined.Share, "Compartir dirección") {
                    abierta = false
                    acciones.compartir(enlaces, centro.nombre)
                }
            }
        }
    }
}

@Composable
private fun Opcion(icono: ImageVector, texto: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(texto) },
        leadingContent = { Icon(icono, contentDescription = null) },
        // The sheet's own background (ListItem defaults to the surface colour, which shows as a band).
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
    )
}
