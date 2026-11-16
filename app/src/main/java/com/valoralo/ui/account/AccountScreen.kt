package com.valoralo.ui.account

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.runtime.Composable
import com.valoralo.ui.components.PantallaPendiente

@Composable
fun AccountScreen() {
    PantallaPendiente(
        icono = Icons.Outlined.AccountCircle,
        titulo = "Mi cuenta",
        texto = "Pronto podrás registrarte, iniciar sesión y verificarte como docente.",
    )
}
