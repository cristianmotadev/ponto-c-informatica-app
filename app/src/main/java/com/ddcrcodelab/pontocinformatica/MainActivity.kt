package com.ddcrcodelab.pontocinformatica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ddcrcodelab.pontocinformatica.data.ThemePreferences
import com.ddcrcodelab.pontocinformatica.ui.funcionarios.FuncionariosScreen
import com.ddcrcodelab.pontocinformatica.ui.login.LoginScreen
import com.ddcrcodelab.pontocinformatica.ui.notificacoes.NotificacoesScreen
import com.ddcrcodelab.pontocinformatica.ui.os.CriarOsScreen
import com.ddcrcodelab.pontocinformatica.ui.os.DetalheOsScreen
import com.ddcrcodelab.pontocinformatica.ui.os.ListaOsScreen
import com.ddcrcodelab.pontocinformatica.ui.pedidos.PedidosScreen
import com.ddcrcodelab.pontocinformatica.ui.perfil.MeuPerfilScreen
import com.ddcrcodelab.pontocinformatica.ui.theme.PontoCTheme
import com.ddcrcodelab.pontocinformatica.ui.theme.ThemeMode
import com.ddcrcodelab.pontocinformatica.util.SessaoManager
import com.google.firebase.auth.FirebaseAuth
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePrefs = remember { ThemePreferences(applicationContext) }
            val themeMode by themePrefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            PontoCTheme(themeMode = themeMode) {
                val sessao: SessaoManager = koinInject()
                val usuarioSessao by sessao.usuario.collectAsStateWithLifecycle()

                val logado = FirebaseAuth.getInstance().currentUser != null

                when {
                    // Senha redefinida pelo dono → troca obrigatória trava o app
                    logado && usuarioSessao?.senhaTemporaria == true -> {
                        MeuPerfilScreen(obrigatoria = true)
                    }

                    // Aguarda o perfil chegar antes de decidir a rota inicial
                    logado && usuarioSessao == null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {
                        val navController = rememberNavController()
                        val inicio = if (logado) "home" else "login"

                        NavHost(navController, startDestination = inicio) {
                            composable("login") {
                                LoginScreen(
                                    aoEntrar = {
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("home") {
                                ListaOsScreen(
                                    aoCriarOs = { navController.navigate("criar_os") },
                                    aoAbrirDetalhe = { osId -> navController.navigate("detalhe/$osId") },
                                    aoAbrirEquipe = { navController.navigate("funcionarios") },
                                    aoAbrirPerfil = { navController.navigate("perfil") },
                                    aoAbrirPedidos = { navController.navigate("pedidos") },
                                    aoAbrirNotificacoes = { navController.navigate("notificacoes") },
                                    aoSair = {
                                        FirebaseAuth.getInstance().signOut()
                                        navController.navigate("login") { popUpTo(0) }
                                    }
                                )
                            }
                            composable("criar_os") {
                                CriarOsScreen(aoSalvar = { navController.popBackStack() })
                            }
                            composable("detalhe/{osId}") { backStack ->
                                DetalheOsScreen(
                                    osId = backStack.arguments?.getString("osId").orEmpty(),
                                    aoVoltar = { navController.popBackStack() }
                                )
                            }
                            composable("funcionarios") {
                                FuncionariosScreen(aoVoltar = { navController.popBackStack() })
                            }
                            composable("perfil") {
                                MeuPerfilScreen(aoVoltar = { navController.popBackStack() })
                            }
                            composable("pedidos") {
                                PedidosScreen(aoVoltar = { navController.popBackStack() })
                            }
                            composable("notificacoes") {
                                NotificacoesScreen(aoVoltar = { navController.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }
}