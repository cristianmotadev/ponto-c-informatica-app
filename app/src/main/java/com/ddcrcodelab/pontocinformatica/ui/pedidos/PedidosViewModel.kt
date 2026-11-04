package com.ddcrcodelab.pontocinformatica.ui.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddcrcodelab.pontocinformatica.data.model.PedidoPeca
import com.ddcrcodelab.pontocinformatica.data.repository.PedidosNotificacoesRepository
import com.ddcrcodelab.pontocinformatica.util.SessaoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class PedidosUiState(
    val pedidos: List<PedidoPeca> = emptyList(),
    val carregando: Boolean = true,
    val erro: String? = null,
    val ocupado: Boolean = false,
    val erroDialog: String? = null
)

class PedidosViewModel(
    private val repository: PedidosNotificacoesRepository,
    private val sessao: SessaoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PedidosUiState())
    val uiState: StateFlow<PedidosUiState> = _uiState.asStateFlow()

    /** A tela decide o que mostrar (botões do dono vs. do funcionário). */
    val ehDono: StateFlow<Boolean> = sessao.ehDono

    init {
        // Trocou a sessão/papel → troca a fonte de dados automaticamente
        viewModelScope.launch {
            sessao.ehDono
                .flatMapLatest { dono ->
                    val uid = sessao.usuario.value?.id
                    if (uid == null) return@flatMapLatest flowOf(emptyList())
                    if (dono) repository.observarTodosPedidos()
                    else repository.observarMeusPedidos(uid)
                }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(erro = e.message, carregando = false)
                }
                .collect { pedidos ->
                    _uiState.value = _uiState.value.copy(pedidos = pedidos, carregando = false)
                }
        }
    }

    fun criarPedido(
        marca: String,
        modelo: String,
        descricao: String,
        quantidadeTexto: String,
        osReferencia: String
    ) {
        val usuario = sessao.usuario.value
        if (usuario == null) {
            _uiState.value = _uiState.value.copy(erroDialog = "Sessão expirada. Entre novamente.")
            return
        }
        val quantidade = quantidadeTexto.trim().toIntOrNull() ?: 0
        if (marca.isBlank() || modelo.isBlank() || quantidade < 1) {
            _uiState.value = _uiState.value.copy(
                erroDialog = "Preencha marca, modelo e quantidade (mínimo 1)."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ocupado = true, erroDialog = null)
            try {
                repository.criarPedido(
                    PedidoPeca(
                        solicitanteUid = usuario.id,
                        solicitanteNome = usuario.nome,
                        marca = marca.trim(),
                        modelo = modelo.trim(),
                        descricao = descricao.trim(),
                        quantidade = quantidade,
                        osReferencia = osReferencia.trim(),
                        criadaEm = System.currentTimeMillis()
                    )
                )
                _uiState.value = _uiState.value.copy(ocupado = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ocupado = false, erroDialog = e.message)
            }
        }
    }

    /** Dono responde. Recusa SEMPRE exige motivo (regra de negócio do dono). */
    fun responder(pedido: PedidoPeca, aprovado: Boolean, previsao: String, mensagem: String) {
        if (!aprovado && mensagem.isBlank()) {
            _uiState.value = _uiState.value.copy(erroDialog = "Informe o motivo da recusa.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ocupado = true, erroDialog = null)
            try {
                repository.responderPedido(pedido.id, aprovado, previsao, mensagem)
                _uiState.value = _uiState.value.copy(ocupado = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ocupado = false, erroDialog = e.message)
            }
        }
    }

    fun cancelar(pedido: PedidoPeca) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ocupado = true)
            try {
                repository.cancelarPedido(pedido.id)
                _uiState.value = _uiState.value.copy(ocupado = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ocupado = false, erro = e.message)
            }
        }
    }

    fun limparErroDialog() {
        _uiState.value = _uiState.value.copy(erroDialog = null)
    }
}