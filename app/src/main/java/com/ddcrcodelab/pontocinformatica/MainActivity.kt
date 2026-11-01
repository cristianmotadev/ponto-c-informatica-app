package com.ddcrcodelab.pontocinformatica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ddcrcodelab.pontocinformatica.data.ThemePreferences
import com.ddcrcodelab.pontocinformatica.ui.funcionarios.FuncionariosScreen
import com.ddcrcodelab.pontocinformatica.ui.login.LoginScreen
import com.ddcrcodelab.pontocinformatica.ui.os.CriarOsScreen
import com.ddcrcodelab.pontocinformatica.ui.os.DetalheOsScreen
import com.ddcrcodelab.pontocinformatica.ui.os.ListaOsScreen
import com.ddcrcodelab.pontocinformatica.ui.theme.PontoCTheme
import com.ddcrcodelab.pontocinformatica.ui.theme.ThemeMode
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePrefs = remember { ThemePreferences(applicationContext) }
            val themeMode by themePrefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

            PontoCTheme(themeMode = themeMode) {
                val navController = rememberNavController()

                // Se já existe sessão ativa, pula direto para a home
                val inicio = if (FirebaseAuth.getInstance().currentUser != null) "home" else "login"

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
                            aoSair = {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("login") {
                                    popUpTo(0) // limpa toda a pilha
                                }
                            }
                        )
                    }
                    composable("criar_os") {
                        CriarOsScreen(
                            aoSalvar = { navController.popBackStack() }
                        )
                    }
                    composable("detalhe/{osId}") { backStack ->
                        DetalheOsScreen(
                            osId = backStack.arguments?.getString("osId").orEmpty(),
                            aoVoltar = { navController.popBackStack() }
                        )
                    }
                    composable("funcionarios") {
                        FuncionariosScreen(
                            aoVoltar = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}