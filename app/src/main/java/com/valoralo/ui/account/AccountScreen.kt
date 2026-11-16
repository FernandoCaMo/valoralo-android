package com.valoralo.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Sesion
import com.valoralo.domain.model.Usuario
import com.valoralo.ui.components.URL_PRIVACIDAD

@Composable
fun AccountScreen(
    onIniciarSesion: () -> Unit,
    onCrearCuenta: () -> Unit,
    onCompletarPerfil: () -> Unit,
    onAcercaDe: () -> Unit,
    viewModel: MiCuentaViewModel = hiltViewModel(),
) {
    val sesion by viewModel.sesion.collectAsStateWithLifecycle()
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val abrirPrivacidad = { runCatching { uriHandler.openUri(URL_PRIVACIDAD) }; Unit }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        when (val s = sesion) {
            Sesion.Cargando -> CircularProgressIndicator(Modifier.padding(32.dp).align(Alignment.CenterHorizontally))
            Sesion.SinSesion -> SinSesion(onIniciarSesion, onCrearCuenta)
            is Sesion.Iniciada -> if (s.perfil == null) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Te falta completar tu perfil para usar la cuenta.")
                    Button(onClick = onCompletarPerfil) { Text("Completar perfil") }
                }
            } else {
                ConSesion(s, s.perfil, viewModel)
            }
        }

        HorizontalDivider(Modifier.padding(top = 8.dp))
        Opcion(Icons.Outlined.Policy, "Política de privacidad", onClick = abrirPrivacidad)
        Opcion(Icons.Outlined.Info, "Acerca de", "Versión, fuente de los datos y licencia", onClick = onAcercaDe)
        if (sesion is Sesion.Iniciada) {
            val context = LocalContext.current
            Opcion(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", flecha = false) { viewModel.cerrarSesion(context) }
        }
    }

    estado.error?.let { MensajeDialogo(it, viewModel::mensajesMostrados) }
    estado.aviso?.let { MensajeDialogo(it, viewModel::mensajesMostrados) }
}

@Composable
private fun SinSesion(onIniciarSesion: () -> Unit, onCrearCuenta: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        Text("Mi cuenta", style = MaterialTheme.typography.titleLarge)
        Text(
            "Puedes leer reseñas sin cuenta. Para escribirlas, inicia sesión o crea una cuenta (a partir de 14 años).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onIniciarSesion, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sesión") }
        OutlinedButton(onClick = onCrearCuenta, modifier = Modifier.fillMaxWidth()) { Text("Crear una cuenta") }
    }
}

@Composable
private fun ConSesion(sesion: Sesion.Iniciada, perfil: Usuario, viewModel: MiCuentaViewModel) {
    var editarAlias by rememberSaveable { mutableStateOf(false) }
    var eliminar by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(perfil.alias, style = MaterialTheme.typography.headlineSmall)
        Text(
            when {
                perfil.rol == Rol.DOCENTE && perfil.verificado -> "Docente verificado"
                perfil.rol == Rol.ESTUDIANTE -> "Estudiante"
                else -> "Otro"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        sesion.correo?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Opcion(Icons.Outlined.Edit, "Cambiar alias", perfil.alias) { editarAlias = true }
    if (!(perfil.rol == Rol.DOCENTE && perfil.verificado)) {
        Opcion(Icons.Outlined.School, "Soy docente", "Verificación con la hoja de servicios. Próximamente.", habilitada = false) {}
    }
    Opcion(Icons.Outlined.DeleteForever, "Eliminar cuenta", "Se borran tu perfil y tu acceso", flecha = false, peligro = true) { eliminar = true }

    if (editarAlias) DialogoAlias(perfil.alias, viewModel) { editarAlias = false }
    if (eliminar) DialogoEliminar(sesion.conGoogle, viewModel) { eliminar = false }
}

@Composable
private fun DialogoAlias(actual: String, viewModel: MiCuentaViewModel, onCerrar: () -> Unit) {
    var alias by rememberSaveable { mutableStateOf(actual) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cambiar alias") },
        text = {
            OutlinedTextField(
                value = alias, onValueChange = { alias = it; error = null },
                label = { Text("Alias") }, singleLine = true,
                isError = error != null, supportingText = { Text(error ?: "Es el nombre que verán los demás.") },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                error = viewModel.cambiarAlias(alias)
                if (error == null) onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Composable
private fun DialogoEliminar(conGoogle: Boolean, viewModel: MiCuentaViewModel, onCerrar: () -> Unit) {
    val context = LocalContext.current
    var contrasena by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("¿Eliminar tu cuenta?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Se borrarán tu perfil y tu acceso a Valoralo. Esta acción no se puede deshacer.")
                if (conGoogle) {
                    Text("Para confirmar que eres tú, te pediremos que elijas tu cuenta de Google.")
                } else {
                    OutlinedTextField(
                        value = contrasena, onValueChange = { contrasena = it },
                        label = { Text("Tu contraseña") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (conGoogle) viewModel.eliminarConGoogle(context) else viewModel.eliminarConContrasena(contrasena)
                    onCerrar()
                },
                enabled = conGoogle || contrasena.isNotEmpty(),
            ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Composable
private fun MensajeDialogo(texto: String, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Entendido") } },
    )
}

@Composable
private fun Opcion(
    icono: ImageVector,
    titulo: String,
    detalle: String? = null,
    flecha: Boolean = true,
    peligro: Boolean = false,
    habilitada: Boolean = true,
    onClick: () -> Unit,
) {
    val color = when {
        !habilitada -> MaterialTheme.colorScheme.onSurfaceVariant
        peligro -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    ListItem(
        headlineContent = { Text(titulo, color = color) },
        supportingContent = detalle?.let { { Text(it) } },
        leadingContent = { Icon(icono, contentDescription = null, tint = color) },
        trailingContent = if (flecha && habilitada) {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        } else null,
        colors = ListItemDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickable(enabled = habilitada, onClick = onClick),
    )
}
