package com.ddcrcodelab.pontocinformatica.ui.os

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

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

    LaunchedEffect(state.salvoComSucesso) {
        if (state.salvoComSucesso) aoSalvar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Scroll: garante uso confortável em telas pequenas e
            // com o teclado de soft input aberto
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Nova Ordem de Serviço", style = MaterialTheme.typography.titleLarge)

        Campo(valor = nome, aoMudar = { nome = it }, rotulo = "Nome do cliente *")
        Campo(
            valor = telefone,
            aoMudar = { telefone = it },
            rotulo = "Telefone/WhatsApp",
            teclado = KeyboardType.Phone
        )
        Campo(valor = marca, aoMudar = { marca = it }, rotulo = "Marca (ex.: Samsung)")
        Campo(valor = modelo, aoMudar = { modelo = it }, rotulo = "Modelo * (ex.: Galaxy A54)")
        Campo(
            valor = defeito,
            aoMudar = { defeito = it },
            rotulo = "Defeito relatado *",
            linhas = 3
        )
        Campo(
            valor = prazo,
            aoMudar = { prazo = it },
            rotulo = "Prazo estimado (dias)",
            teclado = KeyboardType.Number
        )

        state.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                viewModel.salvar(
                    clienteNome = nome,
                    clienteTelefone = telefone,
                    aparelhoMarca = marca,
                    aparelhoModelo = modelo,
                    defeitoRelatado = defeito,
                    prazoDias = prazo.toIntOrNull() ?: 0
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

// Campo reutilizável: evita repetir OutlinedTextField seis vezes
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