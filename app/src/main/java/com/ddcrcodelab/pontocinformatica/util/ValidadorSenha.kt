package com.ddcrcodelab.pontocinformatica.util

/**
 * Política de senha da loja (Fase 12):
 * mínimo 6 caracteres, 1 maiúscula, 1 minúscula, 1 número e 1 especial (@ # *).
 *
 * Validação client-side: o Firebase Auth só exige 6 caracteres no servidor.
 * Para equipe interna pequena isso é suficiente; o checklist existe para
 * educar quem digita, não para ser muralha.
 */
object ValidadorSenha {

    data class Requisito(
        val rotulo: String,
        val atendido: (String) -> Boolean
    )

    val REQUISITOS: List<Requisito> = listOf(
        Requisito("Mínimo de 6 caracteres") { it.length >= 6 },
        Requisito("Uma letra maiúscula (A-Z)") { it.any { c -> c.isUpperCase() } },
        Requisito("Uma letra minúscula (a-z)") { it.any { c -> c.isLowerCase() } },
        Requisito("Um número (0-9)") { it.any { c -> c.isDigit() } },
        Requisito("Um caractere especial (@, # ou *)") { it.any { c -> c == '@' || c == '#' || c == '*' } }
    )

    /** True somente se TODOS os requisitos forem atendidos. */
    fun valida(senha: String): Boolean = REQUISITOS.all { it.atendido(senha) }
}