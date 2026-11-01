package com.ddcrcodelab.pontocinformatica.ui.os

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ddcrcodelab.pontocinformatica.data.model.FotoOs
import com.ddcrcodelab.pontocinformatica.data.model.OrdemServico
import com.ddcrcodelab.pontocinformatica.data.model.StatusOS
import com.ddcrcodelab.pontocinformatica.util.WhatsAppUtil
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetalheOsScreen(
    osId: String,
    aoVoltar: () -> Unit,
    viewModel: DetalheOsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // FASE 8: hospeda o Snackbar com a ação "Avisar cliente"
    val snackbarHostState = remember { SnackbarHostState() }

    val seletorFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.adicionarFoto(uri)
    }

    var fotoParaRemover by remember { mutableStateOf<FotoOs?>(null) }

    /**
     * FASE 8: abre o WhatsApp na conversa do cliente com a mensagem pronta.
     * Sem telefone válido, cai no compartilhamento genérico (permite copiar).
     * Sem app capaz de abrir, Toast amigável em vez de crash.
     */
    fun abrirWhatsApp(ordem: OrdemServico) {
        val link = viewModel.montarLinkCliente(ordem.tokenPublico)
        val mensagem = WhatsAppUtil.mensagemStatus(ordem, link)
        val telefone = WhatsAppUtil.normalizarTelefone(ordem.clienteTelefone)

        try {
            if (telefone != null) {
                // wa.me: abre a conversa CERTA com o texto preenchido
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(WhatsAppUtil.linkWaMe(telefone, mensagem)))
                )
            } else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, mensagem)
                }
                context.startActivity(Intent.createChooser(intent, "Enviar aviso"))
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Nenhum app disponível para enviar", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(osId) { viewModel.carregar(osId) }

    LaunchedEffect(state.ordem?.id) {
        val ordem = state.ordem
        if (ordem != null && ordem.tokenPublico.isBlank()) {
            viewModel.gerarToken(ordem.id)
        }
    }

    // FASE 8: após mudar o status, oferece o aviso ao cliente em 1 toque
    LaunchedEffect(state.notificarStatus) {
        val status = state.notificarStatus ?: return@LaunchedEffect
        val resultado = snackbarHostState.showSnackbar(
            message = "Status alterado para: ${status.rotulo}",
            actionLabel = "Avisar cliente",
            withDismissAction = true,
            duration = SnackbarDuration.Short
        )
        if (resultado == SnackbarResult.ActionPerformed) {
            state.ordem?.let { abrirWhatsApp(it) }
        }
        viewModel.consumirNotificacao()
    }

    // FASE 7: quando o PDF fica pronto, abre o share sheet
    LaunchedEffect(state.pdfGerado) {
        val arquivo = state.pdfGerado ?: return@LaunchedEffect
        val ordem = state.ordem ?: return@LaunchedEffect

        val autoridade = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, autoridade, arquivo)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Orçamento OS ${ordem.id.take(8)} — ${ordem.clienteNome}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Segue o orçamento da OS ${ordem.id.take(8)}.\n" +
                        "Acompanhe em: https://pontocinformatica.web.app/os/${ordem.tokenPublico}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar orçamento"))
        viewModel.pdfFoiCompartilhado()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = aoVoltar) { Text("← Voltar à lista") }

            when {
                state.carregando -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                state.erro != null -> Text(
                    text = state.erro!!,
                    color = MaterialTheme.colorScheme.error
                )

                state.ordem != null -> {
                    val ordem = state.ordem!!
                    val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

                    // Cabeçalho
                    Text(ordem.clienteNome, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${ordem.aparelhoMarca} ${ordem.aparelhoModelo}".trim(),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Entrada: ${fmt.format(Date(ordem.dataEntrada))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    ordem.defeitoRelatado.takeIf { it.isNotBlank() }?.let {
                        Text("Defeito: $it", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Status
                    Text("Status do reparo", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusOS.entries.forEach { status ->
                            FilterChip(
                                selected = ordem.status == status,
                                onClick = { viewModel.mudarStatus(ordem.id, status) },
                                label = { Text(status.rotulo) },
                                enabled = !state.salvando
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Diagnóstico + orçamento
                    var diagnosticoEdit by remember(ordem.id) {
                        mutableStateOf(ordem.diagnostico)
                    }
                    var valorEdit by remember(ordem.id) {
                        mutableStateOf(
                            ordem.valorOrcado
                                .takeIf { it > 0 }
                                ?.toString()
                                ?.removeSuffix(".0")
                                .orEmpty()
                        )
                    }

                    Text("Diagnóstico e orçamento", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = diagnosticoEdit,
                        onValueChange = { diagnosticoEdit = it },
                        label = { Text("Diagnóstico técnico") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = valorEdit,
                        onValueChange = { valorEdit = it },
                        label = { Text("Valor orçado (R$)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.salvarOrcamento(ordem.id, valorEdit, diagnosticoEdit)
                        },
                        enabled = !state.salvando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salvar diagnóstico e orçamento")
                    }

                    Spacer(Modifier.height(8.dp))

                    // PDF do orçamento
                    Button(
                        onClick = { viewModel.gerarOrcamentoPdf() },
                        enabled = !state.gerandoPdf && !state.salvando && ordem.valorOrcado > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.gerandoPdf) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Gerando PDF...")
                            }
                        } else {
                            Text("📄 Gerar PDF do orçamento")
                        }
                    }
                    if (ordem.valorOrcado == 0.0) {
                        Text(
                            "Preencha o valor orçado para liberar o PDF.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Fotos
                    Text("Fotos do aparelho", style = MaterialTheme.typography.titleMedium)

                    if (state.enviandoFoto) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Enviando foto...", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (ordem.fotos.isEmpty() && !state.enviandoFoto) {
                        Text(
                            "Nenhuma foto ainda. Registre o estado do aparelho na entrada.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ordem.fotos, key = { it.url }) { foto ->
                                AsyncImage(
                                    model = foto.url,
                                    contentDescription = "Foto do aparelho",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = { },
                                            onLongClick = { fotoParaRemover = foto }
                                        )
                                )
                            }
                        }
                        Text(
                            "Toque e segure numa foto para removê-la",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            seletorFoto.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        enabled = !state.enviandoFoto,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Adicionar foto")
                    }

                    fotoParaRemover?.let { foto ->
                        AlertDialog(
                            onDismissRequest = { fotoParaRemover = null },
                            title = { Text("Remover foto?") },
                            text = {
                                Text("A foto será apagada do Storage e do site do cliente.")
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.removerFoto(foto)
                                        fotoParaRemover = null
                                    }
                                ) { Text("Remover") }
                            },
                            dismissButton = {
                                TextButton(onClick = { fotoParaRemover = null }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Link público
                    Button(
                        onClick = {
                            val link = viewModel.montarLinkCliente(ordem.tokenPublico)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Acompanhe sua OS")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Olá, ${ordem.clienteNome}! Acompanhe o andamento da sua OS:\n$link"
                                )
                            }
                            context.startActivity(
                                Intent.createChooser(intent, "Compartilhar link")
                            )
                        },
                        enabled = ordem.tokenPublico.isNotBlank() && !state.salvando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Compartilhar link com cliente")
                    }

                    // FASE 8: aviso manual via WhatsApp (reenvio quando quiser)
                    OutlinedButton(
                        onClick = { abrirWhatsApp(ordem) },
                        enabled = !state.salvando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💬 Enviar status no WhatsApp")
                    }

                    // Situação do orçamento
                    when (ordem.orcamentoAprovado) {
                        null -> OutlinedButton(
                            onClick = { viewModel.aprovarOrcamento(ordem.id) },
                            enabled = !state.salvando,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Marcar orçamento como aprovado")
                        }

                        true -> Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Orçamento aprovado em ${fmt.format(Date(ordem.orcamentoDecididoEm))}",
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        false -> Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Orçamento recusado em ${fmt.format(Date(ordem.orcamentoDecididoEm))}",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Histórico
                    Text("Histórico", style = MaterialTheme.typography.titleMedium)
                    ordem.historico.sortedByDescending { it.timestamp }.forEach { entrada ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(entrada.mensagem, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    fmt.format(Date(entrada.timestamp)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}