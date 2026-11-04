package com.ddcrcodelab.pontocinformatica.util

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ddcrcodelab.pontocinformatica.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Uma release do GitHub que é mais nova que o APK instalado. */
data class Atualizacao(
    val versionName: String,
    val versionCode: Int,
    val notas: String,
    val urlDownload: String
)

// DataStore próprio para não misturar com as prefs de tema
private val Context.dataStoreUpdate by preferencesDataStore(name = "atualizacao")

/**
 * FASE 14: cheque de atualização via GitHub Releases (repo público → sem token).
 *
 * Convenção de nome do asset na release: pontoc-v1.1.0-2.apk
 * (o número após o último hífen é o versionCode — é ele que comparamos).
 * O "o que há de novo" exibido no banner é o corpo (body) da release.
 */
class VerificadorAtualizacao(private val context: Context) {

    private val _atualizacao = MutableStateFlow<Atualizacao?>(null)
    val atualizacao: StateFlow<Atualizacao?> = _atualizacao.asStateFlow()

    private val urlApi =
        "https://api.github.com/repos/cristianmotadev/ponto-c-informatica-app/releases/latest"

    /** Chamado pela tela; o rate-limit de 1x/dia mora aqui. */
    suspend fun verificarSePrecisa() {
        val chave = longPreferencesKey("ultima_verificacao")
        val agora = System.currentTimeMillis()
        val ultima = context.dataStoreUpdate.data.first()[chave] ?: 0L
        if (agora - ultima < 24 * 60 * 60 * 1000L) return

        context.dataStoreUpdate.edit { it[chave] = agora }
        try {
            _atualizacao.value = consultar()
        } catch (e: Exception) {
            // Sem rede / API instável: silencioso, update não é crítico
            Log.w("VerificadorAtualizacao", "Cheque falhou: ${e.message}")
            _atualizacao.value = null
        }
    }

    private fun consultar(): Atualizacao? {
        val conexao = URL(urlApi).openConnection() as HttpURLConnection
        conexao.requestMethod = "GET"
        conexao.connectTimeout = 8_000
        conexao.readTimeout = 8_000
        conexao.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (conexao.responseCode != HttpURLConnection.HTTP_OK) return null

            val json = JSONObject(conexao.inputStream.bufferedReader().readText())
            val assets = json.optJSONArray("assets") ?: return null

            var urlDownload: String? = null
            var codeRemoto = -1
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val nome = asset.getString("name")
                if (nome.endsWith(".apk")) {
                    urlDownload = asset.getString("browser_download_url")
                    codeRemoto = nome.substringAfterLast("-").removeSuffix(".apk")
                        .toIntOrNull() ?: -1
                }
            }

            // Só avisa se o code remoto for MAIOR que o instalado
            if (urlDownload == null || codeRemoto <= BuildConfig.VERSION_CODE) return null

            return Atualizacao(
                versionName = json.optString("tag_name").removePrefix("v"),
                versionCode = codeRemoto,
                notas = json.optString("body").ifBlank { "Sem notas nesta versão." },
                urlDownload = urlDownload
            )
        } finally {
            conexao.disconnect()
        }
    }
}