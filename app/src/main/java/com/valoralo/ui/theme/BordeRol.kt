package com.valoralo.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valoralo.domain.model.Rol

/**
 * Border of a review card by the author's role: Docente blue, Estudiante terracotta, Otro the usual soft grey.
 * Colour is a help, not the only cue: the role is always written on the card (and the verified badge stays).
 * Docente and Estudiante reach ≥ 5:1 against the card surfaces in light and dark (WCAG asks 3:1 for borders).
 */
fun colorBordeRol(rol: Rol, oscuro: Boolean): Color = when (rol) {
    Rol.DOCENTE -> if (oscuro) Azul80 else Azul40
    Rol.ESTUDIANTE -> if (oscuro) Melocoton80 else Terracota40
    Rol.OTRO -> if (oscuro) NeutroVar30 else NeutroVar80 // = outlineVariant, the default card border
}

fun grosorBordeRol(rol: Rol): Dp = if (rol == Rol.OTRO) 1.dp else 2.dp

@Composable
private fun temaOscuro() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

@Composable
fun bordeRol(rol: Rol): BorderStroke = BorderStroke(grosorBordeRol(rol), colorBordeRol(rol, temaOscuro()))

/**
 * Role label in the same colour as the border (carousel). Otro: the usual secondary-text grey, because the
 * soft border grey is too light for text. Docente and Estudiante are ≥ 4.5:1 as text on the card surfaces.
 */
@Composable
fun colorTextoRol(rol: Rol): Color =
    if (rol == Rol.OTRO) MaterialTheme.colorScheme.onSurfaceVariant else colorBordeRol(rol, temaOscuro())
