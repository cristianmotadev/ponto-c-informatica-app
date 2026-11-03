package com.ddcrcodelab.pontocinformatica

import android.app.Application
import com.ddcrcodelab.pontocinformatica.di.appModule
import com.ddcrcodelab.pontocinformatica.util.SessaoManager
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class PontoCApplication : Application() {

    private val sessaoManager: SessaoManager by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@PontoCApplication)
            modules(appModule)
        }
        // Inicia a observação da sessão (quem está logado + permissões)
        sessaoManager.iniciar()
    }
}