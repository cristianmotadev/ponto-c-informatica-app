package com.ddcrcodelab.pontocinformatica.ui.os

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddcrcodelab.pontocinformatica.data.model.FotoOs
import com.ddcrcodelab.pontocinformatica.data.model.HistoricoEntrada
import com.ddcrcodelab.pontocinformatica.data.model.OrdemServico
import com.ddcrcodelab.pontocinformatica.data.model.StatusOS
import com.ddcrcodelab.pontocinformatica.data.repository.OsRepository
import com.ddcrcodelab.pontocinformatica.util.GeradorPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

data class DetalheOsUiState(
    val ordem: OrdemServico? = null,
    val carregando: Boolean = true,
    val erro: String? = null,
    val salvando: Boolean = false,
    val enviandoFoto: Boolean = false,
    val gerandoPdf: Boolean = false,
    val pdfGerado: File? = null,
    // FASE 8: status recém-alterado, aguardando decisão de avisar o cliente
    val notificarStatus: StatusOS? = null
)

class DetalheOsViewModel(
    private val repository: OsRepository,
    private val context: Context,
    private val geradorPdf: GeradorPdf
) : ViewModel() {

    private val baseUrl = "https://pontocinformatica.web.app"

    private val _uiState = MutableStateFlow(DetalheOsUiState())
    val uiState: StateFlow<DetalheOsUiState> = _uiState.asStateFlow()

    fun carregar(osId: String) {
        viewModelScope.launch {
            repository.observarOrdem(osId)
                .collect { ordem ->
                    _uiState.value = _uiState.value.copy(
                        ordem = ordem,
                        carregando = false,
                        erro = if (ordem == null) "OS não encontrada." else null
                    )
                }
        }
    }

    /** Muda o status, registra no histórico e sinaliza para o Snackbar da Fase 8. */
    fun mudarStatus(osId: String, novo: StatusOS, observacao: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(salvando = true)
            try {
                val entrada = HistoricoEntrada(
                    mensagem = buildString {
                        append("Status → ${novo.rotulo}")
                        if (observacao.isNotBlank()) append(" · $observacao")
                    },
                    timestamp = System.currentTimeMillis()
                )
                repository.atualizarStatus(osId, novo, entrada)
                _uiState.value = _uiState.value.copy(
                    salvando = false,
                    notificarStatus = novo   // FASE 8: dispara o aviso
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    salvando = false,
                    erro = "Falha ao atualizar: ${e.message}"
                )
            }
        }
    }

    /** FASE 8: limpa o sinal depois que o Snackbar foi tratado pela tela. */
    fun consumirNotificacao() {
        _uiState.value = _uiState.value.copy(notificarStatus = null)
    }

    fun gerarToken(osId: String) {
        viewModelScope.launch {
            try {
                repository.gerarTokenPublico(osId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    erro = "Falha ao gerar link: ${e.message}"
                )
            }
        }
    }

    fun montarLinkCliente(token: String): String = "$baseUrl/os/$token"

    fun aprovarOrcamento(osId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(salvando = true)
            try {
                repository.decidirOrcamento(osId, aprovado = true)
                _uiState.value = _uiState.value.copy(salvando = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    salvando = false,
                    erro = "Falha ao aprovar: ${e.message}"
                )
            }
        }
    }

    fun salvarOrcamento(osId: String, valorTexto: String, diagnostico: String) {
        val valor = valorTexto
            .trim()
            .replace(",", ".")
            .toDoubleOrNull()
            ?: 0.0

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(salvando = true)
            try {
                repository.atualizarOrcamento(osId, valor, diagnostico.trim())
                _uiState.value = _uiState.value.copy(salvando = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    salvando = false,
                    erro = "Falha ao salvar orçamento: ${e.message}"
                )
            }
        }
    }

    fun adicionarFoto(uri: Uri) {
        val osId = _uiState.value.ordem?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(enviandoFoto = true)
            try {
                val bytes = comprimirImagem(uri)
                repository.adicionarFoto(osId, bytes)
                _uiState.value = _uiState.value.copy(enviandoFoto = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    enviandoFoto = false,
                    erro = "Falha ao enviar foto: ${e.message}"
                )
            }
        }
    }

    fun removerFoto(foto: FotoOs) {
        val osId = _uiState.value.ordem?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(salvando = true)
            try {
                repository.removerFoto(osId, foto)
                _uiState.value = _uiState.value.copy(salvando = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    salvando = false,
                    erro = "Falha ao remover foto: ${e.message}"
                )
            }
        }
    }

    fun gerarOrcamentoPdf() {
        val ordem = _uiState.value.ordem ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(gerandoPdf = true)
            try {
                val arquivo = geradorPdf.gerar(ordem)
                _uiState.value = _uiState.value.copy(
                    gerandoPdf = false,
                    pdfGerado = arquivo
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    gerandoPdf = false,
                    erro = "Falha ao gerar PDF: ${e.message}"
                )
            }
        }
    }

    fun pdfFoiCompartilhado() {
        _uiState.value = _uiState.value.copy(pdfGerado = null)
    }

    private suspend fun comprimirImagem(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Não foi possível abrir a imagem")
        val original = stream.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("Não foi possível decodificar a imagem")

        val maxLado = 1280
        val escala = minOf(1f, maxLado.toFloat() / maxOf(original.width, original.height))
        val bitmap = if (escala < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * escala).toInt(),
                (original.height * escala).toInt(),
                true
            )
        } else {
            original
        }

        val saida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, saida)
        saida.toByteArray()
    }
}