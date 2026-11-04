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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
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
import com.ddcrcodelab.pontocinformatica.data.repository.PedidosNotificacoesRepository
import com.ddcrcodelab.pontocinformatica.ui.theme.ThemeMode
import com.ddcrcodelab.pontocinformatica.util.SessaoManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import com.ddcrcodelab.pontocinformatica.ui.components.BannerAtualizacao
import com.ddcrcodelab.pontocinformatica.util.VerificadorAtualizacao

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

@Composable
fun ListaOsScreen(
    aoCriarOs: () -> Unit,
    aoAbrirDetalhe: (String) -> Unit,
    aoAbrirEquipe: () -> Unit,
    aoAbrirPerfil: () -> Unit,
    aoAbrirPedidos: () -> Unit,
    aoAbrirNotificacoes: () -> Unit,
    aoSair: () -> Unit,
    viewModel: ListaOsViewModel = koinViewModel(),
    sessao: SessaoManager = koinInject(),
    repoNotificacoes: PedidosNotificacoesRepository = koinInject(),
    verificador: VerificadorAtualizacao = koinInject()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Identidade da gaveta + badge do sino
    val usuario by sessao.usuario.collectAsStateWithLifecycle()
    val perfil by sessao.perfil.collectAsStateWithLifecycle()
    val ehDono by sessao.ehDono.collectAsStateWithLifecycle()

    val uid = usuario?.id
    val naoLidas by remember(uid) {
        if (uid == null) flowOf(0) else repoNotificacoes.observarContagemNaoLidas(uid)
    }.collectAsStateWithLifecycle(initialValue = 0)

    // FASE 14: cheque de atualização (rate-limit de 1x/dia dentro do verificador)
    val atualizacao by verificador.atualizacao.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { verificador.verificarSePrecisa() }

    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val escopoDrawer = rememberCoroutineScope()

    // Tema
    val context = LocalContext.current
    val themePrefs = remember(context) { ThemePreferences(context) }
    val themeMode by themePrefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val escopo = rememberCoroutineScope()

    fun fecharGavetaE(ir: () -> Unit) {
        escopoDrawer.launch { drawerState.close() }
        ir()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))

                // Cabeçalho: nome completo + perfil de permissão
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(usuario?.nome.orEmpty(), style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (ehDono) "Perfil: Dono" else "Perfil: ${perfil?.nome ?: "—"}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("Meu perfil") },
                    selected = false,
                    onClick = { fecharGavetaE(aoAbrirPerfil) }
                )
                NavigationDrawerItem(
                    label = { Text("📦 Pedidos de peça") },
                    selected = false,
                    onClick = { fecharGavetaE(aoAbrirPedidos) }
                )
                NavigationDrawerItem(
                    label = { Text("🔔 Notificações") },
                    selected = false,
                    onClick = { fecharGavetaE(aoAbrirNotificacoes) }
                )
                if (ehDono) {
                    NavigationDrawerItem(
                        label = { Text("Equipe") },
                        selected = false,
                        onClick = { fecharGavetaE(aoAbrirEquipe) }
                    )
                }
                NavigationDrawerItem(
                    label = { Text("Sair") },
                    selected = false,
                    onClick = aoSair
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { escopoDrawer.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir menu")
                    }

                    Text(
                        "Ordens de Serviço",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    // FASE 13: sino com badge de não lidas
                    IconButton(onClick = aoAbrirNotificacoes) {
                        BadgedBox(
                            badge = {
                                if (naoLidas > 0) {
                                    Badge { Text("$naoLidas") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificações")
                        }
                    }

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
                                ThemeMode.SYSTEM -> "Tema: sistema"
                                ThemeMode.LIGHT -> "Tema: diurno"
                                ThemeMode.DARK -> "Tema: noturno"
                            }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!state.bloqueado) {
                    FloatingActionButton(onClick = aoCriarOs) {
                        Icon(Icons.Default.Add, contentDescription = "Criar nova OS")
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // FASE 14: banner de atualização acima de tudo
                atualizacao?.let {
                    BannerAtualizacao(
                        atualizacao = it,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
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
                            CardOs(ordem = ordem, aoClicar = { aoAbrirDetalhe(ordem.id) })
                        }
                    }
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