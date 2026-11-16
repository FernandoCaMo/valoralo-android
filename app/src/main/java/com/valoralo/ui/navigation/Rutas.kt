package com.valoralo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** Navigation routes from doc 02. */
object Rutas {
    const val HOME = "home"
    const val SEARCH = "search"
    const val CENTRO = "centro/{codigo}"
    const val FAVORITOS = "favoritos"
    const val CUENTA = "cuenta"
    const val ACERCA_DE = "cuenta/acerca-de"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val COMPLETAR_PERFIL = "registro/perfil"

    fun centro(codigo: String) = "centro/$codigo"
}

enum class Pestana(
    val ruta: String,
    val etiqueta: String,
    val icono: ImageVector,
    val iconoSeleccionado: ImageVector,
) {
    INICIO(Rutas.HOME, "Inicio", Icons.Outlined.Home, Icons.Filled.Home),
    BUSCAR(Rutas.SEARCH, "Buscar", Icons.Outlined.Search, Icons.Filled.Search),
    FAVORITOS(Rutas.FAVORITOS, "Favoritos", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    CUENTA(Rutas.CUENTA, "Mi cuenta", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle),
}
