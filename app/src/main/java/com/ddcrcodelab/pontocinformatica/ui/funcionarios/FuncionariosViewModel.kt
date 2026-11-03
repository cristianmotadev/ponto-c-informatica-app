package com.ddcrcodelab.pontocinformatica.ui.funcionarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddcrcodelab.pontocinformatica.data.model.PerfilPermissao
import com.ddcrcodelab.pontocinformatica.data.model.Usuario
import com.ddcrcodelab.pontocinformatica.data.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuthException
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
    val senhaTemporaria: Pair<String, String>? = null // nome do usuário → senha
)

class FuncionariosViewModel(
    private val repository: UsuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FuncionariosUiState())
    val uiState: StateFlow<FuncionariosUiState> = _uiState.asStateFlow()

    init {
        // Seed dos perfis padrão: falha aqui NUNCA pode derrubar o app
        viewModelScope.launch {
            runCatching { repository.garantirPresets() }
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
                    _uiState.value = _uiState.value.copy(perfis = perfis)
                }
        }
    }

    // ===== Usuários =====

    fun criarFuncionario(
        nome: String,
        username: String,
        email: String,
        senha: String,
        perfilId: String
    ) {
        if (nome.isBlank() || username.isBlank() || email.isBlank() || senha.length < 6) {
            _uiState.value = _uiState.value.copy(
                erroCadastro = "Preencha nome, username, e-mail e senha (mín. 6 caracteres)."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ocupado = true, erroCadastro = null)
            try {
                repository.criarContaFuncionario(nome, username, email, senha, perfilId)
                _uiState.value = _uiState.value.copy(ocupado = false, criadoComSucesso = true)
            } catch (e: FirebaseAuthException) {
                _uiState.value = _uiState.value.copy(
                    ocupado = false,
                    erroCadastro = mensagemAuth(e.errorCode)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    ocupado = false,
                    erroCadastro = e.message ?: "Falha ao criar conta."
                )
            }
        }
    }

    fun atualizarUsuario(uid: String, nome: String, username: String, perfilId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ocupado = true, erro = null)
            try {
                repository.atualizarUsuario(uid, nome, username, perfilId)
                _uiState.value = _uiState.value.copy(ocupado = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    ocupado = false,
                    erro = mensagemErro(e)
                )
            }
        }
    }

    fun definirAtivo(uid: String, ativo: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ocupado = true, erro = null)
            try {
                repository.definirAtivo(uid, ativo)
                _uiState.value = _uiState.value.copy(ocupado = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    ocupado = false,
                    erro = mensagemErro(e)
                )
            }
        }
    }

    fun redefinirSenha(usuario: Usuario) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ocupado = true)
            try {
                val temporaria = repository.redefinirSenhaViaFunction(usuario.id)
                _uiState.value = _uiState.value.copy(
                    ocupado = false,
                    senhaTemporaria = usuario.nome to temporaria
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    ocupado = false,
                    erro = mensagemErro(e)
                )
            }
        }
    }

    fun excluirUsuario(usuario: Usuario) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ocupado = true)
            try {
                repository.excluirUsuarioViaFunction(usuario.id)
                _uiState.value = _uiState.value.copy(ocupado = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    ocupado = false,
                    erro = mensagemErro(e)
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
                // salvarPerfil serve tanto para criar (id vazio) quanto atualizar
                repository.salvarPerfil(
                    PerfilPermissao(
                        id = id ?: "",
                        nome = nome,
                        permissoes = permissoes
                    )
                )
                _uiState.value = _uiState.value.copy(ocupado = false, criadoComSucesso = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    ocupado = false,
                    erroCadastro = e.message ?: "Falha ao salvar perfil."
                )
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
            _uiState.value = _uiState.value.copy(ocupado = true, erro = null)
            try {
                repository.excluirPerfil(perfil.id)
                _uiState.value = _uiState.value.copy(ocupado = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    ocupado = false,
                    erro = mensagemErro(e)
                )
            }
        }
    }

    // ===== Consumidores de estado =====

    fun consumirSucesso() {
        _uiState.value = _uiState.value.copy(criadoComSucesso = false)
    }

    fun consumirSenhaTemporaria() {
        _uiState.value = _uiState.value.copy(senhaTemporaria = null)
    }

    // ===== Helpers =====

    private fun mensagemErro(e: Throwable): String =
        e.message ?: "Falha inesperada."

    private fun mensagemAuth(codigo: String): String = when (codigo) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este e-mail já possui conta."
        "ERROR_INVALID_EMAIL" -> "E-mail inválido."
        "ERROR_WEAK_PASSWORD" -> "Senha muito fraca (mínimo 6 caracteres)."
        else -> "Não foi possível criar a conta."
    }
}