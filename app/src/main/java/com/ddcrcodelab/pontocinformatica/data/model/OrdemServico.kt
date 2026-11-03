package com.ddcrcodelab.pontocinformatica.data.model

import com.google.firebase.firestore.DocumentId

enum class StatusOS(val rotulo: String) {
    RECEBIDO("Recebido"),
    EM_ANALISE("Em análise"),
    AGUARDANDO_PECA("Aguardando peça"),
    EM_REPARO("Em reparo"),
    PRONTO("Pronto para retirada"),
    ENTREGUE("Entregue"),
    CANCELADO("Cancelado")
}

/** Foto do aparelho (Storage). */
data class FotoOs(
    val url: String = "",
    val timestamp: Long = 0L
)

/** Entrada do histórico (quem mudou o quê). */
data class HistoricoEntrada(
    val mensagem: String = "",
    val timestamp: Long = 0L,
    val autor: String = ""  // "lojista" | "cliente" | "sistema"
)

/**
 * Ordem de Serviço.
 * FASE 13: orçamento superficial, e-mail do cliente, preferência de recibo,
 * avaliação e comentário.
 */
data class OrdemServico(
    @DocumentId val id: String = "",
    val clienteNome: String = "",
    val clienteTelefone: String = "",
    val clienteEmail: String = "",
    val aparelhoMarca: String = "",
    val aparelhoModelo: String = "",
    val defeitoRelatado: String = "",
    val diagnostico: String = "",
    val status: StatusOS = StatusOS.RECEBIDO,
    val valorOrcado: Double = 0.0,
    val prazoDias: Int = 0,
    val dataEntrada: Long = 0L,
    val previsaoEntrega: Long = 0L,
    val criadoPor: String = "",
    val historico: List<HistoricoEntrada> = emptyList(),
    val fotos: List<FotoOs> = emptyList(),
    val tokenPublico: String = "",
    val orcamentoAprovado: Boolean? = null,
    val orcamentoDecididoEm: Long = 0L,

    // FASE 13
    val orcamentoSuperficial: Boolean = false,
    val reciboPreferencia: String = "",  // "whatsapp" | "email" | "ambos" | ""
    val avaliacao: Int = 0,              // 0..5 (0 = não avaliado)
    val comentarioAvaliacao: String = ""
)