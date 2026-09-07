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
import com.livrohub.domain.worldbuilding.ChapterMentionSynchronizer

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Container de injeção de dependências manual do LivroHub.
 *
 * Centraliza a criação do banco de dados Room, DAOs e repositórios.
 * Cada repositório é instanciado uma única vez (singleton no escopo do Application).
 *
 * @param context Application context utilizado para criar o banco de dados e DataStore.
 */
class AppContainer(context: Context) : AppDependencies {

    private val appContext = context.applicationContext

    private val database: LivroHubDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Room.databaseBuilder(
            appContext,
            LivroHubDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    /** Repositório para operações CRUD de livros. */
    override val bookRepository: BookRepository by lazy {
        OfflineBookRepository(database.bookDao())
    }

    /** Repositório para operações CRUD de capítulos e suas versões. */
    override val chapterRepository: ChapterRepository by lazy {
        OfflineChapterRepository(database)
    }

    /** Repositório para operações CRUD de personagens. */
    override val characterRepository: CharacterRepository by lazy {
        OfflineCharacterRepository(database.characterDao())
    }

    /** Repositório para operações de imagens. */
    override val imageRepository: ImageRepository by lazy {
        OfflineImageRepository(database)
    }

    /** Repositório para operações CRUD de locais. */
    override val locationRepository: LocationRepository by lazy {
        OfflineLocationRepository(database.locationDao())
    }

    override val chapterMentionSynchronizer: ChapterMentionSynchronizer by lazy {
        ChapterMentionSynchronizer(
            characterRepository = characterRepository,
            locationRepository = locationRepository
        )
    }

    /** Repositório para preferências do usuário (tema, fonte, etc). */
    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(appContext.dataStore)
    }

    private companion object {
        const val DATABASE_NAME = "livrohub.db"
    }
}
