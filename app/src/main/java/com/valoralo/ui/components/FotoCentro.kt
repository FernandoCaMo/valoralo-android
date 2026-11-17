package com.valoralo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Image of a centro: the illustration of its type and, on top, the facade photo once there
 * is an approved one (photos come in a later round; most centros will keep the illustration).
 */
@Composable
fun FotoCentro(fotoUrl: String?, tipo: String?, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp))) {
        IlustracionCentro(tipo, Modifier.fillMaxSize())
        if (fotoUrl != null) {
            AsyncImage(
                model = fotoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
