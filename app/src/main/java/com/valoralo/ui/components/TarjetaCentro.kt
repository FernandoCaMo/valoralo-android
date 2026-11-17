package com.valoralo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valoralo.domain.model.Centro

/** Card of a centro on Buscar, Inicio and Favoritos: favourite heart, "Ver más" and "Comparar". */
@Composable
fun TarjetaCentro(
    centro: Centro,
    esFavorito: Boolean,
    enComparador: Boolean,
    onVerMas: () -> Unit,
    onFavorito: () -> Unit,
    onComparar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier.fillMaxWidth().clickable(onClick = onVerMas)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FotoCentro(centro.fotoUrl, centro.tipo, Modifier.size(96.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        centro.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    BotonFavorito(esFavorito, centro.nombre, onFavorito)
                }
                Text(
                    centro.ubicacion(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                NotaMedia(centro.stats.total.global, centro.stats.total.n)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onVerMas, contentPadding = ButtonDefaults.TextButtonContentPadding) {
                        Text("Ver más")
                    }
                    OutlinedButton(onClick = onComparar, contentPadding = ButtonDefaults.TextButtonContentPadding) {
                        if (enComparador) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" Comparando")
                        } else {
                            Text("Comparar")
                        }
                    }
                }
            }
        }
    }
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
