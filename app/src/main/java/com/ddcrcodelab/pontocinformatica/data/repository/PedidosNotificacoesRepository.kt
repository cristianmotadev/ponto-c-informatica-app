package com.ddcrcodelab.pontocinformatica.data.repository

import com.ddcrcodelab.pontocinformatica.data.model.Notificacao
import com.ddcrcodelab.pontocinformatica.data.model.PedidoPeca
import com.ddcrcodelab.pontocinformatica.data.model.StatusPedido
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface PedidosNotificacoesRepository {
    /** Funcionário: só os próprios pedidos. */
    fun observarMeusPedidos(uid: String): Flow<List<PedidoPeca>>

    /** Dono: todos os pedidos da equipe. */
    fun observarTodosPedidos(): Flow<List<PedidoPeca>>

    suspend fun criarPedido(pedido: PedidoPeca)

    /** Dono responde: aprova (previsão) ou recusa (motivo). */
    suspend fun responderPedido(
        pedidoId: String,
        aprovado: Boolean,
        previsao: String,
        mensagem: String
    )

    /** Funcionário cancela enquanto PENDENTE. */
    suspend fun cancelarPedido(pedidoId: String)

    fun observarMinhasNotificacoes(uid: String): Flow<List<Notificacao>>
    fun observarContagemNaoLidas(uid: String): Flow<Int>
    suspend fun marcarLida(notificacaoId: String)
    suspend fun marcarTodasLidas(uid: String)
}

class FirestorePedidosNotificacoesRepository(
    private val firestore: FirebaseFirestore
) : PedidosNotificacoesRepository {

    override fun observarMeusPedidos(uid: String): Flow<List<PedidoPeca>> =
        observarPedidos(
            firestore.collection(COL_PEDIDOS).whereEqualTo("solicitanteUid", uid)
        )

    override fun observarTodosPedidos(): Flow<List<PedidoPeca>> =
        observarPedidos(firestore.collection(COL_PEDIDOS))

    /**
     * Pendentes primeiro, depois os mais recentes.
     * Ordenação em memória (lista pequena) para não exigir índice composto.
     */
    private fun observarPedidos(query: Query): Flow<List<PedidoPeca>> = callbackFlow {
        val listener = query.addSnapshotListener { snapshot, erro ->
            if (erro != null) {
                close(erro)
                return@addSnapshotListener
            }
            val pedidos = snapshot?.toObjects(PedidoPeca::class.java).orEmpty()
                .sortedWith(
                    compareBy(
                        { it.status != StatusPedido.PENDENTE },
                        { -it.criadaEm }
                    )
                )
            trySend(pedidos)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun criarPedido(pedido: PedidoPeca) {
        firestore.collection(COL_PEDIDOS).add(pedido).await()
    }

    override suspend fun responderPedido(
        pedidoId: String,
        aprovado: Boolean,
        previsao: String,
        mensagem: String
    ) {
        firestore.collection(COL_PEDIDOS).document(pedidoId).update(
            mapOf(
                "status" to if (aprovado) StatusPedido.APROVADO else StatusPedido.RECUSADO,
                "previsao" to previsao.trim(),
                "mensagemResposta" to mensagem.trim(),
                "respondidaEm" to System.currentTimeMillis()
            )
        ).await()
    }

    override suspend fun cancelarPedido(pedidoId: String) {
        firestore.collection(COL_PEDIDOS).document(pedidoId)
            .update("status", StatusPedido.CANCELADO)
            .await()
    }

    override fun observarMinhasNotificacoes(uid: String): Flow<List<Notificacao>> = callbackFlow {
        val listener = firestore.collection(COL_NOTIFICACOES)
            .whereEqualTo("destinatarioUid", uid)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                trySend(
                    snapshot?.toObjects(Notificacao::class.java).orEmpty()
                        .sortedByDescending { it.criadaEm }
                )
            }
        awaitClose { listener.remove() }
    }

    /** Badge do sino: query só de não lidas (barata, sem trazer documentos pesados). */
    override fun observarContagemNaoLidas(uid: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection(COL_NOTIFICACOES)
            .whereEqualTo("destinatarioUid", uid)
            .whereEqualTo("lida", false)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun marcarLida(notificacaoId: String) {
        firestore.collection(COL_NOTIFICACOES).document(notificacaoId)
            .update("lida", true)
            .await()
    }

    override suspend fun marcarTodasLidas(uid: String) {
        val naoLidas = firestore.collection(COL_NOTIFICACOES)
            .whereEqualTo("destinatarioUid", uid)
            .whereEqualTo("lida", false)
            .get()
            .await()
        if (naoLidas.isEmpty) return

        val batch = firestore.batch()
        naoLidas.forEach { batch.update(it.reference, "lida", true) }
        batch.commit().await()
    }

    private companion object {
        const val COL_PEDIDOS = "pedidosPeca"
        const val COL_NOTIFICACOES = "notificacoes"
    }
}