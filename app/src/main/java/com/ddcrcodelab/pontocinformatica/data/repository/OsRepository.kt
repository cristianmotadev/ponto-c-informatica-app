package com.ddcrcodelab.pontocinformatica.data.repository

import com.ddcrcodelab.pontocinformatica.data.model.FotoOs
import com.ddcrcodelab.pontocinformatica.data.model.HistoricoEntrada
import com.ddcrcodelab.pontocinformatica.data.model.OrdemServico
import com.ddcrcodelab.pontocinformatica.data.model.StatusOS
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

interface OsRepository {
    fun observarOrdens(): Flow<List<OrdemServico>>
    fun observarOrdem(osId: String): Flow<OrdemServico?>
    suspend fun criarOrdem(ordem: OrdemServico): String
    suspend fun atualizarStatus(osId: String, status: StatusOS, entrada: HistoricoEntrada)
    suspend fun gerarTokenPublico(osId: String): String
    suspend fun decidirOrcamento(osId: String, aprovado: Boolean)
    suspend fun atualizarOrcamento(osId: String, valorOrcado: Double, diagnostico: String)
    suspend fun adicionarFoto(osId: String, bytes: ByteArray): FotoOs
    suspend fun removerFoto(osId: String, foto: FotoOs)

    // FASE 13
    suspend fun converterEmCompleta(osId: String)
    suspend fun salvarAvaliacao(osId: String, nota: Int, comentario: String)
    suspend fun salvarPreferenciaRecibo(osId: String, preferencia: String, email: String)
}

class FirestoreOsRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : OsRepository {

    override fun observarOrdens(): Flow<List<OrdemServico>> = callbackFlow {
        val listener = firestore.collection(COLECAO)
            .orderBy("dataEntrada", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) { close(erro); return@addSnapshotListener }
                trySend(snapshot?.toObjects(OrdemServico::class.java).orEmpty())
            }
        awaitClose { listener.remove() }
    }

    override fun observarOrdem(osId: String): Flow<OrdemServico?> = callbackFlow {
        val listener = firestore.collection(COLECAO).document(osId)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) { close(erro); return@addSnapshotListener }
                trySend(snapshot?.toObject(OrdemServico::class.java))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun criarOrdem(ordem: OrdemServico): String {
        val ref = firestore.collection(COLECAO).add(ordem).await()
        return ref.id
    }

    override suspend fun atualizarStatus(osId: String, status: StatusOS, entrada: HistoricoEntrada) {
        firestore.collection(COLECAO).document(osId)
            .update(
                "status", status,
                "historico", FieldValue.arrayUnion(entrada)
            ).await()
    }

    override suspend fun gerarTokenPublico(osId: String): String {
        val token = UUID.randomUUID().toString()
        firestore.collection(COLECAO).document(osId)
            .update("tokenPublico", token).await()
        return token
    }

    override suspend fun decidirOrcamento(osId: String, aprovado: Boolean) {
        firestore.collection(COLECAO).document(osId)
            .update(
                "orcamentoAprovado", aprovado,
                "orcamentoDecididoEm", System.currentTimeMillis()
            ).await()
    }

    override suspend fun atualizarOrcamento(osId: String, valorOrcado: Double, diagnostico: String) {
        firestore.collection(COLECAO).document(osId)
            .update("valorOrcado", valorOrcado, "diagnostico", diagnostico).await()
    }

    override suspend fun adicionarFoto(osId: String, bytes: ByteArray): FotoOs {
        val nome = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child("ordensServico/$osId/fotos/$nome")
        ref.putBytes(bytes).await()
        val url = ref.downloadUrl.await().toString()
        val foto = FotoOs(url = url, timestamp = System.currentTimeMillis())
        firestore.collection(COLECAO).document(osId)
            .update("fotos", FieldValue.arrayUnion(foto)).await()
        return foto
    }

    override suspend fun removerFoto(osId: String, foto: FotoOs) {
        storage.getReferenceFromUrl(foto.url).delete().await()
        firestore.collection(COLECAO).document(osId)
            .update("fotos", FieldValue.arrayRemove(foto)).await()
    }

    // ===== FASE 13 =====

    override suspend fun converterEmCompleta(osId: String) {
        val entrada = HistoricoEntrada(
            mensagem = "Orçamento superficial convertido em OS completa",
            timestamp = System.currentTimeMillis(),
            autor = "lojista"
        )
        firestore.collection(COLECAO).document(osId)
            .update(
                "orcamentoSuperficial", false,
                "historico", FieldValue.arrayUnion(entrada)
            ).await()
    }

    override suspend fun salvarAvaliacao(osId: String, nota: Int, comentario: String) {
        firestore.collection(COLECAO).document(osId)
            .update(
                "avaliacao", nota,
                "comentarioAvaliacao", comentario.trim()
            ).await()
    }

    override suspend fun salvarPreferenciaRecibo(osId: String, preferencia: String, email: String) {
        firestore.collection(COLECAO).document(osId)
            .update(
                "reciboPreferencia", preferencia,
                "clienteEmail", email.trim()
            ).await()
    }

    private companion object {
        const val COLECAO = "ordensServico"
    }
}