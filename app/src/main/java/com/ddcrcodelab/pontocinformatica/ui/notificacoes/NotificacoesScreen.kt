package com.ddcrcodelab.pontocinformatica.ui.notificacoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ddcrcodelab.pontocinformatica.data.model.Notificacao
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val fmtData = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))

@Composable
fun NotificacoesScreen(
    aoVoltar: () -> Unit,
    viewModel: NotificacoesViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = aoVoltar) { Text("← Voltar") }
                Text(
                    "Notificações",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                if (state.notificacoes.any { !it.lida }) {
                    TextButton(onClick = { viewModel.marcarTodasLidas() }) {
                        Text("Marcar todas")
                    }
                }
            }

            state.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (state.carregando) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (state.notificacoes.isEmpty()) {
                Text("Nenhuma notificação por aqui. 🔕", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.notificacoes, key = { it.id }) { notificacao ->
                        CardNotificacao(
                            notificacao = notificacao,
                            aoClicar = { viewModel.marcarLida(notificacao) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardNotificacao(notificacao: Notificacao, aoClicar: () -> Unit) {
    Card(
        onClick = aoClicar,
        colors = CardDefaults.cardColors(
            containerColor = if (notificacao.lida) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.primaryContainer
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
                Text(notificacao.titulo, style = MaterialTheme.typography.titleSmall)
                Text(
                    fmtData.format(Date(notificacao.criadaEm)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text(notificacao.mensagem, style = MaterialTheme.typography.bodyMedium)
            if (!notificacao.lida) {
                Text("Toque para marcar como lida", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}