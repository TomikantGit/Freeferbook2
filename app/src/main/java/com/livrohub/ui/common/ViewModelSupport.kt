package com.livrohub.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.SharingStarted

/** Política única de compartilhamento de StateFlow para telas observáveis. */
val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(5_000)

/**
 * Factory mínima para ViewModels com dependências de runtime.
 *
 * Evita uma classe `FooViewModelFactory` por ViewModel sem introduzir framework
 * de DI. Quando o projeto crescer, este ponto pode ser substituído por Hilt/Koin
 * sem alterar as telas que apenas recebem a factory.
 */
fun <VM : ViewModel> viewModelFactory(
    initializer: () -> VM
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val instance = initializer()
        require(modelClass.isInstance(instance)) {
            "Factory criou ${instance::class.java.name}, mas foi solicitado ${modelClass.name}."
        }
        return instance as T
    }
}
