package com.ddcrcodelab.pontocinformatica.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Notificação interna (sino do app).
 * Criada SOMENTE pelas Cloud Functions (Admin SDK): o cliente apenas
 * lê as suas e marca como lida — impossível forjar notificação alheia.
 */
data class Notificacao(
    @DocumentId val id: String = "",
    val destinatarioUid: String = "",
    val titulo: String = "",
    val mensagem: String = "",
    val tipo: String = "",       // "pedido_criado", "pedido_respondido", "pedido_cancelado"
    val lida: Boolean = false,
    val criadaEm: Long = 0L
)