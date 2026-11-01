package com.ddcrcodelab.pontocinformatica

import android.app.Application
import com.ddcrcodelab.pontocinformatica.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Ponto de entrada antes de qualquer Activity: o Koin precisa estar
 * iniciado aqui para que qualquer tela consiga receber dependências.
 */
class PontoCApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger() // loga o que o Koin resolve — ajuda a depurar
            androidContext(this@PontoCApplication)
            modules(appModule)
        }
    }
}