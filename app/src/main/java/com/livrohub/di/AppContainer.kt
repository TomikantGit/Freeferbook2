package com.livrohub.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.livrohub.data.local.LivroHubDatabase
import com.livrohub.data.repository.OfflineBookRepository
import com.livrohub.data.repository.OfflineChapterRepository
import com.livrohub.data.repository.OfflineCharacterRepository
import com.livrohub.data.repository.OfflineImageRepository
import com.livrohub.data.repository.OfflineLocationRepository
import com.livrohub.data.repository.SettingsRepositoryImpl
import com.livrohub.domain.repository.BookRepository
import com.livrohub.domain.repository.ChapterRepository
import com.livrohub.domain.repository.CharacterRepository
import com.livrohub.domain.repository.ImageRepository
import com.livrohub.domain.repository.LocationRepository
import com.livrohub.domain.repository.SettingsRepository

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Container de injeção de dependências manual do LivroHub.
 *
 * Centraliza a criação do banco de dados Room, DAOs e repositórios.
 * Cada repositório é instanciado uma única vez (singleton no escopo do Application).
 *
 * @param context Application context utilizado para criar o banco de dados e DataStore.
 */
class AppContainer(context: Context) {

    private val database: LivroHubDatabase = Room.databaseBuilder(
        context.applicationContext,
        LivroHubDatabase::class.java,
        "livrohub.db"
    )
        .fallbackToDestructiveMigration()
        .build()

    /** Repositório para operações CRUD de livros. */
    val bookRepository: BookRepository = OfflineBookRepository(database)

    /** Repositório para operações CRUD de capítulos e suas versões. */
    val chapterRepository: ChapterRepository = OfflineChapterRepository(database)

    /** Repositório para operações CRUD de personagens. */
    val characterRepository: CharacterRepository = OfflineCharacterRepository(database.characterDao())

    /** Repositório para operações de imagens. */
    val imageRepository: ImageRepository = OfflineImageRepository(database)

    /** Repositório para operações CRUD de locais. */
    val locationRepository: LocationRepository = OfflineLocationRepository(database.locationDao())

    /** Repositório para preferências do usuário (tema, fonte, etc). */
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(context.dataStore)
}
