package com.valoralo.ui.favorites

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import com.valoralo.ui.components.PantallaPendiente

@Composable
fun FavoritesScreen() {
    PantallaPendiente(
        icono = Icons.Outlined.FavoriteBorder,
        titulo = "Favoritos",
        texto = "Aquí verás los centros que guardes. Disponible próximamente.",
    )
}
