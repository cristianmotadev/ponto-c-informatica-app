package com.ddcrcodelab.pontocinformatica.ui.funcionarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddcrcodelab.pontocinformatica.data.model.PerfilPermissao
import com.ddcrcodelab.pontocinformatica.data.model.Usuario
import com.ddcrcodelab.pontocinformatica.data.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class FuncionariosUiState(
    val equipe: List<Usuario> = emptyList(),
    val perfis: List<PerfilPermissao> = emptyList(),
    val carregando: Boolean = true,
    val erro: String? = null,
    val ocupado: Boolean = false,
    val erroCadastro: String? = null,
    val criadoComSucesso: Boolean = false,
    // nome -> senha temporária (exibida uma única vez)
    val senhaTemporaria: Pair<String, String>? = null
)

class FuncionariosViewModel(
    private val repository: UsuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FuncionariosUiState())
    val uiState: StateFlow<FuncionariosUiState> = _uiState.asStateFlow()

    init {
        // Seed dos perfis padrão: falha aqui NUNCA pode derrubar o app
        viewModelScope.launch {
            runCatching { repository.criarPerfisPadraoSeNecessario() }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(erro = mensagemErro(e), carregando = false)
                }
        }

        // Listener da equipe: .catch converte negação/erro em mensagem de UI
        viewModelScope.launch {
            repository.observarEquipe()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(erro = mensagemErro(e), carregando = false)
                }
                .collect { equipe ->
                    _uiState.value = _uiState.value.copy(
                        equipe = equipe.sortedBy { it.nome },
                        carregando = false
                    )
                }
        }

        // Listener dos perfis: idem — era aqui que o crash acontecia
        viewModelScope.launch {
            repository.observarPerfis()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(erro = mensagemErro(e), carregando = false)
                }
                .collect { perfis ->
                    _uiState.value = _uiState.value.copy(
                        perfis = perfis.sortedBy { it.nome },
                        carregando = false
                    )
                }
        }
    }

    // ===== Perfis =====

    fun salvarPerfil(id: String?, nome: String, permissoes: List<String>) {
        if (nome.isBlank()) {
            _uiState.value = _uiState.value.copy(erroCadastro = "Dê um nome ao perfil.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ocupado = true, erroCadastro = null)
            try {
                if (id == null) repository.criarPerfil(nome, permissoes)
                else repository.atualizarPerfil(id, nome, permissoes)
                _uiState.value = _uiState.value.copy(ocupado = false, criadoComSucesso = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ocupado = false, erroCadastro = e.message)
            }
        }
    }

    fun excluirPerfil(perfil: PerfilPermissao) {
        val emUso = _uiState.value.equipe.any { it.perfilId == perfil.id }
        if (emUso) {
            _uiState.value = _uiState.value.copy(
                erro = "Perfil em uso por algum membro. Revincule antes de excluir."
            )
            return
        }
        viewModelScope.launch {
            try {
                repository.excluirPerfil(perfil.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(erro = mensagemErro(e))
            }
        }
    }

    // ===== Pessoas =====

    fun salvarFuncionario(
        uid: String?,
        nome: String,
        username: String,
        email: String,
        senha: String,
        perfilId: String
    ) {
        if (nome.isBlank() || username.isBlank() || perfilId.isBlank()) {
            _uiState.value = _uiState.value.copy(erroCadastro = "Preencha nome, username e perfil.")
            return
        }
        // Criação exige e-mail válido (é a credencial real do Auth;
        // username é só um apelido de login)
        if (uid == null && !email.trim().contains("@")) {
            _uiState.value = _uiState.value.copy(erroCadastro = "Informe um e-mail válido para recuperação.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ocupado = true, erroCadastro = null)
            try {
                if (uid == null) {
                    if (senha.length < 6) {
                        _uiState.value = _uiState.value.copy(
                            ocupado = false, erroCadastro = "Senha mínima: 6 caracteres."
                        )
                        return@launch
                    }
                    repository.criarContaFuncionario(nome, username, email, senha, perfilId)
                } else {
                    repository.atualizarUsuario(uid, nome, username, perfilId)
                }
                _uiState.value = _uiState.value.copy(ocupado = false, criadoComSucesso = true)
            } catch (e: FirebaseAuthException) {
                _uiState.value = _uiState.value.copy(
                    ocupado = false, erroCadastro = mensagemAuth(e.errorCode)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ocupado = false, erroCadastro = e.message)
            }
        }
    }

    fun definirAtivo(uid: String, ativo: Boolean) {
        viewModelScope.launch {
            try {
                repository.definirAtivo(uid, ativo)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(erro = mensagemErro(e))
            }
        }
    }

    fun redefinirSenha(usuario: Usuario) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ocupado = true)
            try {
                val temporaria = repository.redefinirSenha(usuario.id)
                _uiState.value = _uiState.value.copy(
                    ocupado = false,
                    senhaTemporaria = usuario.nome to temporaria
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ocupado = false, erro = mensagemErro(e))
            }
        }
    }

    fun excluirUsuario(usuario: Usuario) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ocupado = true)
            try {
                repository.excluirUsuario(usuario.id)
                _uiState.value = _uiState.value.copy(ocupado = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ocupado = false, erro = mensagemErro(e))
            }
        }
    }

    fun consumirSucesso() {
        _uiState.value = _uiState.value.copy(criadoComSucesso = false, erroCadastro = null)
    }

    fun consumirSenhaTemporaria() {
        _uiState.value = _uiState.value.copy(senhaTemporaria = null)
    }

    fun limparErro() {
        _uiState.value = _uiState.value.copy(erro = null)
    }

    // ===== Mensagens =====

    /** PERMISSION_DENIED vira orientação acionável, não stacktrace. */
    private fun mensagemErro(e: Throwable): String =
        if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            "Sem permissão: publique as regras do Firestore e confira se seu usuário é o dono."
        } else {
            e.message ?: "Erro inesperado."
        }

    private fun mensagemAuth(codigo: String): String = when (codigo) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este e-mail já possui conta."
        "ERROR_INVALID_EMAIL" -> "E-mail inválido."
        "ERROR_WEAK_PASSWORD" -> "Senha muito fraca (mínimo 6 caracteres)."
        else -> "Não foi possível criar a conta."
    }
}