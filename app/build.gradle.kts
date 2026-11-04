import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

// Propriedades do keystore (arquivo fora do Git — ver .gitignore)
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
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
        // FASE 14: cada release SOBRA o versionCode; o app compara com o do GitHub
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            // Assina com o keystore; sem keystore.properties, sai não assinado
            // (e o build avisa) em vez de quebrar o Sync de quem não tem o arquivo
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        // FASE 14: gera BuildConfig (VERSION_CODE/VERSION_NAME) para o cheque de update
        buildConfig = true
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
    // compilador do projeto. ÚNICO lugar onde as versões do Firebase são definidas.
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Coroutines + Play Services: permite usar await() com Firebase
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // Navegação entre telas com Compose
    implementation("androidx.navigation:navigation-compose:2.10.2")

    // Lifecycle + Compose (collectAsStateWithLifecycle e viewModel)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    // Koin: injeção de dependência sem KSP
    implementation("io.insert-koin:koin-android:4.2.2")
    implementation("io.insert-koin:koin-androidx-compose:4.2.2")

    // DataStore Preferences: persistência de configurações (tema, última verificação)
    implementation("androidx.datastore:datastore-preferences:1.3.0")

    // Firebase Storage: fotos do aparelho (versão gerenciada pelo BOM)
    implementation("com.google.firebase:firebase-storage")

    // Coil: carregamento assíncrono de imagens com cache
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")

    // Cloud Functions (callables: excluir usuário, redefinir senha)
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