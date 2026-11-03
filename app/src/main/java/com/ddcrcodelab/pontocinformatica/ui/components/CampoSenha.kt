package com.ddcrcodelab.pontocinformatica.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ddcrcodelab.pontocinformatica.util.ValidadorSenha

/**
 * Campo de senha com "olhinho" (mostrar/ocultar).
 * Usamos emoji em vez de Icons.Default.Visibility porque Visibility/
 * VisibilityOff NÃO vêm no material-icons-core (só no extended, que é
 * pesado). Quando o app precisar do extended, trocamos aqui em 1 linha.
 */
@Composable
fun CampoSenha(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier
) {
    var visivel by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = true,
        visualTransformation = if (visivel) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visivel = !visivel }) {
                Text(
                    text = if (visivel) "🙈" else "👁️",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        modifier = modifier
    )
}

/**
 * Checklist em tempo real dos requisitos de senha.
 * Cada item vira ✅ assim que o requisito é cumprido enquanto digita.
 */
@Composable
fun ChecklistSenha(
    senha: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ValidadorSenha.REQUISITOS.forEach { requisito ->
            val atendido = requisito.atendido(senha)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (atendido) "✅" else "⬜")
                Spacer(Modifier.width(8.dp))
                Text(
                    text = requisito.rotulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (atendido) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}