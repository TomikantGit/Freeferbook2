package com.livrohub

import android.app.Application
import com.livrohub.di.AppContainer

/**
 * Classe Application do LivroHub.
 *
 * Inicializa o [AppContainer] que gerencia todas as dependências do app
 * (banco de dados, repositórios, DataStore).
 */
class LivroHubApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
