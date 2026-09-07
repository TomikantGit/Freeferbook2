package com.livrohub.domain.worldbuilding

import com.livrohub.domain.repository.CharacterRepository
import com.livrohub.domain.repository.LocationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

data class MentionSyncResult(
    val charactersUpdated: Int = 0,
    val locationsUpdated: Int = 0
)

/**
 * Sincroniza referências de worldbuilding encontradas no texto de um capítulo.
 *
 * A regra não pertence ao ViewModel do editor: qualquer futura importação, revisão
 * ou automação que salve capítulos pode reutilizar o mesmo serviço.
 * Falhas são tratadas como não críticas porque a versão do capítulo já pode ter sido salva.
 */
class ChapterMentionSynchronizer(
    private val characterRepository: CharacterRepository,
    private val locationRepository: LocationRepository
) {

    suspend fun synchronize(
        bookId: Long,
        chapterTitle: String,
        content: String
    ): MentionSyncResult {
        if (content.isBlank() || chapterTitle.isBlank()) return MentionSyncResult()

        return coroutineScope {
            val characters = async {
                runCatching {
                    synchronizeCharacters(bookId, chapterTitle, content)
                }.getOrDefault(0)
            }
            val locations = async {
                runCatching {
                    synchronizeLocations(bookId, chapterTitle, content)
                }.getOrDefault(0)
            }

            MentionSyncResult(
                charactersUpdated = characters.await(),
                locationsUpdated = locations.await()
            )
        }
    }

    private suspend fun synchronizeCharacters(
        bookId: Long,
        chapterTitle: String,
        content: String
    ): Int {
        var updated = 0
        characterRepository.observeCharacters(bookId).first().forEach { character ->
            val terms = buildList {
                character.name.takeIf(String::isNotBlank)?.let(::add)
                character.surnames
                    .split(',')
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .forEach(::add)
            }

            if (content.containsAny(terms) &&
                !character.chapters.containsChapterReference(chapterTitle)
            ) {
                characterRepository.saveCharacter(
                    character.copy(
                        chapters = character.chapters.appendChapterReference(chapterTitle)
                    )
                )
                updated++
            }
        }
        return updated
    }

    private suspend fun synchronizeLocations(
        bookId: Long,
        chapterTitle: String,
        content: String
    ): Int {
        var updated = 0
        locationRepository.observeLocations(bookId).first().forEach { location ->
            val terms = buildList {
                location.name.takeIf(String::isNotBlank)?.let(::add)
                // Mantém a regra histórica em que trechos separados por vírgula na
                // descrição também funcionam como termos de detecção.
                location.description
                    .split(',')
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .forEach(::add)
            }

            if (content.containsAny(terms) &&
                !location.chapters.containsChapterReference(chapterTitle)
            ) {
                locationRepository.saveLocation(
                    location.copy(
                        chapters = location.chapters.appendChapterReference(chapterTitle)
                    )
                )
                updated++
            }
        }
        return updated
    }

    private fun String.containsAny(terms: Iterable<String>): Boolean =
        terms.any { term -> contains(term, ignoreCase = true) }

    private fun String.containsChapterReference(chapterTitle: String): Boolean =
        split(',')
            .map(String::trim)
            .any { it.equals(chapterTitle, ignoreCase = true) }

    private fun String.appendChapterReference(chapterTitle: String): String =
        if (isBlank()) chapterTitle else "$this, $chapterTitle"
}
