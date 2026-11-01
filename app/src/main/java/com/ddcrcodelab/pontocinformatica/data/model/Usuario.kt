package com.ddcrcodelab.pontocinformatica.data.model

import com.google.firebase.firestore.DocumentId

/** Papéis de acesso dentro da loja. */
object Papeis {
    const val DONO = "dono"
    const val FUNCIONARIO = "funcionario"
}

/**
 * Membro da equipe (coleção "usuarios").
 * O id do documento É o UID do Firebase Auth.
 * FASE 11: username (login alternativo) e perfilId (permissões via perfil).
 * O dono não usa perfilId: tem tudo implicitamente.
 */
data class Usuario(
    @DocumentId val id: String = "",
    val nome: String = "",
    val username: String = "",
    val email: String = "",
    val papel: String = Papeis.FUNCIONARIO,
    val perfilId: String = "",
    val ativo: Boolean = true,
    val criadoEm: Long = 0L
)

/**
 * Perfil de permissões (coleção "perfis"): template reutilizável.
 * Usuários guardam só o perfilId — editar o perfil atualiza todos
 * os vinculados em tempo real, sem loop de escrita.
 */
data class PerfilPermissao(
    @DocumentId val id: String = "",
    val nome: String = "",
    val permissoes: List<String> = emptyList()
)