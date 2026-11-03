package com.ddcrcodelab.pontocinformatica.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ddcrcodelab.pontocinformatica.ui.components.CampoSenha
import com.ddcrcodelab.pontocinformatica.ui.components.ChecklistSenha
import com.ddcrcodelab.pontocinformatica.util.SessaoManager
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

/**
 * Perfil do usuário logado (dono ou funcionário).
 *
 * [obrigatoria] = true quando a senha foi redefinida pelo dono:
 * some o botão de voltar e o app só libera depois de salvar a nova senha.
 */
@Composable
fun MeuPerfilScreen(
    obrigatoria: Boolean = false,
    aoVoltar: () -> Unit = {},
    viewModel: MeuPerfilViewModel = koinViewModel(),
    sessao: SessaoManager = koinInject()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val usuario by sessao.usuario.collectAsStateWithLifecycle()
    val perfil by sessao.perfil.collectAsStateWithLifecycle()
    val ehDono by sessao.ehDono.collectAsStateWithLifecycle()

    var senhaAtual by remember { mutableStateOf("") }
    var senhaNova by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }

    LaunchedEffect(state.sucesso) {
        if (state.sucesso) {
            viewModel.consumirSucesso()
            if (!obrigatoria) aoVoltar()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!obrigatoria) {
                TextButton(onClick = aoVoltar) { Text("← Voltar") }
            }

            Text("Meu perfil", style = MaterialTheme.typography.headlineSmall)

            if (obrigatoria) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🔒 Sua senha foi redefinida pelo dono.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Crie uma nova senha para continuar usando o app.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ===== Dados da conta (somente leitura) =====
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(usuario?.nome.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text("@${usuario?.username.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                    Text(usuario?.email.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = if (ehDono) "Perfil: Dono" else "Perfil: ${perfil?.nome ?: "—"}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ===== Troca de senha =====
            Text("Alterar senha", style = MaterialTheme.typography.titleMedium)

            CampoSenha(
                valor = senhaAtual,
                aoMudar = { senhaAtual = it },
                rotulo = "Senha atual",
                modifier = Modifier.fillMaxWidth()
            )
            CampoSenha(
                valor = senhaNova,
                aoMudar = { senhaNova = it },
                rotulo = "Nova senha",
                modifier = Modifier.fillMaxWidth()
            )
            ChecklistSenha(senha = senhaNova)
            CampoSenha(
                valor = confirmar,
                aoMudar = { confirmar = it },
                rotulo = "Confirmar nova senha",
                modifier = Modifier.fillMaxWidth()
            )

            state.erro?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.alterarSenha(senhaAtual, senhaNova, confirmar) },
                enabled = !state.salvando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.salvando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salvar nova senha")
                }
            }
        }
    }
}