package com.ddcrcodelab.pontocinformatica.ui.os

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddcrcodelab.pontocinformatica.data.model.OrdemServico
import com.ddcrcodelab.pontocinformatica.data.model.Papeis
import com.ddcrcodelab.pontocinformatica.data.repository.OsRepository
import com.ddcrcodelab.pontocinformatica.data.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ListaOsUiState(
    val carregando: Boolean = true,
    val ordens: List<OrdemServico> = emptyList(),
    val erro: String? = null,
    // FASE 9: papel e situação da sessão atual
    val ehDono: Boolean = false,
    val bloqueado: Boolean = false
)

class ListaOsViewModel(
    private val repository: OsRepository,
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListaOsUiState())
    val uiState: StateFlow<ListaOsUiState> = _uiState.asStateFlow()

    init {
        val uid = FirebaseAuth.getInstance().uid

        if (uid == null) {
            // Sem sessão (não deveria acontecer: MainActivity guarda a rota)
            _uiState.value = _uiState.value.copy(bloqueado = true, carregando = false)
        } else {
            // Perfil da sessão: papel (mostra/oculta Equipe) e ativo (bloqueia)
            viewModelScope.launch {
                usuarioRepository.observarUsuario(uid)
                    .catch { /* sem perfil: bloqueado abaixo não muda; regras negam dados */ }
                    .collect { usuario ->
                        _uiState.value = _uiState.value.copy(
                            ehDono = usuario?.papel == Papeis.DONO,
                            bloqueado = usuario != null && !usuario.ativo
                        )
                    }
            }

            // Lista de OS em tempo real (regras negam se conta inativa)
            viewModelScope.launch {
                repository.observarOrdens()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(erro = e.message, carregando = false)
                    }
                    .collect { ordens ->
                        _uiState.value = _uiState.value.copy(carregando = false, ordens = ordens)
                    }
            }
        }
    }
}