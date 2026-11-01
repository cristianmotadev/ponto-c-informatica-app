package com.ddcrcodelab.pontocinformatica.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Ciclo de vida do reparo. O Firestore grava enums como texto:
 * ADICIONE novos valores à vontade, mas não RENOMEIE os existentes
 * (renomear exigiria migração dos documentos já gravados).
 */
enum class StatusOS(val rotulo: String) {
    RECEBIDO("Recebido"),
    EM_ANALISE("Em análise"),
    AGUARDANDO_PECA("Aguardando peça"),
    EM_REPARO("Em reparo"),
    PRONTO("Pronto para retirada"),
    ENTREGUE("Entregue")
}

/**
 * Foto do aparelho: URL pública de download (Storage) + quando foi tirada.
 * Gravada como item de array dentro do documento da OS.
 */
data class FotoOs(
    val url: String = "",
    val timestamp: Long = 0L
)

/**
 * Ordem de Serviço = documento da coleção "ordensServico".
 * Campos simples (String/Long/Double/enum) porque o Firestore mapeia
 * POJOs diretamente. Timestamps em millis (Long) facilitam a conta
 * de "quanto tempo falta" sem depender de java.time (desugaring no minSdk 24).
 */
data class OrdemServico(
    // @DocumentId é preenchido na leitura com o id do documento
    // (e NÃO é gravado como campo)
    @DocumentId val id: String = "",
    val clienteNome: String = "",
    val clienteTelefone: String = "",
    val aparelhoMarca: String = "",
    val aparelhoModelo: String = "",
    val defeitoRelatado: String = "",
    val diagnostico: String = "",
    val status: StatusOS = StatusOS.RECEBIDO,
    val valorOrcado: Double = 0.0,
    val prazoDias: Int = 0,             // prazo estimado informado na entrada
    val dataEntrada: Long = 0L,         // millis
    val previsaoEntrega: Long = 0L,     // dataEntrada + prazo (millis)
    val criadoPor: String = "",         // uid de quem criou (futuro: funcionários)
    val historico: List<HistoricoEntrada> = emptyList(),
    val fotos: List<FotoOs> = emptyList(),

    // Token aleatório (UUID) que compõe o link público do cliente
    val tokenPublico: String = "",

    // Aprovação do orçamento pelo cliente no site:
    // null = pendente | true = aprovado | false = recusado
    val orcamentoAprovado: Boolean? = null,
    val orcamentoDecididoEm: Long = 0L
)

/**
 * Entrada do histórico: o que foi feito e quando.
 * Gravado como array dentro do documento da OS.
 */
data class HistoricoEntrada(
    val mensagem: String = "",
    val timestamp: Long = 0L
)