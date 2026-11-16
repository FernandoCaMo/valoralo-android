package com.valoralo.ui.verify

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoralo.domain.model.EstadoSolicitud
import com.valoralo.domain.model.MiVerificacion
import java.time.format.DateTimeFormatter

private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificarDocenteScreen(
    onVolver: () -> Unit,
    viewModel: VerificarDocenteViewModel = hiltViewModel(),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val elegirPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onPdf(datosDelPdf(context, it)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verificarme como docente") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (s.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (s.yaVerificado) {
                Aviso("Ya estás verificado como docente. Tus reseñas muestran la insignia \"Docente verificado\".", icono = true)
                return@Column
            }
            s.solicitud?.let { EstadoSolicitudCard(it) }
            if (s.solicitud?.estado == EstadoSolicitud.PENDIENTE) return@Column

            Explicacion()
            val f = s.formulario
            OutlinedTextField(
                value = f.nombre, onValueChange = viewModel::onNombre,
                label = { Text("Nombre y apellidos") }, singleLine = true,
                isError = f.errorNombre != null,
                supportingText = { Text(f.errorNombre ?: "Tal como aparecen en el documento. Nunca se muestran a nadie.") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = f.csv, onValueChange = viewModel::onCsv,
                label = { Text("Código seguro de verificación (CSV)") }, singleLine = true,
                isError = f.errorCsv != null,
                supportingText = { Text(f.errorCsv ?: "Aparece en el pie del documento, junto a la dirección de verificación de la Junta.") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            if (f.pdf == null) {
                OutlinedButton(onClick = { elegirPdf.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null)
                    Text(" Adjuntar el PDF (opcional)")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null)
                    Text(f.pdf.nombre, Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { viewModel.onPdf(null) }) { Icon(Icons.Filled.Close, contentDescription = "Quitar el PDF") }
                }
            }
            f.errorPdf?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            f.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::enviar, enabled = !f.enviando, modifier = Modifier.fillMaxWidth()) {
                Text(if (f.enviando) "Enviando…" else "Enviar solicitud")
            }
        }
    }
}

@Composable
private fun Explicacion() {
    Text("Qué necesitas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    Text(
        "La hoja de servicios del Portal del docente de la Junta de Andalucía (o tu nombramiento, toma de posesión " +
            "o adjudicación de destino), con código seguro de verificación (CSV). Tiene que estar expedida " +
            "después de crear tu cuenta en Valoralo: descárgala de nuevo si es antigua.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Text("Qué hacemos con tus datos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    Text(
        "Un administrador comprueba el documento en la web de la Junta. Tu nombre y el CSV solo se guardan mientras se " +
            "revisa la solicitud; después se borran y solo queda una huella cifrada del CSV para que no se reutilice. " +
            "El PDF también se borra. Tu nombre nunca se muestra.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EstadoSolicitudCard(v: MiVerificacion) {
    val fecha = v.fechaSolicitud?.format(FECHA)?.let { " el $it" }.orEmpty()
    val texto = when (v.estado) {
        EstadoSolicitud.PENDIENTE -> "Solicitud enviada$fecha. La revisaremos pronto; si en 30 días no se ha revisado, caducará y podrás enviar otra."
        EstadoSolicitud.APROBADA -> "Solicitud aprobada. En unos segundos verás tu cuenta como docente verificado."
        EstadoSolicitud.RECHAZADA -> "Solicitud rechazada: ${v.motivoRechazo ?: "sin motivo"}. Puedes enviar otra con un documento nuevo."
        EstadoSolicitud.CADUCADA -> "Tu solicitud caducó sin revisarse. Puedes enviar otra."
    }
    Aviso(texto, icono = v.estado == EstadoSolicitud.APROBADA)
}

@Composable
private fun Aviso(texto: String, icono: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icono) Icon(Icons.Outlined.Verified, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(texto, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

private fun datosDelPdf(context: Context, uri: Uri): PdfElegido {
    var nombre = "documento.pdf"
    var bytes: Long? = null
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            c.getString(0)?.let { nombre = it }
            if (!c.isNull(1)) bytes = c.getLong(1)
        }
    }
    return PdfElegido(uri, nombre, bytes)
}
