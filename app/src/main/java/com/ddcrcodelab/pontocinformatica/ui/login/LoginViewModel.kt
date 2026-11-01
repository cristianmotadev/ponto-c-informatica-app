package com.ddcrcodelab.pontocinformatica.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddcrcodelab.pontocinformatica.data.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LoginUiState(
    val carregando: Boolean = false,
    val erro: String? = null,
    val logado: Boolean = false,
    val recuperando: Boolean = false,
    val mensagemRecuperacao: String? = null
)

class LoginViewModel(
    private val repository: UsuarioRepository
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Aceita username OU e-mail no mesmo campo. */
    fun entrar(identificador: String, senha: String) {
        if (identificador.isBlank() || senha.isBlank()) {
            _uiState.value = LoginUiState(erro = "Preencha usuário e senha.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState(carregando = true)
            try {
                val email = if (identificador.contains("@")) {
                    identificador.trim()
                } else {
                    repository.buscarEmailPorUsername(identificador)
                        ?: run {
                            _uiState.value = LoginUiState(erro = "Usuário não encontrado.")
                            return@launch
                        }
                }
                auth.signInWithEmailAndPassword(email, senha).await()
                _uiState.value = LoginUiState(logado = true)
            } catch (e: FirebaseAuthException) {
                _uiState.value = LoginUiState(erro = mensagemAmigavel(e.errorCode))
            }
        }
    }

    /** Envia link de recuperação para o e-mail real cadastrado. */
    fun recuperarSenha(identificador: String) {
        if (identificador.isBlank()) {
            _uiState.value = _uiState.value.copy(erro = "Informe usuário ou e-mail.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(recuperando = true, erro = null)
            try {
                val mascarado = repository.enviarRecuperacao(identificador)
                _uiState.value = _uiState.value.copy(
                    recuperando = false,
                    mensagemRecuperacao = "Link de recuperação enviado para $mascarado"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    recuperando = false,
                    erro = e.message ?: "Falha na recuperação."
                )
            }
        }
    }

    fun limparMensagemRecuperacao() {
        _uiState.value = _uiState.value.copy(mensagemRecuperacao = null, erro = null)
    }

    private fun mensagemAmigavel(codigo: String): String = when (codigo) {
        "ERROR_INVALID_EMAIL" -> "E-mail inválido."
        "ERROR_USER_NOT_FOUND" -> "Usuário não encontrado."
        "ERROR_WRONG_PASSWORD" -> "Senha incorreta."
        "ERROR_INVALID_CREDENTIAL" -> "Credenciais incorretas."
        else -> "Não foi possível entrar. Tente novamente."
    }
}