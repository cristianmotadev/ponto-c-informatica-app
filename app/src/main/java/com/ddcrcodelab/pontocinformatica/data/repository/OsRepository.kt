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

/**
 * Contrato de acesso a dados de OS. As telas dependem do contrato,
 * não do Firestore/Storage — e testes futuros podem trocar por implementação fake.
 */
interface OsRepository {
    /** Fluxo em tempo real das OS, mais recentes primeiro. */
    fun observarOrdens(): Flow<List<OrdemServico>>

    /** Fluxo em tempo real de uma OS específica. */
    fun observarOrdem(osId: String): Flow<OrdemServico?>

    /** Cria uma OS nova e devolve o id gerado. */
    suspend fun criarOrdem(ordem: OrdemServico): String

    /** Atualiza o status de uma OS e adiciona entrada no histórico. */
    suspend fun atualizarStatus(osId: String, status: StatusOS, entrada: HistoricoEntrada)

    /** Gera novo token público para a OS (invalida o anterior). */
    suspend fun gerarTokenPublico(osId: String): String

    /** Aprova ou recusa o orçamento (usado pelo app e pelo site). */
    suspend fun decidirOrcamento(osId: String, aprovado: Boolean)

    /** Grava valor do orçamento e diagnóstico técnico. */
    suspend fun atualizarOrcamento(osId: String, valorOrcado: Double, diagnostico: String)

    /** FASE 6: envia bytes já comprimidos ao Storage e registra a URL no documento. */
    suspend fun adicionarFoto(osId: String, bytes: ByteArray): FotoOs

    /** FASE 6: apaga o arquivo do Storage e remove a referência do documento. */
    suspend fun removerFoto(osId: String, foto: FotoOs)
}

/**
 * Implementação Firestore + Storage.
 * Os observar* usam callbackFlow: o listener só fica ativo enquanto alguém
 * coleta o Flow. Quando a tela/ViewModel morre, o awaitClose remove o
 * listener — sem gasto de rede/bateria em segundo plano.
 */
class FirestoreOsRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : OsRepository {

    override fun observarOrdens(): Flow<List<OrdemServico>> = callbackFlow {
        val listener = firestore.collection(COLECAO)
            .orderBy("dataEntrada", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                val ordens = snapshot?.toObjects(OrdemServico::class.java).orEmpty()
                trySend(ordens)
            }
        awaitClose { listener.remove() }
    }

    override fun observarOrdem(osId: String): Flow<OrdemServico?> = callbackFlow {
        val listener = firestore.collection(COLECAO)
            .document(osId)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                val ordem = snapshot?.toObject(OrdemServico::class.java)
                trySend(ordem)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun criarOrdem(ordem: OrdemServico): String {
        val ref = firestore.collection(COLECAO).add(ordem).await()
        return ref.id
    }

    override suspend fun atualizarStatus(osId: String, status: StatusOS, entrada: HistoricoEntrada) {
        firestore.collection(COLECAO)
            .document(osId)
            .update(
                "status", status,
                "historico", FieldValue.arrayUnion(entrada)
            )
            .await()
    }

    override suspend fun gerarTokenPublico(osId: String): String {
        // UUID v4: 122 bits de entropia — impossível adivinhar por força bruta
        val token = UUID.randomUUID().toString()
        firestore.collection(COLECAO)
            .document(osId)
            .update("tokenPublico", token)
            .await()
        return token
    }

    override suspend fun decidirOrcamento(osId: String, aprovado: Boolean) {
        firestore.collection(COLECAO)
            .document(osId)
            .update(
                "orcamentoAprovado", aprovado,
                "orcamentoDecididoEm", System.currentTimeMillis()
            )
            .await()
    }

    override suspend fun atualizarOrcamento(osId: String, valorOrcado: Double, diagnostico: String) {
        firestore.collection(COLECAO)
            .document(osId)
            .update(
                "valorOrcado", valorOrcado,
                "diagnostico", diagnostico
            )
            .await()
    }

    override suspend fun adicionarFoto(osId: String, bytes: ByteArray): FotoOs {
        // Caminho com UUID: inadivinhável e sem colisão de nomes
        val nome = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child("ordensServico/$osId/fotos/$nome")

        // putBytes: envia os bytes já comprimidos pelo ViewModel
        ref.putBytes(bytes).await()
        val url = ref.downloadUrl.await().toString()

        val foto = FotoOs(url = url, timestamp = System.currentTimeMillis())

        // arrayUnion: adiciona ao array sem sobrescrever fotos concorrentes
        firestore.collection(COLECAO)
            .document(osId)
            .update("fotos", FieldValue.arrayUnion(foto))
            .await()

        return foto
    }

    override suspend fun removerFoto(osId: String, foto: FotoOs) {
        // 1) Apaga o arquivo físico no Storage a partir da URL pública
        storage.getReferenceFromUrl(foto.url).delete().await()

        // 2) Remove a referência do documento (arrayRemove casa o objeto inteiro)
        firestore.collection(COLECAO)
            .document(osId)
            .update("fotos", FieldValue.arrayRemove(foto))
            .await()
    }

    private companion object {
        const val COLECAO = "ordensServico"
    }
}