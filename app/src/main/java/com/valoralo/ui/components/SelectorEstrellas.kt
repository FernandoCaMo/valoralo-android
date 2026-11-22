package com.valoralo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valoralo.ui.theme.Estrella

/** What each note means (1.0.8): shown next to the main stars and read by TalkBack. */
fun etiquetaNota(n: Int): String = when (n) {
    1 -> "Muy mal"
    2 -> "Mal"
    3 -> "Regular"
    4 -> "Bien"
    5 -> "Excelente"
    else -> ""
}

/**
 * 1–5 star picker. Each star is a 48 dp touch target announced as "etiqueta: n de 5, Bien".
 * With [conEtiqueta], the meaning of the chosen note is written after the stars.
 */
@Composable
fun SelectorEstrellas(
    nota: Int?,
    onNota: (Int) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 32.dp,
    conEtiqueta: Boolean = false,
) {
    Row(modifier.selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { n ->
            val llena = nota != null && n <= nota
            Box(
                Modifier
                    .size(48.dp)
                    .selectable(selected = nota == n, role = Role.RadioButton, onClick = { onNota(n) })
                    .semantics { contentDescription = "$etiqueta: $n de 5, ${etiquetaNota(n)}" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (llena) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = if (llena) Estrella else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(tamano),
                )
            }
        }
        if (conEtiqueta) {
            Text(
                nota?.let(::etiquetaNota) ?: "Sin nota",
                style = MaterialTheme.typography.titleSmall,
                color = if (nota == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
