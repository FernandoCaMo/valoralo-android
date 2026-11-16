package com.valoralo.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.valoralo.ui.components.PantallaPendiente

@Composable
fun AccountScreen(onAcercaDe: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            PantallaPendiente(
                icono = Icons.Outlined.AccountCircle,
                titulo = "Mi cuenta",
                texto = "Pronto podrás registrarte, iniciar sesión y verificarte como docente.",
            )
        }
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Acerca de") },
            supportingContent = { Text("Versión, fuente de los datos y licencia") },
            leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().clickable(onClick = onAcercaDe),
        )
    }
}
