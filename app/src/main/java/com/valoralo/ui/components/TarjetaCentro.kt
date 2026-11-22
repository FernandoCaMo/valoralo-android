package com.valoralo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valoralo.domain.model.Centro

/**
 * Card of a centro on Buscar, Inicio and Favoritos (1.0.8): the whole card opens the centro (no more
 * "Ver más"), favourite bookmark, and "Comparar" as a toggle chip that never wraps.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TarjetaCentro(
    centro: Centro,
    esFavorito: Boolean,
    enComparador: Boolean,
    onAbrir: () -> Unit,
    onFavorito: () -> Unit,
    onComparar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onAbrir,
        modifier = modifier.fillMaxWidth().semantics { onClick(label = "ver el centro") { onAbrir(); true } },
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FotoCentro(centro.fotoUrl, centro.tipo, Modifier.size(72.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        centro.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(top = 4.dp),
                    )
                    BotonFavorito(esFavorito, centro.nombre, onFavorito)
                }
                Text(
                    centro.ubicacion(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.Center,
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    NotaMedia(centro.stats.total.global, centro.stats.total.n, Modifier.padding(end = 8.dp))
                    BotonComparar(enComparador, onComparar)
                }
            }
        }
    }
}

/** "Comparar" / "✓ Comparando": a toggle chip, one line at any font size. */
@Composable
fun BotonComparar(enComparador: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = enComparador,
        onClick = onClick,
        label = { Text(if (enComparador) "Comparando" else "Comparar", maxLines = 1, softWrap = false) },
        leadingIcon = if (enComparador) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null,
        modifier = modifier,
    )
}

@Composable
fun BotonFavorito(esFavorito: Boolean, nombre: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            if (esFavorito) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (esFavorito) "Quitar $nombre de favoritos" else "Guardar $nombre en favoritos",
            tint = if (esFavorito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
