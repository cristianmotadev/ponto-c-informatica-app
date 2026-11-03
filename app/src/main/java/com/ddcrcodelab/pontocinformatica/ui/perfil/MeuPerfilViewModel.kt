package com.ddcrcodelab.pontocinformatica.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddcrcodelab.pontocinformatica.data.repository.UsuarioRepository
import com.ddcrcodelab.pontocinformatica.util.SessaoManager
import com.ddcrcodelab.pontocinformatica.util.ValidadorSenha
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MeuPerfilUiState(
    val salvando: Boolean = false,
    val erro: String? = null,
    val sucesso: Boolean = false
)

class MeuPerfilViewModel(
    private val usuarioRepository: UsuarioRepository,
    private val sessao: SessaoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeuPerfilUiState())
    val uiState: StateFlow<MeuPerfilUiState> = _uiState.asStateFlow()

    /**
     * Troca de senha com reautenticação: o Firebase exige provar a senha
     * ATUAL antes de aceitar updatePassword (proteção contra sessão roubada).
     */
    fun alterarSenha(senhaAtual: String, senhaNova: String, confirmar: String) {
        // Validações baratas antes de gastar rede
        if (senhaNova != confirmar) {
            _uiState.value = _uiState.value.copy(erro = "A confirmação não confere com a nova senha.")
            return
        }
        if (!ValidadorSenha.valida(senhaNova)) {
            _uiState.value = _uiState.value.copy(erro = "A nova senha não cumpre todos os requisitos.")
            return
        }
        if (senhaNova == senhaAtual) {
            _uiState.value = _uiState.value.copy(erro = "A nova senha deve ser diferente da atual.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(salvando = true, erro = null)
            try {
                val user = FirebaseAuth.getInstance().currentUser
                    ?: throw IllegalStateException("Sessão expirada. Entre novamente.")
                val email = user.email.orEmpty()

                // 1) Prova que sabe a senha atual
                val credencial = EmailAuthProvider.getCredential(email, senhaAtual)
                user.reauthenticate(credencial).await()

                // 2) Troca a senha
                user.updatePassword(senhaNova).await()

                // 3) Se estava em troca obrigatória, libera o app
                if (sessao.usuario.value?.senhaTemporaria == true) {
                    usuarioRepository.limparFlagSenhaTemporaria(user.uid)
                }

                _uiState.value = _uiState.value.copy(salvando = false, sucesso = true)
            } catch (e: FirebaseAuthException) {
                _uiState.value = _uiState.value.copy(
                    salvando = false,
                    erro = mensagemAmigavel(e.errorCode)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    salvando = false,
                    erro = e.message ?: "Falha ao alterar a senha."
                )
            }
        }
    }

    fun consumirSucesso() {
        _uiState.value = _uiState.value.copy(sucesso = false)
    }

    private fun mensagemAmigavel(codigo: String): String = when (codigo) {
        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_CREDENTIAL" -> "Senha atual incorreta."
        "ERROR_WEAK_PASSWORD" -> "Senha nova fraca demais para o Firebase (mínimo 6)."
        "ERROR_TOO_MANY_REQUESTS" -> "Muitas tentativas. Aguarde alguns minutos."
        else -> "Não foi possível alterar a senha."
    }
}