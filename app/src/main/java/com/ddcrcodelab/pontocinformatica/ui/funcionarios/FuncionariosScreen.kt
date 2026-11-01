package com.ddcrcodelab.pontocinformatica.ui.funcionarios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ddcrcodelab.pontocinformatica.data.model.Papeis
import com.ddcrcodelab.pontocinformatica.data.model.PerfilPermissao
import com.ddcrcodelab.pontocinformatica.data.model.Usuario
import com.ddcrcodelab.pontocinformatica.util.Permissoes
import org.koin.androidx.compose.koinViewModel

@Composable
fun FuncionariosScreen(
    aoVoltar: () -> Unit,
    viewModel: FuncionariosViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Diálogos (null = fechado)
    var dialogPerfil by remember { mutableStateOf<PerfilPermissao?>(null) }
    var criandoPerfil by remember { mutableStateOf(false) }
    var dialogUsuario by remember { mutableStateOf<Usuario?>(null) }
    var criandoUsuario by remember { mutableStateOf(false) }
    var confirmarExclusaoUsuario by remember { mutableStateOf<Usuario?>(null) }
    var confirmarExclusaoPerfil by remember { mutableStateOf<PerfilPermissao?>(null) }

    LaunchedEffect(state.criadoComSucesso) {
        if (state.criadoComSucesso) {
            dialogPerfil = null
            criandoPerfil = false
            dialogUsuario = null
            criandoUsuario = false
            viewModel.consumirSucesso()
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
            TextButton(onClick = aoVoltar) { Text("← Voltar") }
            Text("Equipe", style = MaterialTheme.typography.headlineSmall)

            state.erro?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            // ===== Seção: perfis de permissão =====
            Text("Perfis de permissão", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { criandoPerfil = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("➕ Novo perfil") }

            if (state.carregando) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            } else {
                state.perfis.forEach { perfil ->
                    CardPerfil(
                        perfil = perfil,
                        qtdVinculados = state.equipe.count { it.perfilId == perfil.id },
                        onEditar = { dialogPerfil = perfil },
                        onExcluir = { confirmarExclusaoPerfil = perfil }
                    )
                }
            }

            // ===== Seção: pessoas =====
            Text("Pessoas", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { criandoUsuario = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("➕ Novo funcionário") }

            state.equipe.forEach { usuario ->
                CardUsuario(
                    usuario = usuario,
                    nomePerfil = state.perfis.firstOrNull { it.id == usuario.perfilId }?.nome
                        ?: if (usuario.papel == Papeis.DONO) "Dono (tudo)" else "Sem perfil",
                    onEditar = { dialogUsuario = usuario },
                    onRedefinirSenha = { viewModel.redefinirSenha(usuario) },
                    onExcluir = { confirmarExclusaoUsuario = usuario },
                    onToggleAtivo = { ativo -> viewModel.definirAtivo(usuario.id, ativo) }
                )
            }
        }
    }

    // ===== Diálogos =====
    if (criandoPerfil || dialogPerfil != null) {
        DialogPerfil(
            inicial = dialogPerfil,
            ocupado = state.ocupado,
            erro = state.erroCadastro,
            onDismiss = { criandoPerfil = false; dialogPerfil = null; viewModel.consumirSucesso() },
            onConfirm = { nome, permissoes ->
                viewModel.salvarPerfil(dialogPerfil?.id, nome, permissoes)
            }
        )
    }

    if (criandoUsuario || dialogUsuario != null) {
        DialogUsuario(
            inicial = dialogUsuario,
            perfis = state.perfis,
            ocupado = state.ocupado,
            erro = state.erroCadastro,
            onDismiss = { criandoUsuario = false; dialogUsuario = null; viewModel.consumirSucesso() },
            onConfirm = { nome, username, email, senha, perfilId ->
                viewModel.salvarFuncionario(
                    dialogUsuario?.id, nome, username, email, senha, perfilId
                )
            }
        )
    }

    confirmarExclusaoPerfil?.let { perfil ->
        AlertDialog(
            onDismissRequest = { confirmarExclusaoPerfil = null },
            title = { Text("Excluir perfil?") },
            text = { Text("O perfil \"${perfil.nome}\" será removido.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.excluirPerfil(perfil)
                    confirmarExclusaoPerfil = null
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusaoPerfil = null }) { Text("Cancelar") }
            }
        )
    }

    confirmarExclusaoUsuario?.let { usuario ->
        AlertDialog(
            onDismissRequest = { confirmarExclusaoUsuario = null },
            title = { Text("Excluir conta?") },
            text = {
                Text("A conta de ${usuario.nome} será EXCLUÍDA do Auth e do Firestore. Esta ação não tem volta.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.excluirUsuario(usuario)
                    confirmarExclusaoUsuario = null
                }) { Text("Excluir definitivamente") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusaoUsuario = null }) { Text("Cancelar") }
            }
        )
    }

    state.senhaTemporaria?.let { (nome, senha) ->
        AlertDialog(
            onDismissRequest = { viewModel.consumirSenhaTemporaria() },
            title = { Text("Senha temporária de $nome") },
            text = { Text("Anote e informe ao funcionário. Ela não será mostrada de novo:\n\n$senha") },
            confirmButton = {
                TextButton(onClick = { viewModel.consumirSenhaTemporaria() }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun CardPerfil(
    perfil: PerfilPermissao,
    qtdVinculados: Int,
    onEditar: () -> Unit,
    onExcluir: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    perfil.nome,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onEditar) { Text("Editar") }
                TextButton(onClick = onExcluir) { Text("Excluir") }
            }
            Text(
                "${perfil.permissoes.size} permissões · $qtdVinculados vinculado(s)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CardUsuario(
    usuario: Usuario,
    nomePerfil: String,
    onEditar: () -> Unit,
    onRedefinirSenha: () -> Unit,
    onExcluir: () -> Unit,
    onToggleAtivo: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(usuario.nome, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "@${usuario.username} · ${usuario.email}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        if (usuario.papel == Papeis.DONO) "Dono (tudo)" else "Perfil: $nomePerfil",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (usuario.papel != Papeis.DONO) {
                    Switch(checked = usuario.ativo, onCheckedChange = onToggleAtivo)
                }
            }
            if (usuario.papel != Papeis.DONO) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEditar) { Text("Editar") }
                    OutlinedButton(onClick = onRedefinirSenha) { Text("Redefinir senha") }
                    OutlinedButton(onClick = onExcluir) { Text("Excluir") }
                }
            }
        }
    }
}

@Composable
private fun DialogPerfil(
    inicial: PerfilPermissao?,
    ocupado: Boolean,
    erro: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var nome by remember { mutableStateOf(inicial?.nome.orEmpty()) }
    var marcadas by remember { mutableStateOf(inicial?.permissoes.orEmpty().toMutableList()) }

    AlertDialog(
        onDismissRequest = { if (!ocupado) onDismiss() },
        title = { Text(if (inicial == null) "Novo perfil" else "Editar perfil") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do perfil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Permissoes.grupos.forEach { (grupo, filhos) ->
                    val todosMarcados = filhos.all { it in marcadas }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = todosMarcados,
                            onCheckedChange = { marcar ->
                                marcadas = if (marcar) {
                                    (marcadas + filhos).distinct().toMutableList()
                                } else {
                                    marcadas.filter { it !in filhos }.toMutableList()
                                }
                            }
                        )
                        Text(grupo, style = MaterialTheme.typography.titleSmall)
                    }
                    filhos.forEach { permissao ->
                        Row(
                            modifier = Modifier.padding(start = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = permissao in marcadas,
                                onCheckedChange = { marcar ->
                                    marcadas = if (marcar) {
                                        (marcadas + permissao).distinct().toMutableList()
                                    } else {
                                        marcadas.filter { it != permissao }.toMutableList()
                                    }
                                }
                            )
                            Text(Permissoes.rotulo(permissao), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nome, marcadas) },
                enabled = !ocupado
            ) {
                if (ocupado) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !ocupado) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogUsuario(
    inicial: Usuario?,
    perfis: List<PerfilPermissao>,
    ocupado: Boolean,
    erro: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    val criando = inicial == null
    var nome by remember { mutableStateOf(inicial?.nome.orEmpty()) }
    var username by remember { mutableStateOf(inicial?.username.orEmpty()) }
    var email by remember { mutableStateOf(inicial?.email.orEmpty()) }
    var senha by remember { mutableStateOf("") }
    var perfilId by remember { mutableStateOf(inicial?.perfilId ?: perfis.firstOrNull()?.id.orEmpty()) }

    AlertDialog(
        onDismissRequest = { if (!ocupado) onDismiss() },
        title = { Text(if (criando) "Novo funcionário" else "Editar funcionário") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it },
                    label = { Text("Nome") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text("Username (login)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (criando) {
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("E-mail (recuperação de senha)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = senha, onValueChange = { senha = it },
                        label = { Text("Senha temporária (mín. 6)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text("Perfil de permissões:", style = MaterialTheme.typography.titleSmall)
                perfis.forEach { perfil ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = perfilId == perfil.id,
                            onClick = { perfilId = perfil.id }
                        )
                        Text(perfil.nome)
                    }
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nome, username, email, senha, perfilId) },
                enabled = !ocupado
            ) {
                if (ocupado) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !ocupado) { Text("Cancelar") }
        }
    )
}