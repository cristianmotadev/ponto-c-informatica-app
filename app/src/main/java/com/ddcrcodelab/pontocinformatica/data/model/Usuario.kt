package com.ddcrcodelab.pontocinformatica.data.model

import com.google.firebase.firestore.DocumentId

/** Papéis de acesso dentro da loja. */
object Papeis {
    const val DONO = "dono"
    const val FUNCIONARIO = "funcionario"
}

/**
 * Regras de username: 3-20 caracteres, minúsculas/números/._-
 * Normalizado no cadastro e no login (doc id = username → unicidade atômica).
 */
object Usernames {
    fun normalizar(bruto: String): String =
        bruto.trim().lowercase().replace(Regex("[^a-z0-9._-]"), "")

    fun valido(normalizado: String): Boolean =
        normalizado.length in 3..20
}

/**
 * Catálogo de permissões (Fase 11). Nomes pontuados = grupo.subpermissão.
 * As regras do Firestore espelham EXATAMENTE estas strings por campo
 * (defesa em profundidade: UI oculta, Firestore recusa).
 */
object Permissoes {
    const val CRIAR_OS = "criar_os"
    const val EDITAR_OS_DADOS = "editar_os.dados"
    const val EDITAR_OS_STATUS = "editar_os.status"
    const val EDITAR_OS_FOTOS = "editar_os.fotos"
    const val EDITAR_OS_COMPARTILHAR = "editar_os.compartilhar"
    const val DAR_DIAGNOSTICO = "dar_diagnostico"
    const val GERAR_ORCAMENTO_VALOR = "gerar_orcamento.valor"
    const val GERAR_ORCAMENTO_REVISAO = "gerar_orcamento.revisao"
    const val GERAR_ORCAMENTO_APROVAR = "gerar_orcamento.aprovar"
    const val EXCLUIR_OS = "excluir_os"
    const val VER_RELATORIOS = "ver_relatorios"

    // Não concedível: implícita do dono
    const val GERENCIAR_EQUIPE = "gerenciar_equipe"

    /** Grupos para a UI de gestão (checkbox de grupo marca todos os filhos). */
    val GRUPOS: LinkedHashMap<String, List<String>> = linkedMapOf(
        CRIAR_OS to listOf(CRIAR_OS),
        "editar_os" to listOf(
            EDITAR_OS_DADOS, EDITAR_OS_STATUS, EDITAR_OS_FOTOS, EDITAR_OS_COMPARTILHAR
        ),
        DAR_DIAGNOSTICO to listOf(DAR_DIAGNOSTICO),
        "gerar_orcamento" to listOf(
            GERAR_ORCAMENTO_VALOR, GERAR_ORCAMENTO_REVISAO, GERAR_ORCAMENTO_APROVAR
        ),
        EXCLUIR_OS to listOf(EXCLUIR_OS),
        VER_RELATORIOS to listOf(VER_RELATORIOS)
    )

    val TODAS_CONCEDIVEIS: List<String> = GRUPOS.values.flatten()

    /** Rótulos amigáveis para a tela de perfis. */
    val ROTULOS: Map<String, String> = mapOf(
        CRIAR_OS to "Criar OS e orçamento superficial",
        EDITAR_OS_DADOS to "Editar dados (cliente, aparelho, defeito)",
        EDITAR_OS_STATUS to "Mudar status (grava histórico)",
        EDITAR_OS_FOTOS to "Adicionar/remover fotos",
        EDITAR_OS_COMPARTILHAR to "Compartilhar link e mensagens WhatsApp",
        DAR_DIAGNOSTICO to "Escrever diagnóstico técnico",
        GERAR_ORCAMENTO_VALOR to "Definir/editar valor do orçamento",
        GERAR_ORCAMENTO_REVISAO to "Criar revisões e novos prazos",
        GERAR_ORCAMENTO_APROVAR to "Aprovar orçamento pelo lojista",
        EXCLUIR_OS to "Excluir OS (com registro de auditoria)",
        VER_RELATORIOS to "Ver e exportar relatórios"
    )

    /** Presets prontos (atalho na criação de perfis). */
    val PRESET_BALCAO = listOf(CRIAR_OS, EDITAR_OS_DADOS, EDITAR_OS_COMPARTILHAR)
    val PRESET_TECNICO = PRESET_BALCAO + listOf(
        EDITAR_OS_STATUS, EDITAR_OS_FOTOS, DAR_DIAGNOSTICO,
        GERAR_ORCAMENTO_VALOR, GERAR_ORCAMENTO_REVISAO
    )
}

/**
 * Membro da equipe (coleção "usuarios").
 * O id do documento É o UID do Firebase Auth.
 * FASE 11: username (login) e perfilId (permissões resolvidas pelo perfil).
 */
data class Usuario(
    @DocumentId val id: String = "",
    val nome: String = "",
    val email: String = "",
    val username: String = "",
    val papel: String = Papeis.FUNCIONARIO,
    val perfilId: String = "",
    val ativo: Boolean = true,
    val criadoEm: Long = 0L,
    // FASE 12: true após o dono redefinir a senha → app trava na troca obrigatória
    val senhaTemporaria: Boolean = false
)

/**
 * Perfil de permissão (coleção "perfis"): template vinculável a usuários.
 * O usuário NÃO guarda a lista de permissões — guarda só perfilId, e as
 * regras resolvem o perfil em tempo real: editou o perfil, mudou para todos.
 */
data class PerfilPermissao(
    @DocumentId val id: String = "",
    val nome: String = "",
    val permissoes: List<String> = emptyList()
)

/**
 * Índice username → dados de auth (coleção "usuariosPorNome").
 * O ID do documento É o username normalizado → unicidade garantida
 * pelo próprio Firestore, sem corrida entre cadastros simultâneos.
 */
data class UsuarioPorNome(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = ""
)