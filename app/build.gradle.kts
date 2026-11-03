plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.ddcrcodelab.pontocinformatica"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ddcrcodelab.pontocinformatica"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Dependências do template (mantidas)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)

    // Ícones do Material (Icons.Default.Add e cia.): artefato separado do material3
    implementation("androidx.compose.material:material-icons-core")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Firebase BOM 34.x: geração compilada com Kotlin <= 2.2, compatível com o
    // compilador do projeto (Kotlin 2.2.10 do AGP 9.4.1).
    // ÚNICO lugar onde as versões do Firebase são definidas — não fixe versões
    // de libs firebase-* em nenhum outro ponto, senão a mais alta vence de novo.
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-auth")      // sem versão: o BOM gerencia
    implementation("com.google.firebase:firebase-firestore") // banco (Fase 2)

    // Coroutines + Play Services: permite usar await() com Firebase
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // Navegação entre telas com Compose
    implementation("androidx.navigation:navigation-compose:2.10.2")

    // Lifecycle + Compose (collectAsStateWithLifecycle e viewModel)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    // Koin: injeção de dependência SEM processador de anotações (KSP).
    // Escolhido nesta fase por adicionar zero risco à matriz de versões do build;
    // a arquitetura em camadas permite migrar para Hilt depois sem reescrever telas.
    implementation("io.insert-koin:koin-android:4.2.2")
    implementation("io.insert-koin:koin-androidx-compose:4.2.2")

    // DataStore Preferences: persistência de configurações (tema)
    implementation("androidx.datastore:datastore-preferences:1.3.0")

    // Firebase Storage: fotos do aparelho (versão gerenciada pelo BOM)
    implementation("com.google.firebase:firebase-storage")

    // Coil: carregamento assíncrono de imagens com cache (leve e mantido ativamente)
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")

    // Coil 3: fetcher de imagens via rede (OBRIGATÓRIO no Coil 3;
    // no Coil 2 vinha embutido no coil-compose)
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")

    // Cloud Functions (callables: excluir usuário, redefinir senha) — versão pelo BOM
    implementation("com.google.firebase:firebase-functions")

    // Testes (mantidos do template)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}