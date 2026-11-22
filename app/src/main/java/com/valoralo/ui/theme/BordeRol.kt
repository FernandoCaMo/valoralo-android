package com.valoralo.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valoralo.domain.model.Rol

/** Candidates for the Docente colour (1.0.8): it must not be the brand blue of buttons and links. */
enum class ColorDocente { VERDE, TEAL }

/** Chosen with the before/after captures (docs/diseno). */
val PROPUESTA_DOCENTE = ColorDocente.VERDE

/**
 * Border of a review card by the author's role: Docente green (1.0.8; was the brand blue), Estudiante
 * terracotta, Otro the usual soft grey. Colour is a help, not the only cue: the role is always written on
 * the card (and the verified badge stays). Docente and Estudiante reach ≥ 4.5:1 against the card surfaces
 * in light and dark (WCAG asks 3:1 for borders, 4.5:1 for the carousel's role label).
 */
fun colorBordeRol(rol: Rol, oscuro: Boolean, docente: ColorDocente = PROPUESTA_DOCENTE): Color = when (rol) {
    Rol.DOCENTE -> when (docente) {
        ColorDocente.VERDE -> if (oscuro) DocenteVerde80 else DocenteVerde40
        ColorDocente.TEAL -> if (oscuro) DocenteTeal80 else DocenteTeal40
    }
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
 * soft border grey is too light for text.
 */
@Composable
fun colorTextoRol(rol: Rol): Color =
    if (rol == Rol.OTRO) MaterialTheme.colorScheme.onSurfaceVariant else colorBordeRol(rol, temaOscuro())

/** "Docente verificado" badge: container and content (green, apart from the selection blue). */
@Composable
fun coloresInsigniaVerificado(): Pair<Color, Color> =
    if (temaOscuro()) VerificadoContenedorOscuro to OnVerificadoContenedorOscuro else VerificadoContenedor to OnVerificadoContenedor
