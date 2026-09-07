package com.livrohub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.livrohub.ui.LivroHubAppRoot

/**
 * Activity principal e único ponto de entrada do app.
 *
 * Obtém o [AppContainer] do [LivroHubApp] e injeta os repositórios
 * no composable raiz [LivroHubAppRoot].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as LivroHubApp
        setContent {
            LivroHubAppRoot(
                bookRepository = app.container.bookRepository,
                chapterRepository = app.container.chapterRepository,
                settingsRepository = app.container.settingsRepository,
                characterRepository = app.container.characterRepository,
                imageRepository = app.container.imageRepository,
                locationRepository = app.container.locationRepository
            )
        }
    }
}
