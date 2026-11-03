package com.ddcrcodelab.pontocinformatica.util

import com.ddcrcodelab.pontocinformatica.data.model.Papeis
import com.ddcrcodelab.pontocinformatica.data.model.PerfilPermissao
import com.ddcrcodelab.pontocinformatica.data.model.Usuario
import com.ddcrcodelab.pontocinformatica.data.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Resolve em tempo real quem está logado, qual o perfil e quais permissões
 * ele tem. As telas leem este manager para mostrar/esconder ações; as
 * regras do Firestore fazem a mesma checagem no servidor.
 *
 * O dono tem TODAS as permissões implicitamente — nunca depende de perfil.
 */
class SessaoManager(
    private val usuarioRepository: UsuarioRepository
) {
    private val auth = FirebaseAuth.getInstance()
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Coroutine da observação atual (usuário + perfil).
    // Cancelada e recriada a cada troca de sessão (login/logout/troca de conta).
    private var jobObservacao: Job? = null

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private val _perfil = MutableStateFlow<PerfilPermissao?>(null)
    val perfil: StateFlow<PerfilPermissao?> = _perfil.asStateFlow()

    private val _permissoes = MutableStateFlow<Set<String>>(emptySet())
    val permissoes: StateFlow<Set<String>> = _permissoes.asStateFlow()

    private val _ehDono = MutableStateFlow(false)
    val ehDono: StateFlow<Boolean> = _ehDono.asStateFlow()

    /**
     * Inicia a observação da sessão. Chamado uma vez no PontoCApplication.
     *
     * O callback do addAuthStateListener NÃO é escopo suspend: por isso cada
     * mudança de sessão abre uma coroutine própria (e cancela a anterior,
     * evitando listeners órfãos consumindo rede/bateria).
     */
    fun iniciar() {
        auth.addAuthStateListener { firebaseAuth ->
            jobObservacao?.cancel()
            val uid = firebaseAuth.currentUser?.uid

            if (uid == null) {
                // Deslogado: zera tudo
                _usuario.value = null
                _perfil.value = null
                _permissoes.value = emptySet()
                _ehDono.value = false
                return@addAuthStateListener
            }

            jobObservacao = escopo.launch {
                usuarioRepository.observarUsuario(uid).collect { usuario ->
                    _usuario.value = usuario
                    val ehDono = usuario?.papel == Papeis.DONO
                    _ehDono.value = ehDono
                    val perfilId = usuario?.perfilId

                    when {
                        // Dono: todas as permissões, sem depender de perfil
                        ehDono -> {
                            _perfil.value = null
                            _permissoes.value = PERMISSOES_DONO
                        }

                        // Funcionário sem perfil vinculado: sem permissões
                        perfilId.isNullOrBlank() -> {
                            _perfil.value = null
                            _permissoes.value = emptySet()
                        }

                        // Funcionário com perfil: observa o perfil em tempo real
                        else -> {
                            usuarioRepository.observarPerfil(perfilId)
                                .collect { perfil ->
                                    _perfil.value = perfil
                                    _permissoes.value =
                                        perfil?.permissoes?.toSet() ?: emptySet()
                                }
                        }
                    }
                }
            }
        }
    }

    /**
     * Checagem síncrona: "este usuário pode fazer X?".
     * Para o dono, sempre true.
     */
    fun tem(permissao: String): Boolean {
        if (_ehDono.value) return true
        return permissao in _permissoes.value
    }

    /**
     * Versão reativa para Compose.
     * Retorna Flow (não StateFlow): as telas coletam com
     * collectAsStateWithLifecycle(initialValue = false), que é o
     * overload correto para Flow simples.
     */
    fun temFlow(permissao: String): Flow<Boolean> =
        combine(_ehDono, _permissoes) { ehDono, perms ->
            ehDono || permissao in perms
        }

    companion object {
        // Dono tem todas as permissões concedíveis + gerenciar_equipe
        val PERMISSOES_DONO: Set<String> = setOf(
            "criar_os",
            "editar_os.dados",
            "editar_os.status",
            "editar_os.fotos",
            "editar_os.compartilhar",
            "dar_diagnostico",
            "gerar_orcamento.valor",
            "gerar_orcamento.revisao",
            "gerar_orcamento.aprovar",
            "excluir_os",
            "ver_relatorios",
            "gerenciar_equipe"
        )
    }
}