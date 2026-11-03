package com.ddcrcodelab.pontocinformatica.ui.os

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddcrcodelab.pontocinformatica.data.model.OrdemServico
import com.ddcrcodelab.pontocinformatica.data.repository.OsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CriarOsUiState(
    val salvando: Boolean = false,
    val erro: String? = null,
    val salvoComSucesso: Boolean = false
)

class CriarOsViewModel(
    private val repository: OsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CriarOsUiState())
    val uiState: StateFlow<CriarOsUiState> = _uiState.asStateFlow()

    fun salvar(
        clienteNome: String,
        clienteTelefone: String,
        aparelhoMarca: String,
        aparelhoModelo: String,
        defeitoRelatado: String,
        prazoDias: Int,
        superficial: Boolean,
        valorEstimado: Double
    ) {
        if (clienteNome.isBlank() || aparelhoModelo.isBlank() || defeitoRelatado.isBlank()) {
            _uiState.value = CriarOsUiState(erro = "Preencha nome, modelo e defeito.")
            return
        }

        viewModelScope.launch {
            _uiState.value = CriarOsUiState(salvando = true)
            try {
                val agora = System.currentTimeMillis()
                val ordem = OrdemServico(
                    clienteNome = clienteNome.trim(),
                    clienteTelefone = clienteTelefone.trim(),
                    aparelhoMarca = aparelhoMarca.trim(),
                    aparelhoModelo = aparelhoModelo.trim(),
                    defeitoRelatado = defeitoRelatado.trim(),
                    prazoDias = prazoDias,
                    dataEntrada = agora,
                    previsaoEntrega = agora + prazoDias * 86_400_000L,
                    criadoPor = FirebaseAuth.getInstance().uid.orEmpty(),
                    orcamentoSuperficial = superficial,
                    valorOrcado = valorEstimado
                )
                repository.criarOrdem(ordem)
                _uiState.value = CriarOsUiState(salvoComSucesso = true)
            } catch (e: Exception) {
                _uiState.value = CriarOsUiState(erro = "Falha ao salvar: ${e.message}")
            }
        }
    }
}