package com.ddcrcodelab.pontocinformatica.ui.os

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriarOsScreen(
    aoSalvar: () -> Unit,
    viewModel: CriarOsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var nome by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var defeito by remember { mutableStateOf("") }
    var prazo by remember { mutableStateOf("3") }
    var superficial by remember { mutableStateOf(false) }
    var valorEstimado by remember { mutableStateOf("") }

    LaunchedEffect(state.salvoComSucesso) {
        if (state.salvoComSucesso) aoSalvar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova Ordem de Serviço") },
                navigationIcon = {
                    IconButton(onClick = aoSalvar) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Campo(valor = nome, aoMudar = { nome = it }, rotulo = "Nome do cliente *")
            Campo(
                valor = telefone, aoMudar = { telefone = it },
                rotulo = "Telefone/WhatsApp", teclado = KeyboardType.Phone
            )
            Campo(valor = marca, aoMudar = { marca = it }, rotulo = "Marca (ex.: Samsung)")
            Campo(valor = modelo, aoMudar = { modelo = it }, rotulo = "Modelo * (ex.: Galaxy A54)")
            Campo(
                valor = defeito, aoMudar = { defeito = it },
                rotulo = "Defeito relatado *", linhas = 3
            )
            Campo(
                valor = prazo, aoMudar = { prazo = it },
                rotulo = "Prazo estimado (dias)", teclado = KeyboardType.Number
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Switch(checked = superficial, onCheckedChange = { superficial = it })
                Column {
                    Text("Orçamento superficial", style = MaterialTheme.typography.titleSmall)
                    Text("Prévia antes de receber o aparelho", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (superficial) {
                Campo(
                    valor = valorEstimado, aoMudar = { valorEstimado = it },
                    rotulo = "Valor estimado (R$, opcional)",
                    teclado = KeyboardType.Decimal
                )
            }

            state.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    viewModel.salvar(
                        clienteNome = nome,
                        clienteTelefone = telefone,
                        aparelhoMarca = marca,
                        aparelhoModelo = modelo,
                        defeitoRelatado = defeito,
                        prazoDias = prazo.toIntOrNull() ?: 0,
                        superficial = superficial,
                        valorEstimado = valorEstimado.replace(",", ".").toDoubleOrNull() ?: 0.0
                    )
                },
                enabled = !state.salvando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.salvando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salvar OS")
                }
            }
        }
    }
}

@Composable
private fun Campo(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    teclado: KeyboardType = KeyboardType.Text,
    linhas: Int = 1
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = linhas == 1,
        minLines = linhas,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth()
    )
}