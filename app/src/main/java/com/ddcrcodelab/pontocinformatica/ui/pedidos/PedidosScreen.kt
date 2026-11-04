package com.ddcrcodelab.pontocinformatica.ui.pedidos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ddcrcodelab.pontocinformatica.data.model.PedidoPeca
import com.ddcrcodelab.pontocinformatica.data.model.StatusPedido
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val fmtData = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

@Composable
fun PedidosScreen(
    aoVoltar: () -> Unit,
    viewModel: PedidosViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ehDono by viewModel.ehDono.collectAsStateWithLifecycle()

    var mostrandoNovo by remember { mutableStateOf(false) }
    var respondendo by remember { mutableStateOf<Pair<Boolean, PedidoPeca>?>(null) }
    var cancelando by remember { mutableStateOf<PedidoPeca?>(null) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = aoVoltar) { Text("← Voltar") }
            Text("Pedidos de peça", style = MaterialTheme.typography.headlineSmall)

            // Funcionário pede; dono responde (os botões mudam por papel)
            if (!ehDono) {
                Button(
                    onClick = { mostrandoNovo = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("➕ Novo pedido de peça")
                }
            }

            state.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (state.carregando) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (state.pedidos.isEmpty()) {
                Text(
                    text = if (ehDono) "Nenhum pedido da equipe até agora."
                    else "Você ainda não pediu nenhuma peça.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.pedidos, key = { it.id }) { pedido ->
                        CardPedido(
                            pedido = pedido,
                            ehDono = ehDono,
                            onAprovar = { respondendo = true to pedido },
                            onRecusar = { respondendo = false to pedido },
                            onCancelar = { cancelando = pedido }
                        )
                    }
                }
            }
        }
    }

    if (mostrandoNovo) {
        DialogNovoPedido(
            ocupado = state.ocupado,
            erro = state.erroDialog,
            onDismiss = { mostrandoNovo = false; viewModel.limparErroDialog() },
            onConfirm = { marca, modelo, descricao, qtd, osRef ->
                viewModel.criarPedido(marca, modelo, descricao, qtd, osRef)
                mostrandoNovo = false
            }
        )
    }

    respondendo?.let { (aprovado, pedido) ->
        DialogResponder(
            pedido = pedido,
            aprovado = aprovado,
            ocupado = state.ocupado,
            erro = state.erroDialog,
            onDismiss = { respondendo = null; viewModel.limparErroDialog() },
            onConfirm = { previsao, mensagem ->
                viewModel.responder(pedido, aprovado, previsao, mensagem)
                respondendo = null
            }
        )
    }

    cancelando?.let { pedido ->
        AlertDialog(
            onDismissRequest = { cancelando = null },
            title = { Text("Cancelar pedido?") },
            text = { Text("O pedido de ${pedido.marca} ${pedido.modelo} será cancelado e o dono avisado.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelar(pedido)
                        cancelando = null
                    }
                ) { Text("Cancelar pedido") }
            },
            dismissButton = {
                TextButton(onClick = { cancelando = null }) { Text("Manter") }
            }
        )
    }
}

@Composable
private fun CardPedido(
    pedido: PedidoPeca,
    ehDono: Boolean,
    onAprovar: () -> Unit,
    onRecusar: () -> Unit,
    onCancelar: () -> Unit
) {
    val pendente = pedido.status == StatusPedido.PENDENTE

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (pendente) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${pedido.quantidade}x ${pedido.marca} ${pedido.modelo}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    StatusPedido.rotulo(pedido.status),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (pedido.descricao.isNotBlank()) {
                Text(pedido.descricao, style = MaterialTheme.typography.bodyMedium)
            }
            if (pedido.osReferencia.isNotBlank()) {
                Text("OS/cliente: ${pedido.osReferencia}", style = MaterialTheme.typography.bodySmall)
            }

            // Dono vê QUEM pediu (nome completo); funcionário vê quando pediu
            if (ehDono) {
                Text("Pedido por ${pedido.solicitanteNome}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Em ${fmtData.format(Date(pedido.criadaEm))}", style = MaterialTheme.typography.bodySmall)

            // Resposta do dono
            if (pedido.status == StatusPedido.APROVADO || pedido.status == StatusPedido.RECUSADO) {
                if (pedido.previsao.isNotBlank()) {
                    Text("Previsão: ${pedido.previsao}", style = MaterialTheme.typography.bodyMedium)
                }
                if (pedido.mensagemResposta.isNotBlank()) {
                    Text(
                        if (pedido.status == StatusPedido.RECUSADO) "Motivo: ${pedido.mensagemResposta}"
                        else "Complemento: ${pedido.mensagemResposta}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ehDono && pendente) {
                    Button(onClick = onAprovar) { Text("Aprovar") }
                    OutlinedButton(onClick = onRecusar) { Text("Recusar") }
                }
                if (!ehDono && pendente) {
                    OutlinedButton(onClick = onCancelar) { Text("Cancelar pedido") }
                }
            }
        }
    }
}

@Composable
private fun DialogNovoPedido(
    ocupado: Boolean,
    erro: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("1") }
    var osReferencia by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!ocupado) onDismiss() },
        title = { Text("Novo pedido de peça") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = marca, onValueChange = { marca = it },
                    label = { Text("Marca") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = modelo, onValueChange = { modelo = it },
                    label = { Text("Modelo") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descricao, onValueChange = { descricao = it },
                    label = { Text("Código / descrição (flex, conectores...)") },
                    minLines = 2, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantidade, onValueChange = { quantidade = it },
                    label = { Text("Quantidade") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = osReferencia, onValueChange = { osReferencia = it },
                    label = { Text("OS / cliente (opcional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(marca, modelo, descricao, quantidade, osReferencia) },
                enabled = !ocupado
            ) { Text("Enviar pedido") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !ocupado) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogResponder(
    pedido: PedidoPeca,
    aprovado: Boolean,
    ocupado: Boolean,
    erro: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var previsao by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!ocupado) onDismiss() },
        title = { Text(if (aprovado) "Aprovar pedido" else "Recusar pedido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${pedido.quantidade}x ${pedido.marca} ${pedido.modelo}\nPedido por ${pedido.solicitanteNome}",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = previsao, onValueChange = { previsao = it },
                    label = {
                        Text(
                            if (aprovado) "Previsão de chegada (ex.: quinta, 12/06)"
                            else "Possível previsão alternativa (opcional)"
                        )
                    },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mensagem, onValueChange = { mensagem = it },
                    label = {
                        Text(
                            if (aprovado) "Mensagem complementar (opcional)"
                            else "Motivo da recusa (obrigatório)"
                        )
                    },
                    minLines = 2, modifier = Modifier.fillMaxWidth()
                )
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(previsao, mensagem) },
                enabled = !ocupado
            ) { Text(if (aprovado) "Aprovar" else "Recusar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !ocupado) { Text("Voltar") }
        }
    )
}