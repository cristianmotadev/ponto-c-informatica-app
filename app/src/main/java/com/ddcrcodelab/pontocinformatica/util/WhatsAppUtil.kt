package com.ddcrcodelab.pontocinformatica.util

import android.net.Uri
import com.ddcrcodelab.pontocinformatica.data.model.OrdemServico
import com.ddcrcodelab.pontocinformatica.data.model.StatusOS
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitários para avisar o cliente pelo WhatsApp.
 * Sem SDK: usamos o link oficial wa.me (deep link), que abre a
 * conversa com o número do cliente e o texto já preenchido.
 */
object WhatsAppUtil {

    private val fmtData = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    /**
     * Converte telefone digitado no formato BR para o formato do wa.me
     * (DDI + DDD + número, só dígitos).
     * Aceita: (11) 99999-0000 | 11 999990000 | 5511999990000
     * Retorna null se não parecer um telefone BR válido.
     */
    fun normalizarTelefone(bruto: String): String? {
        val digitos = bruto.filter { it.isDigit() }
        return when {
            // DDD + número (10 ou 11 dígitos): acrescenta DDI 55
            digitos.length == 10 || digitos.length == 11 -> "55$digitos"
            // Já veio com DDI 55
            digitos.startsWith("55") && (digitos.length == 12 || digitos.length == 13) -> digitos
            else -> null
        }
    }

    /** Link wa.me com a mensagem codificada para URL. */
    fun linkWaMe(telefoneNormalizado: String, mensagem: String): String =
        "https://wa.me/$telefoneNormalizado?text=" + Uri.encode(mensagem)

    /**
     * Mensagem calorosa e informativa, adaptada ao status atual.
     * Inclui a previsão de entrega (quando faz sentido) e o link
     * público de acompanhamento (status, fotos e orçamento).
     */
    fun mensagemStatus(ordem: OrdemServico, link: String): String {
        val situacao = when (ordem.status) {
            StatusOS.RECEBIDO -> "recebido na loja e já registrado no sistema ✅"
            StatusOS.EM_ANALISE -> "em análise pela nossa equipe técnica 🔍"
            StatusOS.AGUARDANDO_PECA -> "aguardando a chegada de uma peça 📦"
            StatusOS.EM_REPARO -> "em reparo na bancada 🔧"
            StatusOS.PRONTO -> "PRONTO para retirada! 🎉"
            StatusOS.ENTREGUE -> "entregue. Obrigado pela confiança! 🙏"
            StatusOS.CANCELADO -> "cancelado"
        }
        val aparelho = "${ordem.aparelhoMarca} ${ordem.aparelhoModelo}".trim()

        return buildString {
            append("Olá, ${ordem.clienteNome}! 👋\n")
            append("Seu $aparelho está: $situacao.\n")
            if (ordem.previsaoEntrega > 0L &&
                ordem.status != StatusOS.PRONTO &&
                ordem.status != StatusOS.ENTREGUE
            ) {
                append("Previsão de entrega: ${fmtData.format(Date(ordem.previsaoEntrega))}.\n")
            }
            append("Acompanhe em tempo real (status, fotos e orçamento): $link")
        }
    }
}