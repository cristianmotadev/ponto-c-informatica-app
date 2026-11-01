package com.ddcrcodelab.pontocinformatica.di

import com.ddcrcodelab.pontocinformatica.data.repository.FirestoreOsRepository
import com.ddcrcodelab.pontocinformatica.data.repository.FirestoreUsuarioRepository
import com.ddcrcodelab.pontocinformatica.data.repository.OsRepository
import com.ddcrcodelab.pontocinformatica.data.repository.UsuarioRepository
import com.ddcrcodelab.pontocinformatica.ui.funcionarios.FuncionariosViewModel
import com.ddcrcodelab.pontocinformatica.ui.login.LoginViewModel
import com.ddcrcodelab.pontocinformatica.ui.os.CriarOsViewModel
import com.ddcrcodelab.pontocinformatica.ui.os.DetalheOsViewModel
import com.ddcrcodelab.pontocinformatica.ui.os.ListaOsViewModel
import com.ddcrcodelab.pontocinformatica.util.GeradorPdf
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }
    single { FirebaseFunctions.getInstance() }
    single<OsRepository> { FirestoreOsRepository(get(), get()) }
    single<UsuarioRepository> { FirestoreUsuarioRepository(get(), get(), get()) }
    single { GeradorPdf(get()) }

    viewModel { LoginViewModel(get()) }
    viewModel { ListaOsViewModel(get(), get()) }
    viewModel { CriarOsViewModel(get()) }
    viewModel { DetalheOsViewModel(get(), get(), get()) }
    viewModel { FuncionariosViewModel(get()) }
}