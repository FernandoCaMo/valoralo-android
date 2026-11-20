package com.valoralo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Valoracion
import com.valoralo.domain.usecase.EstadoLike
import com.valoralo.ui.components.BotonLike
import com.valoralo.ui.components.Estrellas
import com.valoralo.ui.theme.bordeRol
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * [onDenunciar] shows the report button (other people's reviews); [etiquetaEstado] tells the
 * author that their own review is under review or hidden.
 */
@Composable
fun TarjetaResena(
    valoracion: Valoracion,
    modifier: Modifier = Modifier,
    onDenunciar: (() -> Unit)? = null,
    /** "Bloquear": stop seeing this author's reviews (others' reviews only). */
    onBloquear: (() -> Unit)? = null,
    etiquetaEstado: String? = null,
    /** What the heart shows; [onLike] null = own review (only the number). */
    like: EstadoLike = EstadoLike(dado = false, n = valoracion.nLikes),
    onLike: (() -> Unit)? = null,
) {
    OutlinedCard(modifier.fillMaxWidth(), border = bordeRol(valoracion.rolAutor)) {
        Column(Modifier.padding(16.dp)) {
            etiquetaEstado?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(valoracion.alias)
                Column(Modifier.weight(1f)) {
                    Text(valoracion.alias, style = MaterialTheme.typography.titleSmall)
                    EtiquetaRol(valoracion)
                }
                Text(
                    valoracion.fecha.format(FORMATO_FECHA),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            FilaEstrellas("Centro", valoracion.centro.nota)
            FilaEstrellas("Instalaciones", valoracion.instalaciones.nota)
            if (valoracion.profesorado.isNotEmpty()) {
                Text(
                    valoracion.profesorado.joinToString(" · ") { "${it.departamento} ${it.nota}★" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(valoracion.texto, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                BotonLike(like, onLike, Modifier.padding(end = 4.dp))
                Text(
                    if (valoracion.editada) "(editada)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (onBloquear != null) {
                    TextButton(onClick = onBloquear) {
                        Icon(Icons.Outlined.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Bloquear", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (onDenunciar != null) {
                    TextButton(onClick = onDenunciar) {
                        Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Denunciar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(alias: String) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            alias.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun EtiquetaRol(valoracion: Valoracion) {
    if (valoracion.esDocenteVerificado) {
        Row(
            Modifier
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Text(
                "Docente verificado",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    } else {
        Text(
            when (valoracion.rolAutor) {
                Rol.DOCENTE -> "Docente"
                Rol.ESTUDIANTE -> "Estudiante"
                Rol.OTRO -> "Otro"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilaEstrellas(etiqueta: String, nota: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Estrellas(nota)
    }
}
