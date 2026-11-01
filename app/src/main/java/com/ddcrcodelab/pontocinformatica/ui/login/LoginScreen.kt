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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ddcrcodelab.pontocinformatica.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    aoEntrar: () -> Unit,
    viewModel: LoginViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var identificador by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mostrarRecuperacao by remember { mutableStateOf(false) }

    LaunchedEffect(state.logado) {
        if (state.logado) aoEntrar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = identificador,
            onValueChange = { identificador = it },
            label = { Text("Usuário ou e-mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

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
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.entrar(identificador, senha) },
            enabled = !state.carregando,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.carregando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Entrar")
            }
        }

        TextButton(onClick = { mostrarRecuperacao = true }) {
            Text("Esqueci minha senha")
        }
    }

    if (mostrarRecuperacao) {
        DialogRecuperacao(
            recuperando = state.recuperando,
            mensagem = state.mensagemRecuperacao,
            onDismiss = {
                mostrarRecuperacao = false
                viewModel.limparMensagemRecuperacao()
            },
            onConfirm = { viewModel.recuperarSenha(it) }
        )
    }
}

@Composable
private fun DialogRecuperacao(
    recuperando: Boolean,
    mensagem: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var identificador by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!recuperando) onDismiss() },
        title = { Text("Recuperar senha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Informe seu usuário ou e-mail cadastrado.")
                OutlinedTextField(
                    value = identificador,
                    onValueChange = { identificador = it },
                    label = { Text("Usuário ou e-mail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                mensagem?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(identificador) },
                enabled = !recuperando && mensagem == null
            ) {
                if (recuperando) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enviar link")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !recuperando) { Text("Fechar") }
        }
    )
}