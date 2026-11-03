package com.ddcrcodelab.pontocinformatica.ui.funcionarios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions   // ← adicionada
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ddcrcodelab.pontocinformatica.data.model.PerfilPermissao
import com.ddcrcodelab.pontocinformatica.data.model.Permissoes
import com.ddcrcodelab.pontocinformatica.data.model.Papeis
import com.ddcrcodelab.pontocinformatica.data.model.Usuario
import org.koin.androidx.compose.koinViewModel

@Composable
fun FuncionariosScreen(
    aoVoltar: () -> Unit,
    viewModel: FuncionariosViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // FASE 12: o dono não gerencia a si mesmo aqui (ele usa "Meu perfil")
    val meuUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid

    var criandoUsuario by remember { mutableStateOf(false) }
    var dialogUsuario by remember { mutableStateOf<Usuario?>(null) }

    // FASE 11 CORREÇÃO: estado separado para perfis
    // null = fechado, PerfilPermissao() vazio = criar novo, com id = editar
    var perfilEmEdicao by remember { mutableStateOf<PerfilPermissao?>(null) }
    var confirmarExclusaoPerfil by remember { mutableStateOf<PerfilPermissao?>(null) }
    var confirmarExclusaoUsuario by remember { mutableStateOf<Usuario?>(null) }
    var confirmarRedefinirSenha by remember { mutableStateOf<Usuario?>(null) }

    LaunchedEffect(state.criadoComSucesso) {
        if (state.criadoComSucesso) {
            criandoUsuario = false
            dialogUsuario = null
            perfilEmEdicao = null
            viewModel.consumirSucesso()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = aoVoltar) { Text("← Voltar") }
            Text("Equipe", style = MaterialTheme.typography.headlineSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { criandoUsuario = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("➕ Novo funcionário")
                }
                // FASE 11 CORREÇÃO: abre diálogo de criar perfil (vazio)
                Button(
                    onClick = { perfilEmEdicao = PerfilPermissao() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📋 Gerenciar perfis")
                }
            }

            state.erro?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (state.carregando) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.equipe.filterNot { it.id == meuUid }, key = { it.id }) { usuario ->
                        CardUsuario(
                            usuario = usuario,
                            perfilNome = state.perfis.find { it.id == usuario.perfilId }?.nome ?: "—",
                            onEditar = { dialogUsuario = usuario },
                            onToggleAtivo = { ativo ->
                                viewModel.definirAtivo(usuario.id, ativo)
                            },
                            onRedefinirSenha = { confirmarRedefinirSenha = usuario },
                            onExcluir = { confirmarExclusaoUsuario = usuario }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de criar/editar usuário
    if (criandoUsuario || dialogUsuario != null) {
        DialogUsuario(
            inicial = dialogUsuario,
            perfis = state.perfis,
            ocupado = state.ocupado,
            erro = state.erroCadastro,
            onDismiss = {
                criandoUsuario = false
                dialogUsuario = null
                viewModel.consumirSucesso()
            },
            onConfirm = { nome, username, email, senha, perfilId ->
                if (dialogUsuario == null) {
                    viewModel.criarFuncionario(nome, username, email, senha, perfilId)
                } else {
                    viewModel.atualizarUsuario(
                        dialogUsuario!!.id, nome, username, perfilId
                    )
                }
            }
        )
    }

    // FASE 11 CORREÇÃO: diálogo de perfis abre quando perfilEmEdicao != null
    if (perfilEmEdicao != null) {
        DialogPerfil(
            inicial = perfilEmEdicao,
            ocupado = state.ocupado,
            erro = state.erroCadastro,
            onDismiss = { perfilEmEdicao = null },
            onConfirm = { id, nome, permissoes ->
                viewModel.salvarPerfil(id, nome, permissoes)
                perfilEmEdicao = null
            },
            onExcluir = { perfil ->
                confirmarExclusaoPerfil = perfil
                perfilEmEdicao = null
            },
            onNovo = {
                perfilEmEdicao = PerfilPermissao()
            }
        )
    }

    // Confirmação de exclusão de perfil
    confirmarExclusaoPerfil?.let { perfil ->
        AlertDialog(
            onDismissRequest = { confirmarExclusaoPerfil = null },
            title = { Text("Excluir perfil?") },
            text = { Text("O perfil \"${perfil.nome}\" será removido.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.excluirPerfil(perfil)
                        confirmarExclusaoPerfil = null
                    }
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusaoPerfil = null }) { Text("Cancelar") }
            }
        )
    }

    // Confirmação de exclusão de usuário
    confirmarExclusaoUsuario?.let { usuario ->
        AlertDialog(
            onDismissRequest = { confirmarExclusaoUsuario = null },
            title = { Text("Excluir usuário?") },
            text = {
                Text(
                    "A conta de \"${usuario.nome}\" será excluída permanentemente " +
                            "(Auth, índice e documento). Esta ação não pode ser desfeita."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.excluirUsuario(usuario)
                        confirmarExclusaoUsuario = null
                    }
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusaoUsuario = null }) { Text("Cancelar") }
            }
        )
    }

    // Confirmação de redefinição de senha
    confirmarRedefinirSenha?.let { usuario ->
        AlertDialog(
            onDismissRequest = { confirmarRedefinirSenha = null },
            title = { Text("Redefinir senha?") },
            text = {
                Text(
                    "Uma senha temporária será gerada para \"${usuario.nome}\" " +
                            "e exibida uma única vez. Anote-a e entregue ao funcionário."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.redefinirSenha(usuario)
                        confirmarRedefinirSenha = null
                    }
                ) { Text("Gerar senha") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRedefinirSenha = null }) { Text("Cancelar") }
            }
        )
    }

    // Exibição da senha temporária (após redefinir)
    state.senhaTemporaria?.let { (nome, senha) ->
        AlertDialog(
            onDismissRequest = { viewModel.consumirSenhaTemporaria() },
            title = { Text("Senha temporária") },
            text = {
                Column {
                    Text("Funcionário: $nome")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Senha: $senha",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚠️ Anote esta senha agora. Ela só é exibida uma vez.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.consumirSenhaTemporaria() }) {
                    Text("Entendi")
                }
            }
        )
    }
}

@Composable
private fun CardUsuario(
    usuario: Usuario,
    perfilNome: String,
    onEditar: () -> Unit,
    onToggleAtivo: (Boolean) -> Unit,
    onRedefinirSenha: () -> Unit,
    onExcluir: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(usuario.nome, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "@${usuario.username} · ${usuario.email}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Perfil: $perfilNome",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (usuario.papel != Papeis.DONO) {
                    Switch(checked = usuario.ativo, onCheckedChange = onToggleAtivo)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onEditar) { Text("Editar") }
                if (usuario.papel != Papeis.DONO) {
                    TextButton(onClick = onRedefinirSenha) { Text("Redefinir senha") }
                    TextButton(onClick = onExcluir) {
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
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
    var nome by remember { mutableStateOf(inicial?.nome ?: "") }
    var username by remember { mutableStateOf(inicial?.username ?: "") }
    var email by remember { mutableStateOf(inicial?.email ?: "") }
    var senha by remember { mutableStateOf("") }
    var perfilId by remember { mutableStateOf(inicial?.perfilId ?: "") }

    AlertDialog(
        onDismissRequest = { if (!ocupado) onDismiss() },
        title = { Text(if (inicial == null) "Novo funcionário" else "Editar funcionário") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (3-20 chars, minúsculas, números, . _ -)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail (para recuperação de senha)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                if (inicial == null) {
                    OutlinedTextField(
                        value = senha,
                        onValueChange = { senha = it },
                        label = { Text("Senha temporária (mín. 6)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (perfis.isNotEmpty()) {
                    Text("Selecione o perfil:", style = MaterialTheme.typography.bodySmall)
                    perfis.forEach { perfil ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = perfilId == perfil.id,
                                onCheckedChange = { if (it) perfilId = perfil.id }
                            )
                            Text(perfil.nome)
                        }
                    }
                } else {
                    Text(
                        "Nenhum perfil criado. Crie um perfil primeiro.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nome, username, email, senha, perfilId) },
                enabled = !ocupado
            ) {
                if (ocupado) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salvar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !ocupado) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogPerfil(
    inicial: PerfilPermissao?,
    ocupado: Boolean,
    erro: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?, String, List<String>) -> Unit,
    onExcluir: (PerfilPermissao) -> Unit,
    onNovo: () -> Unit
) {
    var nome by remember { mutableStateOf(inicial?.nome ?: "") }
    var permissoes by remember { mutableStateOf(inicial?.permissoes ?: emptyList()) }

    val ehNovo = inicial?.id.isNullOrBlank()

    AlertDialog(
        onDismissRequest = { if (!ocupado) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (ehNovo) "Novo perfil" else "Editar perfil")
                if (!ehNovo) {
                    TextButton(onClick = { onExcluir(inicial) }) {
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do perfil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Permissões:", style = MaterialTheme.typography.titleSmall)

                Permissoes.GRUPOS.forEach { (grupo, subpermissoes) ->
                    val todasMarcadas = subpermissoes.all { it in permissoes }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = todasMarcadas,
                            onCheckedChange = { marcada ->
                                permissoes = if (marcada) {
                                    (permissoes + subpermissoes).distinct()
                                } else {
                                    permissoes - subpermissoes.toSet()
                                }
                            }
                        )
                        Text(
                            grupo.replace("_", " ").replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    subpermissoes.forEach { sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp)
                        ) {
                            Checkbox(
                                checked = sub in permissoes,
                                onCheckedChange = { marcada ->
                                    permissoes = if (marcada) {
                                        permissoes + sub
                                    } else {
                                        permissoes - sub
                                    }
                                }
                            )
                            Text(
                                Permissoes.ROTULOS[sub] ?: sub,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(inicial?.id?.takeIf { it.isNotBlank() }, nome, permissoes) },
                enabled = !ocupado
            ) {
                if (ocupado) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salvar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !ocupado) { Text("Fechar") }
        }
    )
}