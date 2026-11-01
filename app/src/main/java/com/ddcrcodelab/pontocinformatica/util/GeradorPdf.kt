package com.ddcrcodelab.pontocinformatica.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import com.ddcrcodelab.pontocinformatica.data.model.OrdemServico
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gera o PDF do orçamento desenhando DIRETO em Canvas + PdfDocument.
 *
 * Por que sem WebView: WebView "headless" (fora de tela) não dispara
 * onPageFinished em alguns fabricantes (MIUI etc.), travando a coroutine
 * para sempre. Canvas é síncrono, nativo (API 19+), roda fora da Main
 * thread e não depende de renderizador externo.
 *
 * Layout: página A4 (595x842 pontos), com paginação automática caso o
 * conteúdo estoure o limite inferior.
 */
class GeradorPdf(private val context: Context) {

    private val fmtData = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    private val fmtValor = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    // A4 em pontos (1 pt = 1/72 de polegada)
    private val larguraPagina = 595
    private val alturaPagina = 842
    private val margem = 40f
    private val limiteInferior = 800f

    // Paleta da marca (mesma do app e do site)
    private val laranja = Color.parseColor("#B25E3A")
    private val preto = Color.parseColor("#1A1A1A")
    private val cinza = Color.parseColor("#888888")
    private val fundoClaro = Color.parseColor("#F5F2EF")
    private val fundoTotal = Color.parseColor("#FFF4EE")
    private val cinzaLinha = Color.parseColor("#DDDDDD")

    /**
     * Gera o PDF e devolve o arquivo pronto para compartilhamento.
     * Roda em Dispatchers.IO: desenho de Canvas não exige Main thread.
     */
    suspend fun gerar(ordem: OrdemServico): File = withContext(Dispatchers.IO) {
        val documento = PdfDocument()
        var numeroPagina = 0

        fun infoPagina() =
            PdfDocument.PageInfo.Builder(larguraPagina, alturaPagina, numeroPagina++).create()

        var pagina = documento.startPage(infoPagina())
        var canvas = pagina.canvas
        var y = margem + 10

        // Fecha a página atual e abre outra (paginação automática)
        fun novaPagina() {
            documento.finishPage(pagina)
            pagina = documento.startPage(infoPagina())
            canvas = pagina.canvas
            y = margem + 10
        }

        // Garante espaço: se o bloco não couber, pula para a próxima página
        fun garantir(altura: Float) {
            if (y + altura > limiteInferior) novaPagina()
        }

        // ===== Estilos (Paints) =====
        val pTitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = laranja; textSize = 22f; typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val pSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cinza; textSize = 10f; textAlign = Paint.Align.CENTER
        }
        val pSubTitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = preto; textSize = 12f; typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val pLaranjaFill = Paint().apply { color = laranja; style = Paint.Style.FILL }
        val pPretoFill = Paint().apply { color = preto; style = Paint.Style.FILL }
        val pFundoClaro = Paint().apply { color = fundoClaro; style = Paint.Style.FILL }
        val pFundoTotal = Paint().apply { color = fundoTotal; style = Paint.Style.FILL }
        val pLinha = Paint().apply { color = cinzaLinha; style = Paint.Style.FILL }
        val pOs = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = preto; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
        }
        val pChip = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 9f; typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val pSecao = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 9f; typeface = Typeface.DEFAULT_BOLD
        }
        val pLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cinza; textSize = 8f; typeface = Typeface.DEFAULT_BOLD
        }
        val pLabelLaranja = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = laranja; textSize = 9f; typeface = Typeface.DEFAULT_BOLD
        }
        val pValor = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = preto; textSize = 12f }
        val pTotal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = preto; textSize = 24f; typeface = Typeface.DEFAULT_BOLD
        }
        val pRodape = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cinza; textSize = 8f; textAlign = Paint.Align.CENTER
        }
        val pLink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = laranja; textSize = 8f; textAlign = Paint.Align.CENTER
        }

        val larguraUtil = (larguraPagina - 2 * margem).toInt()

        // Parágrafo com quebra automática de linha (StaticLayout, API 23+)
        fun paragrafo(texto: String, paint: Paint, x: Float, largura: Int) {
            // StaticLayout só aceita TextPaint: envelopamos o Paint
            // preservando cor, tamanho e tipoface do estilo original
            val paintTexto = TextPaint(paint)
            val layout = StaticLayout.Builder.obtain(texto, 0, texto.length, paintTexto, largura).build()
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height
        }

        // Rótulo + valor com espaçamento padrão entre campos
        fun campo(label: String, valor: String, x: Float = margem, largura: Int = larguraUtil) {
            garantir(46f)
            canvas.drawText(label.uppercase(Locale("pt", "BR")), x, y, pLabel)
            y += 14f
            paragrafo(valor, pValor, x, largura)
            y += 10f
        }

        // Barra preta de título de seção
        fun secao(titulo: String) {
            garantir(60f)
            canvas.drawRect(margem, y, larguraPagina - margem, y + 16f, pPretoFill)
            canvas.drawText(titulo, margem + 8f, y + 11f, pSecao)
            y += 28f
        }

        val osCurto = ordem.id.take(8).uppercase(Locale("pt", "BR")).ifBlank { "NOVA" }
        val agora = fmtData.format(Date())

        // ===== Cabeçalho =====
        canvas.drawText("Ponto C. Informática", larguraPagina / 2f, y + 14, pTitulo)
        y += 30f
        canvas.drawText("Assistência técnica especializada", larguraPagina / 2f, y, pSub)
        y += 16f
        canvas.drawText("ORÇAMENTO DE SERVIÇO", larguraPagina / 2f, y, pSubTitulo)
        y += 12f
        canvas.drawRect(margem, y, larguraPagina - margem, y + 3f, pLaranjaFill)
        y += 22f

        // ===== Caixa OS + chip de status =====
        garantir(44f)
        canvas.drawRect(margem, y, larguraPagina - margem, y + 30f, pFundoClaro)
        canvas.drawText("OS $osCurto", margem + 10f, y + 19f, pOs)
        val chip = ordem.status.rotulo
        val chipLargura = pChip.measureText(chip) + 20f
        val chipX = larguraPagina - margem - chipLargura - 8f
        canvas.drawRoundRect(chipX, y + 6f, chipX + chipLargura, y + 24f, 9f, 9f, pLaranjaFill)
        canvas.drawText(chip, chipX + chipLargura / 2f, y + 18f, pChip)
        y += 44f

        // ===== Cliente =====
        secao("CLIENTE")
        campo("Nome", ordem.clienteNome.ifBlank { "—" })
        campo("Telefone / WhatsApp", ordem.clienteTelefone.ifBlank { "—" })

        // ===== Aparelho =====
        secao("APARELHO")
        campo("Marca", ordem.aparelhoMarca.ifBlank { "—" })
        campo("Modelo", ordem.aparelhoModelo.ifBlank { "—" })

        // ===== Serviço =====
        secao("SERVIÇO")
        campo("Defeito relatado", ordem.defeitoRelatado.ifBlank { "—" })
        if (ordem.diagnostico.isNotBlank()) {
            campo("Diagnóstico técnico", ordem.diagnostico)
        }

        // ===== Datas em duas colunas =====
        garantir(46f)
        val colunaDireita = margem + larguraUtil / 2f + 10
        val entrada = if (ordem.dataEntrada > 0) fmtData.format(Date(ordem.dataEntrada)) else "—"
        val previsao = if (ordem.previsaoEntrega > 0) fmtData.format(Date(ordem.previsaoEntrega)) else "—"
        canvas.drawText("ENTRADA", margem, y, pLabel)
        canvas.drawText("PREVISÃO DE ENTREGA", colunaDireita, y, pLabel)
        y += 14f
        canvas.drawText(entrada, margem, y, pValor)
        canvas.drawText(previsao, colunaDireita, y, pValor)
        y += 22f

        // ===== Valor total =====
        garantir(76f)
        canvas.drawRect(margem, y, larguraPagina - margem, y + 60f, pFundoTotal)
        canvas.drawRect(margem, y, margem + 5f, y + 60f, pLaranjaFill)
        canvas.drawText("VALOR TOTAL DO ORÇAMENTO", margem + 14f, y + 18f, pLabelLaranja)
        canvas.drawText(fmtValor.format(ordem.valorOrcado), margem + 14f, y + 46f, pTotal)
        y += 84f

        // ===== Rodapé com link público =====
        garantir(70f)
        canvas.drawRect(margem, y, larguraPagina - margem, y + 1f, pLinha)
        y += 16f
        canvas.drawText("Acompanhe em tempo real:", larguraPagina / 2f, y, pRodape)
        y += 12f
        canvas.drawText("pontocinformatica.web.app", larguraPagina / 2f, y, pLink)
        y += 11f
        canvas.drawText("/os/${ordem.tokenPublico}", larguraPagina / 2f, y, pLink)
        y += 16f
        canvas.drawText("Emitido em $agora", larguraPagina / 2f, y, pRodape)

        documento.finishPage(pagina)

        // Salva no cache (sobrescreve o anterior da mesma OS — sem acumular lixo)
        val pasta = File(context.cacheDir, "orcamentos").apply { mkdirs() }
        val arquivo = File(pasta, "orcamento_${ordem.id}.pdf")
        FileOutputStream(arquivo).use { out -> documento.writeTo(out) }
        documento.close()

        arquivo
    }
}