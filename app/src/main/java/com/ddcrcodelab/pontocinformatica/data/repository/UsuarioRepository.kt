package com.ddcrcodelab.pontocinformatica.data.repository

import android.content.Context
import com.ddcrcodelab.pontocinformatica.data.model.Papeis
import com.ddcrcodelab.pontocinformatica.data.model.PerfilPermissao
import com.ddcrcodelab.pontocinformatica.data.model.Usuario
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
import kotlin.random.Random

interface UsuarioRepository {
    fun observarUsuario(uid: String): Flow<Usuario?>
    fun observarEquipe(): Flow<List<Usuario>>
    fun observarPerfis(): Flow<List<PerfilPermissao>>
    suspend fun buscarEmailPorUsername(username: String): String?
    suspend fun enviarRecuperacao(identificador: String): String
    suspend fun criarPerfisPadraoSeNecessario()
    suspend fun criarPerfil(nome: String, permissoes: List<String>): String
    suspend fun atualizarPerfil(id: String, nome: String, permissoes: List<String>)
    suspend fun excluirPerfil(id: String)
    suspend fun criarContaFuncionario(
        nome: String, username: String, email: String, senha: String, perfilId: String
    ): String
    suspend fun atualizarUsuario(uid: String, nome: String, username: String, perfilId: String)
    suspend fun definirAtivo(uid: String, ativo: Boolean)
    suspend fun redefinirSenha(uid: String): String
    suspend fun excluirUsuario(uid: String)
}

class FirestoreUsuarioRepository(
    private val firestore: FirebaseFirestore,
    private val context: Context,
    private val functions: FirebaseFunctions
) : UsuarioRepository {

    override fun observarUsuario(uid: String): Flow<Usuario?> = callbackFlow {
        val listener = firestore.collection(USUARIOS).document(uid)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) { close(erro); return@addSnapshotListener }
                trySend(snapshot?.toObject(Usuario::class.java))
            }
        awaitClose { listener.remove() }
    }

    override fun observarEquipe(): Flow<List<Usuario>> = callbackFlow {
        val listener = firestore.collection(USUARIOS)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) { close(erro); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Usuario::class.java).orEmpty())
            }
        awaitClose { listener.remove() }
    }

    override fun observarPerfis(): Flow<List<PerfilPermissao>> = callbackFlow {
        val listener = firestore.collection(PERFIS)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) { close(erro); return@addSnapshotListener }
                trySend(snapshot?.toObjects(PerfilPermissao::class.java).orEmpty())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun buscarEmailPorUsername(username: String): String? {
        val doc = firestore.collection(USUARIOS_POR_NOME)
            .document(normalizarUsername(username))
            .get().await()
        return doc?.getString("email")
    }

    /** Aceita username ou e-mail; devolve o e-mail mascarado para confirmação. */
    override suspend fun enviarRecuperacao(identificador: String): String {
        val email = if (identificador.contains("@")) {
            identificador.trim()
        } else {
            buscarEmailPorUsername(identificador)
                ?: throw IllegalArgumentException("Usuário não encontrado.")
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
        return mascarar(email)
    }

    override suspend fun criarPerfisPadraoSeNecessario() {
        val existentes = firestore.collection(PERFIS).limit(1).get().await()
        if (!existentes.isEmpty) return

        criarPerfil(
            "Balcão",
            listOf(
                "criar_os",
                "editar_os.dados",
                "editar_os.compartilhar"
            )
        )
        criarPerfil(
            "Técnico",
            listOf(
                "criar_os",
                "editar_os.dados",
                "editar_os.compartilhar",
                "editar_os.status",
                "editar_os.fotos",
                "dar_diagnostico",
                "gerar_orcamento.valor",
                "gerar_orcamento.revisao"
            )
        )
    }

    override suspend fun criarPerfil(nome: String, permissoes: List<String>): String {
        val ref = firestore.collection(PERFIS)
            .add(PerfilPermissao(nome = nome.trim(), permissoes = permissoes))
            .await()
        return ref.id
    }

    override suspend fun atualizarPerfil(id: String, nome: String, permissoes: List<String>) {
        firestore.collection(PERFIS).document(id)
            .update("nome", nome.trim(), "permissoes", permissoes)
            .await()
    }

    override suspend fun excluirPerfil(id: String) {
        firestore.collection(PERFIS).document(id).delete().await()
    }

    /**
     * Cria a conta no Auth via FirebaseApp SECUNDÁRIO (não desloga o dono)
     * e grava perfil + índice de username em batch atômico.
     */
    override suspend fun criarContaFuncionario(
        nome: String, username: String, email: String, senha: String, perfilId: String
    ): String = withContext(Dispatchers.IO) {
        val userNorm = normalizarUsername(username)
        validarUsername(userNorm)

        // Unicidade: o ID do documento É o username — get barato antes,
        // e o batch falha se outro cadastro concorrer no mesmo instante
        val indice = firestore.collection(USUARIOS_POR_NOME).document(userNorm).get().await()
        if (indice.exists()) throw IllegalArgumentException("Username já em uso.")

        val authSecundario = FirebaseAuth.getInstance(appSecundario())
        try {
            val resultado = authSecundario.createUserWithEmailAndPassword(email.trim(), senha).await()
            val uid = resultado.user?.uid ?: throw IllegalStateException("Auth não retornou UID")

            firestore.batch().apply {
                set(
                    firestore.collection(USUARIOS).document(uid),
                    Usuario(
                        id = uid,
                        nome = nome.trim(),
                        username = userNorm,
                        email = email.trim(),
                        papel = Papeis.FUNCIONARIO,
                        perfilId = perfilId,
                        ativo = true,
                        criadoEm = System.currentTimeMillis()
                    )
                )
                set(
                    firestore.collection(USUARIOS_POR_NOME).document(userNorm),
                    mapOf("uid" to uid, "email" to email.trim())
                )
            }.commit().await()
            uid
        } finally {
            authSecundario.signOut()
        }
    }

    override suspend fun atualizarUsuario(
        uid: String, nome: String, username: String, perfilId: String
    ) {
        val userNorm = normalizarUsername(username)
        validarUsername(userNorm)

        val atual = firestore.collection(USUARIOS).document(uid).get().await()
        val usuario = atual.toObject(Usuario::class.java) ?: throw IllegalArgumentException("Usuário não encontrado.")
        val email = usuario.email

        val indiceNovo = firestore.collection(USUARIOS_POR_NOME).document(userNorm).get().await()
        if (indiceNovo.exists() && indiceNovo.getString("uid") != uid) {
            throw IllegalArgumentException("Username já em uso.")
        }

        firestore.batch().apply {
            update(
                firestore.collection(USUARIOS).document(uid),
                mapOf("nome" to nome.trim(), "username" to userNorm, "perfilId" to perfilId)
            )
            if (usuario.username.isNotBlank() && usuario.username != userNorm) {
                delete(firestore.collection(USUARIOS_POR_NOME).document(usuario.username))
            }
            set(
                firestore.collection(USUARIOS_POR_NOME).document(userNorm),
                mapOf("uid" to uid, "email" to email)
            )
        }.commit().await()
    }

    override suspend fun definirAtivo(uid: String, ativo: Boolean) {
        firestore.collection(USUARIOS).document(uid).update("ativo", ativo).await()
    }

    /** Gera senha temporária e aplica via Cloud Function (Admin SDK). */
    override suspend fun redefinirSenha(uid: String): String {
        val temporaria = gerarSenhaTemporaria()
        functions.getHttpsCallable("redefinirSenha")
            .call(hashMapOf("uid" to uid, "novaSenha" to temporaria))
            .await()
        return temporaria
    }

    override suspend fun excluirUsuario(uid: String) {
        functions.getHttpsCallable("excluirUsuario")
            .call(hashMapOf("uid" to uid))
            .await()
    }

    // ===== utilidades =====

    private fun appSecundario(): FirebaseApp = try {
        FirebaseApp.getInstance(APP_SECUNDARIO)
    } catch (e: IllegalStateException) {
        FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, APP_SECUNDARIO)
    }

    private fun normalizarUsername(bruto: String): String =
        bruto.trim().lowercase().replace(" ", "_")

    private fun validarUsername(username: String) {
        val valido = Regex("^[a-z0-9._-]{3,20}$").matches(username)
        if (!valido) {
            throw IllegalArgumentException(
                "Username: 3-20 caracteres, minúsculas, números, . _ -"
            )
        }
    }

    private fun mascarar(email: String): String {
        val partes = email.split("@")
        if (partes.size != 2) return email
        val nome = partes[0]
        val visivel = nome.take(2)
        return "$visivel***@${partes[1]}"
    }

    private fun gerarSenhaTemporaria(): String {
        val chars = ("ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789")
        return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private companion object {
        const val USUARIOS = "usuarios"
        const val PERFIS = "perfis"
        const val USUARIOS_POR_NOME = "usuariosPorNome"
        const val APP_SECUNDARIO = "auth-cadastro-equipe"
    }
}