package com.ddcrcodelab.pontocinformatica.util

/**
 * Catálogo de permissões (Fase 11).
 * Os valores são gravados nos PERFIS e checados em duas camadas:
 *  - UI (botões some/desabilitam)
 *  - Regras do Firestore (por campo, via affectedKeys)
 * NÃO renomeie valores sem migrar perfis e regras.
 */
object Permissoes {
    const val CRIAR_OS = "criar_os"
    const val EDITAR_DADOS = "editar_os.dados"
    const val EDITAR_STATUS = "editar_os.status"
    const val EDITAR_FOTOS = "editar_os.fotos"
    const val COMPARTILHAR = "editar_os.compartilhar"
    const val DAR_DIAGNOSTICO = "dar_diagnostico"
    const val ORCAMENTO_VALOR = "gerar_orcamento.valor"
    const val ORCAMENTO_REVISAO = "gerar_orcamento.revisao"
    const val ORCAMENTO_APROVAR = "gerar_orcamento.aprovar"
    const val EXCLUIR_OS = "excluir_os"
    const val VER_RELATORIOS = "ver_relatorios"

    /** Estrutura da árvore de checkboxes da tela de perfis. */
    val grupos: List<Pair<String, List<String>>> = listOf(
        "Ordens de serviço" to listOf(CRIAR_OS, EXCLUIR_OS),
        "Edição da OS" to listOf(EDITAR_DADOS, EDITAR_STATUS, EDITAR_FOTOS, COMPARTILHAR),
        "Técnico" to listOf(DAR_DIAGNOSTICO),
        "Orçamento" to listOf(ORCAMENTO_VALOR, ORCAMENTO_REVISAO, ORCAMENTO_APROVAR),
        "Gestão" to listOf(VER_RELATORIOS)
    )

    fun rotulo(permissao: String): String = when (permissao) {
        CRIAR_OS -> "Criar OS"
        EXCLUIR_OS -> "Excluir OS (com auditoria)"
        EDITAR_DADOS -> "Editar dados (cliente/aparelho/defeito)"
        EDITAR_STATUS -> "Mudar status"
        EDITAR_FOTOS -> "Adicionar/remover fotos"
        COMPARTILHAR -> "Compartilhar link e WhatsApp"
        DAR_DIAGNOSTICO -> "Dar diagnóstico técnico"
        ORCAMENTO_VALOR -> "Definir/editar valor"
        ORCAMENTO_REVISAO -> "Criar revisões e novos prazos"
        ORCAMENTO_APROVAR -> "Aprovar pelo lojista"
        VER_RELATORIOS -> "Ver relatórios"
        else -> permissao
    }
}