package com.ddcrcodelab.pontocinformatica.data.repository

import android.content.Context
import com.ddcrcodelab.pontocinformatica.data.model.PerfilPermissao
import com.ddcrcodelab.pontocinformatica.data.model.Permissoes
import com.ddcrcodelab.pontocinformatica.data.model.Usernames
import com.ddcrcodelab.pontocinformatica.data.model.Usuario
import com.ddcrcodelab.pontocinformatica.data.model.UsuarioPorNome
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface UsuarioRepository {
    fun observarUsuario(uid: String): Flow<Usuario?>
    fun observarPerfil(perfilId: String): Flow<PerfilPermissao?>
    fun observarPerfis(): Flow<List<PerfilPermissao>>
    fun observarEquipe(): Flow<List<Usuario>>

    /** Login por username: username → índice → email para o signIn. */
    suspend fun buscarPorUsername(username: String): UsuarioPorNome?

    /** Recuperação de senha: Firebase envia e-mail para o e-mail real. */
    suspend fun enviarResetSenha(email: String)

    /** Cria conta no Auth (app secundário) + perfil + índice de username. */
    suspend fun criarContaFuncionario(
        nome: String,
        username: String,
        email: String,
        senha: String,
        perfilId: String
    ): String

    /** Edita nome/username/perfil; mantém o índice usuariosPorNome coerente. */
    suspend fun atualizarUsuario(uid: String, nome: String, username: String, perfilId: String)

    suspend fun definirAtivo(uid: String, ativo: Boolean)

    /** FASE 12: o próprio usuário limpa o flag após a troca obrigatória. */
    suspend fun limparFlagSenhaTemporaria(uid: String)

    suspend fun salvarPerfil(perfil: PerfilPermissao): String
    suspend fun excluirPerfil(perfilId: String)

    /** Cria Balcão e Técnico se a coleção perfis estiver vazia (bootstrap). */
    suspend fun garantirPresets()

    /** Cloud Function: gera senha temporária (exibida uma única vez). */
    suspend fun redefinirSenhaViaFunction(uid: String): String

    /** Cloud Function: exclui Auth + índice + doc (só dono; nunca a si mesmo). */
    suspend fun excluirUsuarioViaFunction(uid: String)
}

class FirestoreUsuarioRepository(
    private val firestore: FirebaseFirestore,
    private val context: Context
) : UsuarioRepository {

    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()

    override fun observarUsuario(uid: String): Flow<Usuario?> = callbackFlow {
        val listener = firestore.collection(COL_USUARIOS).document(uid)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Usuario::class.java))
            }
        awaitClose { listener.remove() }
    }

    override fun observarPerfil(perfilId: String): Flow<PerfilPermissao?> = callbackFlow {
        if (perfilId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection(COL_PERFIS).document(perfilId)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(PerfilPermissao::class.java))
            }
        awaitClose { listener.remove() }
    }

    override fun observarPerfis(): Flow<List<PerfilPermissao>> = callbackFlow {
        val listener = firestore.collection(COL_PERFIS)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(PerfilPermissao::class.java).orEmpty())
            }
        awaitClose { listener.remove() }
    }

    override fun observarEquipe(): Flow<List<Usuario>> = callbackFlow {
        val listener = firestore.collection(COL_USUARIOS)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Usuario::class.java).orEmpty())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun buscarPorUsername(username: String): UsuarioPorNome? {
        val normalizado = Usernames.normalizar(username)
        if (normalizado.isBlank()) return null
        return firestore.collection(COL_NOMES).document(normalizado)
            .get()
            .await()
            ?.toObject(UsuarioPorNome::class.java)
    }

    override suspend fun enviarResetSenha(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim()).await()
    }

    /**
     * O pulo do gato continua o mesmo: createUserWithEmailAndPassword troca
     * a sessão da instância em que roda, então usamos um FirebaseApp
     * SECUNDÁRIO e derrubamos só a sessão dele no final.
     */
    override suspend fun criarContaFuncionario(
        nome: String,
        username: String,
        email: String,
        senha: String,
        perfilId: String
    ): String = withContext(Dispatchers.IO) {
        val usernameNormalizado = Usernames.normalizar(username)
        require(Usernames.valido(usernameNormalizado)) {
            "Username inválido: use 3-20 caracteres (letras minúsculas, números, . _ -)"
        }

        // Unicidade pré-checada para mensagem amigável (a garantia real
        // continua sendo o create do documento, atômico no Firestore)
        val existente = firestore.collection(COL_NOMES).document(usernameNormalizado)
            .get().await()
        require(!existe(existente)) { "Este username já está em uso." }

        val authSecundario = FirebaseAuth.getInstance(appSecundario())
        try {
            val resultado = authSecundario
                .createUserWithEmailAndPassword(email.trim(), senha)
                .await()
            val uid = resultado.user?.uid
                ?: throw IllegalStateException("Auth não retornou UID")

            val batch = firestore.batch()

            batch.set(
                firestore.collection(COL_USUARIOS).document(uid),
                Usuario(
                    id = uid,
                    nome = nome.trim(),
                    email = email.trim(),
                    username = usernameNormalizado,
                    papel = com.ddcrcodelab.pontocinformatica.data.model.Papeis.FUNCIONARIO,
                    perfilId = perfilId,
                    ativo = true,
                    criadoEm = System.currentTimeMillis()
                )
            )
            batch.set(
                firestore.collection(COL_NOMES).document(usernameNormalizado),
                UsuarioPorNome(id = usernameNormalizado, uid = uid, email = email.trim())
            )
            batch.commit().await()
            uid
        } finally {
            authSecundario.signOut()
        }
    }

    override suspend fun atualizarUsuario(
        uid: String,
        nome: String,
        username: String,
        perfilId: String
    ) {
        val usernameNormalizado = Usernames.normalizar(username)
        require(Usernames.valido(usernameNormalizado)) {
            "Username inválido: use 3-20 caracteres (letras minúsculas, números, . _ -)"
        }

        val docAtual = firestore.collection(COL_USUARIOS).document(uid).get().await()
        val usuarioAtual = docAtual?.toObject(Usuario::class.java)
            ?: throw IllegalStateException("Usuário não encontrado")

        if (usernameNormalizado != usuarioAtual.username) {
            val ocupado = firestore.collection(COL_NOMES).document(usernameNormalizado)
                .get().await()
            require(!existe(ocupado)) { "Este username já está em uso." }
        }

        val batch = firestore.batch()

        batch.update(
            firestore.collection(COL_USUARIOS).document(uid),
            mapOf(
                "nome" to nome.trim(),
                "username" to usernameNormalizado,
                "perfilId" to perfilId
            )
        )
        // Índice: troca o documento antigo pelo novo atomicamente
        if (usernameNormalizado != usuarioAtual.username) {
            if (usuarioAtual.username.isNotBlank()) {
                batch.delete(firestore.collection(COL_NOMES).document(usuarioAtual.username))
            }
            batch.set(
                firestore.collection(COL_NOMES).document(usernameNormalizado),
                UsuarioPorNome(id = usernameNormalizado, uid = uid, email = usuarioAtual.email)
            )
        }
        batch.commit().await()
    }

    override suspend fun definirAtivo(uid: String, ativo: Boolean) {
        firestore.collection(COL_USUARIOS).document(uid)
            .update("ativo", ativo)
            .await()
    }

    override suspend fun limparFlagSenhaTemporaria(uid: String) {
        firestore.collection(COL_USUARIOS).document(uid)
            .update("senhaTemporaria", false)
            .await()
    }

    override suspend fun salvarPerfil(perfil: PerfilPermissao): String {
        require(perfil.nome.isNotBlank()) { "Dê um nome ao perfil." }
        val ref = if (perfil.id.isBlank()) {
            firestore.collection(COL_PERFIS).document()
        } else {
            firestore.collection(COL_PERFIS).document(perfil.id)
        }
        ref.set(perfil.copy(id = ref.id)).await()
        return ref.id
    }

    override suspend fun excluirPerfil(perfilId: String) {
        // Impede excluir perfil ainda vinculado a alguém
        val vinculados = firestore.collection(COL_USUARIOS)
            .whereEqualTo("perfilId", perfilId)
            .limit(1)
            .get()
            .await()
        require(vinculados.isEmpty) {
            "Há usuários vinculados a este perfil. Mude-os de perfil antes de excluir."
        }
        firestore.collection(COL_PERFIS).document(perfilId).delete().await()
    }

    override suspend fun garantirPresets() {
        val existentes = firestore.collection(COL_PERFIS).limit(1).get().await()
        if (!existentes.isEmpty) return

        val batch = firestore.batch()
        val balcao = firestore.collection(COL_PERFIS).document()
        batch.set(balcao, PerfilPermissao(id = balcao.id, nome = "Balcão", permissoes = Permissoes.PRESET_BALCAO))
        val tecnico = firestore.collection(COL_PERFIS).document()
        batch.set(tecnico, PerfilPermissao(id = tecnico.id, nome = "Técnico", permissoes = Permissoes.PRESET_TECNICO))
        batch.commit().await()
    }

    override suspend fun redefinirSenhaViaFunction(uid: String): String {
        val resposta = functions.getHttpsCallable("redefinirSenha")
            .call(mapOf("uid" to uid))
            .await()
        val dados = resposta.data as? Map<*, *>
            ?: throw IllegalStateException("Resposta inválida da Function")
        return dados["senhaTemporaria"] as? String
            ?: throw IllegalStateException("Function não retornou a senha")
    }

    override suspend fun excluirUsuarioViaFunction(uid: String) {
        functions.getHttpsCallable("excluirUsuario")
            .call(mapOf("uid" to uid))
            .await()
    }

    private fun existe(snapshot: com.google.firebase.firestore.DocumentSnapshot?) =
        snapshot != null && snapshot.exists()

    /** FirebaseApp isolado, usado só para cadastro (não desloga o dono). */
    private fun appSecundario(): FirebaseApp = try {
        FirebaseApp.getInstance(APP_SECUNDARIO)
    } catch (e: IllegalStateException) {
        FirebaseApp.initializeApp(
            context,
            FirebaseApp.getInstance().options,
            APP_SECUNDARIO
        )
    }

    private companion object {
        const val COL_USUARIOS = "usuarios"
        const val COL_PERFIS = "perfis"
        const val COL_NOMES = "usuariosPorNome"
        const val APP_SECUNDARIO = "auth-cadastro-equipe"
    }
}