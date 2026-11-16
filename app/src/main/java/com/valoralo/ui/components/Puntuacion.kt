package com.valoralo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valoralo.domain.usecase.formatearNota
import com.valoralo.domain.usecase.textoResenas
import com.valoralo.ui.theme.Estrella

/** "4,5 ★ (124 reseñas)" as in the sketches. */
@Composable
fun NotaMedia(
    nota: Double?,
    n: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    iconSize: Dp = 18.dp,
) {
    val descripcion = if (nota == null) "Sin valoraciones"
    else "Nota ${formatearNota(nota)} de 5, ${textoResenas(n)}"
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = descripcion },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(formatearNota(nota), style = style.copy(fontWeight = FontWeight.Bold))
        Icon(Icons.Filled.Star, contentDescription = null, tint = Estrella, modifier = Modifier.size(iconSize))
        Text(
            "(${textoResenas(n)})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Five stars for a single 1–5 review note. */
@Composable
fun Estrellas(nota: Int, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Row(modifier.clearAndSetSemantics { contentDescription = "$nota de 5 estrellas" }) {
        repeat(5) { i ->
            Icon(
                imageVector = if (i < nota) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = Estrella,
                modifier = Modifier.size(size),
            )
        }
    }
}
