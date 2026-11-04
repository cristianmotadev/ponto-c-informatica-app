package com.ddcrcodelab.pontocinformatica.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ddcrcodelab.pontocinformatica.util.Atualizacao

/**
 * Banner no topo da lista quando há versão nova no GitHub.
 * "Baixar APK" abre o navegador direto no asset da release.
 */
@Composable
fun BannerAtualizacao(
    atualizacao: Atualizacao,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🚀 Atualização disponível: v${atualizacao.versionName}",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(atualizacao.notas, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(atualizacao.urlDownload))
                    )
                }
            ) {
                Text("Baixar APK")
            }
        }
    }
}