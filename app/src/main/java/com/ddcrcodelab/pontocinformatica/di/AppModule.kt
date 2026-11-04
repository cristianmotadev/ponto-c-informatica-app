package com.ddcrcodelab.pontocinformatica.di

import com.ddcrcodelab.pontocinformatica.data.repository.FirestoreOsRepository
import com.ddcrcodelab.pontocinformatica.data.repository.FirestorePedidosNotificacoesRepository
import com.ddcrcodelab.pontocinformatica.data.repository.FirestoreUsuarioRepository
import com.ddcrcodelab.pontocinformatica.data.repository.OsRepository
import com.ddcrcodelab.pontocinformatica.data.repository.PedidosNotificacoesRepository
import com.ddcrcodelab.pontocinformatica.data.repository.UsuarioRepository
import com.ddcrcodelab.pontocinformatica.ui.funcionarios.FuncionariosViewModel
import com.ddcrcodelab.pontocinformatica.ui.login.LoginViewModel
import com.ddcrcodelab.pontocinformatica.ui.notificacoes.NotificacoesViewModel
import com.ddcrcodelab.pontocinformatica.ui.os.CriarOsViewModel
import com.ddcrcodelab.pontocinformatica.ui.os.DetalheOsViewModel
import com.ddcrcodelab.pontocinformatica.ui.os.ListaOsViewModel
import com.ddcrcodelab.pontocinformatica.ui.pedidos.PedidosViewModel
import com.ddcrcodelab.pontocinformatica.ui.perfil.MeuPerfilViewModel
import com.ddcrcodelab.pontocinformatica.util.GeradorPdf
import com.ddcrcodelab.pontocinformatica.util.SessaoManager
import com.ddcrcodelab.pontocinformatica.util.VerificadorAtualizacao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }
    single<OsRepository> { FirestoreOsRepository(get(), get()) }
    single<UsuarioRepository> { FirestoreUsuarioRepository(get(), get()) }
    single<PedidosNotificacoesRepository> { FirestorePedidosNotificacoesRepository(get()) }
    single { GeradorPdf(get()) }
    single { SessaoManager(get()) }
    single { VerificadorAtualizacao(get()) }

    viewModel { LoginViewModel(get()) }
    viewModel { ListaOsViewModel(get(), get()) }
    viewModel { CriarOsViewModel(get()) }
    viewModel { DetalheOsViewModel(get(), get(), get()) }
    viewModel { FuncionariosViewModel(get()) }
    viewModel { MeuPerfilViewModel(get(), get()) }
    viewModel { PedidosViewModel(get(), get()) }
    viewModel { NotificacoesViewModel(get(), get()) }
}