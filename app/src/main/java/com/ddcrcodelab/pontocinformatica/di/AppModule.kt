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
import com.ddcrcodelab.pontocinformatica.util.SessaoManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.ddcrcodelab.pontocinformatica.ui.perfil.MeuPerfilViewModel

val appModule = module {
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }
    single<OsRepository> { FirestoreOsRepository(get(), get()) }
    single<UsuarioRepository> { FirestoreUsuarioRepository(get(), get()) }
    single { GeradorPdf(get()) }
    single { SessaoManager(get()) }  // FASE 11: manager de sessão e permissões

    viewModel { LoginViewModel(get()) }
    viewModel { ListaOsViewModel(get(), get()) }
    viewModel { CriarOsViewModel(get()) }
    viewModel { DetalheOsViewModel(get(), get(), get()) }
    viewModel { FuncionariosViewModel(get()) }
    viewModel { MeuPerfilViewModel(get(), get()) }
}