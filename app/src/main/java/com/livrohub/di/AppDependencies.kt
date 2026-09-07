package com.livrohub.di

import com.livrohub.data.archive.BookArchiveManager
import com.livrohub.domain.repository.BookRepository
import com.livrohub.domain.repository.ChapterRepository
import com.livrohub.domain.repository.CharacterRepository
import com.livrohub.domain.repository.ImageRepository
import com.livrohub.domain.repository.LocationRepository
import com.livrohub.domain.repository.SettingsRepository
import com.livrohub.domain.worldbuilding.ChapterMentionSynchronizer

/**
 * Contrato das dependências de longa duração do aplicativo.
 *
 * A UI depende deste contrato em vez da implementação concreta de [AppContainer].
 * Isso mantém o ponto de entrada estável quando novos repositórios/serviços forem
 * adicionados e facilita a substituição por fakes em previews e testes futuros.
 */
interface AppDependencies {
    val bookRepository: BookRepository
    val chapterRepository: ChapterRepository
    val settingsRepository: SettingsRepository
    val characterRepository: CharacterRepository
    val imageRepository: ImageRepository
    val locationRepository: LocationRepository
    val chapterMentionSynchronizer: ChapterMentionSynchronizer
    val bookArchiveManager: BookArchiveManager
}
