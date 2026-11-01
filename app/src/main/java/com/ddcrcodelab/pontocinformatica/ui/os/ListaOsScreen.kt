package com.ddcrcodelab.pontocinformatica.ui.os

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ddcrcodelab.pontocinformatica.data.ThemePreferences
import com.ddcrcodelab.pontocinformatica.data.model.OrdemServico
import com.ddcrcodelab.pontocinformatica.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.ExperimentalMaterial3Api

// SimpleDateFormat: java.time exigiria desugaring no minSdk 24
private val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

fun dataCurta(millis: Long): String =
    if (millis > 0) formatoData.format(Date(millis)) else "—"

/** Texto de "quanto tempo falta", arredondando para cima em dias. */
fun diasRestantes(previsao: Long): String {
    if (previsao <= 0L) return ""
    val dias = ceil((previsao - System.currentTimeMillis()) / 86_400_000.0).toInt()
    return when {
        dias > 1 -> "Faltam $dias dias"
        dias == 1 -> "Falta 1 dia"
        else -> "Prazo vencido"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaOsScreen(
    aoCriarOs: () -> Unit,
    aoAbrirDetalhe: (String) -> Unit,
    aoAbrirEquipe: () -> Unit,
    aoSair: () -> Unit,
    viewModel: ListaOsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Tema: Context lido no escopo composable; remember cacheia a instância
    val context = LocalContext.current
    val themePrefs = remember(context) { ThemePreferences(context) }
    val themeMode by themePrefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val escopo = rememberCoroutineScope()

    Scaffold(
        topBar = {
            // TopAppBar já reserva o espaço da barra de status (edge-to-edge):
            // o Row customizado anterior desenhava o título sob o relógio/notificações
            TopAppBar(
                title = {
                    Text(
                        text = "Ordens de Serviço",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    // Fase 11: gestão de equipe aparece só para o dono
                    if (state.ehDono) {
                        TextButton(onClick = aoAbrirEquipe) { Text("👥 Equipe") }
                    }

                    // Ciclo de tema com rótulo curto para caber em telas estreitas
                    TextButton(onClick = {
                        val proximo = when (themeMode) {
                            ThemeMode.SYSTEM -> ThemeMode.LIGHT
                            ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.SYSTEM
                        }
                        escopo.launch { themePrefs.setThemeMode(proximo) }
                    }) {
                        Text(
                            when (themeMode) {
                                ThemeMode.SYSTEM -> "🌓 Sistema"
                                ThemeMode.LIGHT -> "☀️ Claro"
                                ThemeMode.DARK -> "🌙 Escuro"
                            }
                        )
                    }

                    // Fase 10: sair da conta
                    IconButton(onClick = aoSair) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sair da conta"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // Sem FAB para conta bloqueada
            if (!state.bloqueado) {
                FloatingActionButton(onClick = aoCriarOs) {
                    Icon(Icons.Default.Add, contentDescription = "Criar nova OS")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // FASE 9: conta desativada → tela de bloqueio com saída
                state.bloqueado -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Conta desativada.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Fale com o responsável pela loja para reativar seu acesso.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = aoSair) { Text("Sair") }
                }

                state.carregando -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                state.erro != null -> Text(
                    text = state.erro!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )

                state.ordens.isEmpty() -> Text(
                    text = "Nenhuma OS ainda.\nToque em + para criar a primeira.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.ordens, key = { it.id }) { ordem ->
                        CardOs(
                            ordem = ordem,
                            aoClicar = { aoAbrirDetalhe(ordem.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardOs(ordem: OrdemServico, aoClicar: () -> Unit) {
    Card(
        onClick = aoClicar,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(ordem.clienteNome, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${ordem.aparelhoMarca} ${ordem.aparelhoModelo}".trim(),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = ordem.status.rotulo,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
                val restante = diasRestantes(ordem.previsaoEntrega)
                if (restante.isNotEmpty()) {
                    Text(restante, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(
                text = "Previsto para ${dataCurta(ordem.previsaoEntrega)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}