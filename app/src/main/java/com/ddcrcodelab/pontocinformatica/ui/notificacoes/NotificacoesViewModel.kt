package com.ddcrcodelab.pontocinformatica.ui.notificacoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddcrcodelab.pontocinformatica.data.model.Notificacao
import com.ddcrcodelab.pontocinformatica.data.repository.PedidosNotificacoesRepository
import com.ddcrcodelab.pontocinformatica.util.SessaoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class NotificacoesUiState(
    val notificacoes: List<Notificacao> = emptyList(),
    val carregando: Boolean = true,
    val erro: String? = null
)

class NotificacoesViewModel(
    private val repository: PedidosNotificacoesRepository,
    private val sessao: SessaoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificacoesUiState())
    val uiState: StateFlow<NotificacoesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessao.usuario
                .flatMapLatest { usuario ->
                    val uid = usuario?.id
                    if (uid == null) flowOf(emptyList())
                    else repository.observarMinhasNotificacoes(uid)
                }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(erro = e.message, carregando = false)
                }
                .collect { lista ->
                    _uiState.value = _uiState.value.copy(notificacoes = lista, carregando = false)
                }
        }
    }

    fun marcarLida(notificacao: Notificacao) {
        if (notificacao.lida) return
        viewModelScope.launch {
            try {
                repository.marcarLida(notificacao.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(erro = e.message)
            }
        }
    }

    fun marcarTodasLidas() {
        val uid = sessao.usuario.value?.id ?: return
        viewModelScope.launch {
            try {
                repository.marcarTodasLidas(uid)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(erro = e.message)
            }
        }
    }
}