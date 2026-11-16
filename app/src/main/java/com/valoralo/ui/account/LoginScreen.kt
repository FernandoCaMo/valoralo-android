package com.valoralo.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onVolver: () -> Unit,
    onCrearCuenta: () -> Unit,
    onTerminado: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var recuperar by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s.terminado) { if (s.terminado) onTerminado() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Iniciar sesión") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
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
            OutlinedButton(
                onClick = { viewModel.entrarConGoogle(context) },
                enabled = !s.enviando,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continuar con Google") }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f))
                Text("  o con tu correo  ", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(Modifier.weight(1f))
            }

            OutlinedTextField(
                value = s.correo, onValueChange = viewModel::onCorreo,
                label = { Text("Correo electrónico") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.contrasena, onValueChange = viewModel::onContrasena,
                label = { Text("Contraseña") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::entrar, enabled = !s.enviando, modifier = Modifier.fillMaxWidth()) {
                Text(if (s.enviando) "Entrando…" else "Entrar")
            }
            TextButton(onClick = { recuperar = true }) { Text("¿Has olvidado la contraseña?") }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Text("¿Aún no tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onCrearCuenta, modifier = Modifier.fillMaxWidth()) { Text("Crear una cuenta") }
        }
    }

    if (recuperar) {
        var correo by rememberSaveable { mutableStateOf(s.correo) }
        AlertDialog(
            onDismissRequest = { recuperar = false },
            title = { Text("Recuperar la contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Te enviaremos un enlace para crear una contraseña nueva.")
                    OutlinedTextField(
                        value = correo, onValueChange = { correo = it },
                        label = { Text("Correo electrónico") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.recuperarContrasena(correo); recuperar = false }) { Text("Enviar") }
            },
            dismissButton = { TextButton(onClick = { recuperar = false }) { Text("Cancelar") } },
        )
    }

    s.aviso?.let { aviso ->
        AlertDialog(
            onDismissRequest = viewModel::avisoMostrado,
            text = { Text(aviso) },
            confirmButton = { TextButton(onClick = viewModel::avisoMostrado) { Text("Entendido") } },
        )
    }
}
