package com.valoralo.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoralo.domain.model.Rol
import com.valoralo.ui.components.URL_CONDICIONES
import com.valoralo.ui.components.URL_PRIVACIDAD
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Sign-up with e-mail ([soloPerfil] = false) or completing the profile after the first
 * Google login ([soloPerfil] = true, no e-mail/password fields and a "Cancelar" that
 * deletes the just-created account).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    soloPerfil: Boolean,
    onVolver: () -> Unit,
    onTerminado: () -> Unit,
    viewModel: RegistroViewModel = hiltViewModel(),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var elegirFecha by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s.terminado) { if (s.terminado) onTerminado() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (soloPerfil) "Completa tu perfil" else "Crear una cuenta") },
                navigationIcon = {
                    if (!soloPerfil) {
                        IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (soloPerfil) {
                Text("Ya has entrado con Google. Para terminar, necesitamos unos datos.")
            } else {
                Campo(s.correo, viewModel::onCorreo, "Correo electrónico", s.errores[CampoRegistro.CORREO], KeyboardType.Email)
                Campo(
                    s.contrasena, viewModel::onContrasena, "Contraseña", s.errores[CampoRegistro.CONTRASENA],
                    KeyboardType.Password, ayuda = "Al menos 8 caracteres, con letras y números.", secreto = true,
                )
            }
            Campo(
                s.alias, viewModel::onAlias, "Alias", s.errores[CampoRegistro.ALIAS], KeyboardType.Text,
                ayuda = "Es el nombre que verán los demás en tus reseñas. No uses tu nombre completo. " +
                    "(${s.alias.length}/${com.valoralo.domain.usecase.ALIAS_MAX})",
            )

            // Birth date: only used to check the minimum age; it is not stored.
            OutlinedTextField(
                value = s.fechaNacimiento?.format(FORMATO).orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha de nacimiento") },
                trailingIcon = {
                    IconButton(onClick = { elegirFecha = true }) { Icon(Icons.Filled.CalendarMonth, contentDescription = "Elegir fecha") }
                },
                isError = s.errores[CampoRegistro.FECHA] != null,
                supportingText = {
                    Text(s.errores[CampoRegistro.FECHA] ?: "Solo para comprobar que tienes al menos 14 años. No la guardamos.")
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("¿Cómo vas a valorar?", style = MaterialTheme.typography.titleSmall)
            Column(Modifier.selectableGroup()) {
                OpcionRol(Rol.ESTUDIANTE, "Estudiante", "Estudias o has estudiado hace poco en un centro.", s.rol, viewModel::onRol)
                OpcionRol(Rol.OTRO, "Otro", "Familia, exalumnado… Si eres docente, elige esta opción y verifícate después en Mi cuenta.", s.rol, viewModel::onRol)
            }

            Row(
                Modifier.fillMaxWidth().toggleable(s.aceptaPrivacidad, role = Role.Checkbox, onValueChange = viewModel::onPrivacidad),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = s.aceptaPrivacidad, onCheckedChange = null)
                Text("He leído y acepto las condiciones de uso y la política de privacidad", Modifier.padding(start = 8.dp))
            }
            Row {
                TextButton(onClick = { runCatching { uriHandler.openUri(URL_CONDICIONES) } }) { Text("Condiciones de uso") }
                TextButton(onClick = { runCatching { uriHandler.openUri(URL_PRIVACIDAD) } }) { Text("Política de privacidad") }
            }
            s.errores[CampoRegistro.PRIVACIDAD]?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { if (soloPerfil) viewModel.completarPerfil() else viewModel.registrar() },
                enabled = !s.enviando,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (s.enviando) "Un momento…" else if (soloPerfil) "Terminar" else "Crear cuenta") }
            if (soloPerfil) {
                OutlinedButton(onClick = viewModel::cancelar, enabled = !s.enviando, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar y no crear la cuenta")
                }
            }
        }
    }

    if (elegirFecha) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = s.fechaNacimiento?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
            yearRange = 1920..LocalDate.now().year,
            initialDisplayMode = DisplayMode.Input,
        )
        DatePickerDialog(
            onDismissRequest = { elegirFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        viewModel.onFecha(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    elegirFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { elegirFecha = false }) { Text("Cancelar") } },
        ) { DatePicker(state = estado) }
    }
}

@Composable
private fun Campo(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String?,
    teclado: KeyboardType,
    ayuda: String? = null,
    secreto: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        visualTransformation = if (secreto) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OpcionRol(rol: Rol, titulo: String, descripcion: String, elegido: Rol, onElegir: (Rol) -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = rol == elegido, role = Role.RadioButton, onClick = { onElegir(rol) })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = rol == elegido, onClick = null)
        Column(Modifier.padding(start = 8.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
