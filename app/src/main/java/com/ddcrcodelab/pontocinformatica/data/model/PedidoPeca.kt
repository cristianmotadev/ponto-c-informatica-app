package com.ddcrcodelab.pontocinformatica.data.model

import com.google.firebase.firestore.DocumentId

/** Ciclo de vida do pedido de peça ao dono. */
object StatusPedido {
    const val PENDENTE = "PENDENTE"
    const val APROVADO = "APROVADO"
    const val RECUSADO = "RECUSADO"
    const val CANCELADO = "CANCELADO"

    fun rotulo(valor: String): String = when (valor) {
        APROVADO -> "Aprovado"
        RECUSADO -> "Recusado"
        CANCELADO -> "Cancelado"
        else -> "Pendente"
    }
}

/**
 * Pedido de peça feito por um membro da equipe ao dono.
 * Identificação SEMPRE por nome completo (regra do dono).
 */
data class PedidoPeca(
    @DocumentId val id: String = "",
    val solicitanteUid: String = "",
    val solicitanteNome: String = "",
    val marca: String = "",
    val modelo: String = "",
    val descricao: String = "",        // código da peça, cabo flex, extras
    val quantidade: Int = 1,
    val osReferencia: String = "",     // opcional: OS/cliente a quem a peça atende
    val status: String = StatusPedido.PENDENTE,
    val previsao: String = "",         // dono: quando consegue a peça
    val mensagemResposta: String = "", // dono: complemento ou motivo da recusa
    val criadaEm: Long = 0L,
    val respondidaEm: Long = 0L
)