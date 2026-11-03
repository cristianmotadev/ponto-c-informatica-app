package com.ddcrcodelab.pontocinformatica.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    aoEntrar: () -> Unit,
    viewModel: LoginViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var identificador by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mostrandoReset by remember { mutableStateOf(false) }

    // Navega uma única vez quando o login confirma
    LaunchedEffect(state.sucesso) {
        if (state.sucesso) {
            viewModel.consumirSucesso()
            aoEntrar()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Ponto C. Informática", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Gestão de ordens de serviço",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = identificador,
            onValueChange = { identificador = it },
            label = { Text("Usuário ou e-mail") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        state.erro?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.entrar(identificador, senha) },
            enabled = !state.carregando,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.carregando) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Entrar")
            }
        }

        TextButton(onClick = { mostrandoReset = true }) {
            Text("Esqueci minha senha")
        }
    }

    if (mostrandoReset) {
        DialogRecuperarSenha(
            enviando = state.enviandoReset,
            erro = state.erroReset,
            enviado = state.resetEnviado,
            onDismiss = {
                mostrandoReset = false
                viewModel.consumirReset()
            },
            onConfirm = { viewModel.recuperarSenha(it) }
        )
    }
}

@Composable
private fun DialogRecuperarSenha(
    enviando: Boolean,
    erro: String?,
    enviado: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var identificador by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        title = { Text("Recuperar senha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Informe seu usuário ou e-mail. Enviaremos o link de redefinição para o e-mail cadastrado.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = identificador,
                    onValueChange = { identificador = it },
                    label = { Text("Usuário ou e-mail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (enviado) {
                    Text("E-mail de recuperação enviado. Confira a caixa de entrada.")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(identificador) },
                enabled = !enviando && !enviado
            ) {
                if (enviando) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enviar link")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !enviando) { Text("Fechar") }
        }
    )
}