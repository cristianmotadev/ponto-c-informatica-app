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
    val sucesso: Boolean = false,
    val enviandoReset: Boolean = false,
    val resetEnviado: Boolean = false,
    val erroReset: String? = null
)

class LoginViewModel(
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * FASE 11: o campo aceita username OU e-mail.
     * Tem "@" → e-mail direto. Sem "@" → normaliza e busca no índice
     * usuariosPorNome para descobrir o e-mail de auth.
     */
    fun entrar(identificador: String, senha: String) {
        if (identificador.isBlank() || senha.isBlank()) {
            _uiState.value = _uiState.value.copy(erro = "Preencha usuário e senha.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(carregando = true, erro = null)
            try {
                val email = resolverEmail(identificador)
                    ?: throw IllegalArgumentException(
                        if (identificador.contains("@")) "Conta não encontrada."
                        else "Usuário não encontrado."
                    )

                auth.signInWithEmailAndPassword(email, senha).await()
                _uiState.value = _uiState.value.copy(carregando = false, sucesso = true)
            } catch (e: FirebaseAuthException) {
                _uiState.value = _uiState.value.copy(
                    carregando = false,
                    erro = mensagemAuth(e.errorCode)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    carregando = false,
                    erro = e.message ?: "Falha no login."
                )
            }
        }
    }

    /** Recuperação: aceita username ou e-mail; o link vai para o e-mail real. */
    fun recuperarSenha(identificador: String) {
        if (identificador.isBlank()) {
            _uiState.value = _uiState.value.copy(erroReset = "Informe usuário ou e-mail.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(enviandoReset = true, erroReset = null)
            try {
                val email = resolverEmail(identificador)
                    ?: throw IllegalArgumentException("Conta não encontrada.")
                usuarioRepository.enviarResetSenha(email)
                _uiState.value = _uiState.value.copy(enviandoReset = false, resetEnviado = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    enviandoReset = false,
                    erroReset = e.message ?: "Não foi possível enviar o e-mail."
                )
            }
        }
    }

    fun consumirSucesso() {
        _uiState.value = _uiState.value.copy(sucesso = false)
    }

    fun consumirReset() {
        _uiState.value = _uiState.value.copy(resetEnviado = false)
    }

    private suspend fun resolverEmail(identificador: String): String? =
        if (identificador.contains("@")) {
            identificador.trim()
        } else {
            usuarioRepository.buscarPorUsername(identificador)?.email
        }

    private fun mensagemAuth(codigo: String): String = when (codigo) {
        "ERROR_INVALID_LOGIN_CREDENTIALS",
        "ERROR_WRONG_PASSWORD" -> "Usuário ou senha incorretos."
        "ERROR_USER_NOT_FOUND" -> "Conta não encontrada."
        "ERROR_USER_DISABLED" -> "Conta desativada. Fale com o responsável."
        "ERROR_TOO_MANY_REQUESTS" -> "Muitas tentativas. Aguarde alguns minutos."
        else -> "Não foi possível entrar."
    }
}