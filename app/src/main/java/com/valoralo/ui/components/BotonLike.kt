package com.valoralo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.valoralo.domain.usecase.EstadoLike
import com.valoralo.domain.usecase.descripcionLike
import kotlinx.coroutines.launch

/**
 * Small heart with the number of likes, no words. [onClick] null = your own review
 * (only the number). TalkBack reads "Dar like, 12 likes" (not visible).
 */
@Composable
fun BotonLike(estado: EstadoLike, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val latido = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val descripcion = descripcionLike(estado.n, estado.dado, propia = onClick == null)
    val color = if (estado.dado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val base = modifier.clip(MaterialTheme.shapes.small)
    val interactivo = if (onClick != null) {
        base.clickable(role = Role.Button) {
            scope.launch {
                latido.animateTo(1.3f, tween(75))
                latido.animateTo(1f, tween(75))
            }
            onClick()
        }
    } else {
        base
    }
    Row(
        interactivo
            .semantics { contentDescription = descripcion }
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            if (estado.dado) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp).scale(latido.value),
        )
        // Already in the description; not read twice.
        Text("${estado.n}", style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.clearAndSetSemantics { })
    }
}
