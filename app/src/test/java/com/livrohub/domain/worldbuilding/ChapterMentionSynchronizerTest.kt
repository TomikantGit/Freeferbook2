package com.livrohub.domain.worldbuilding

import com.livrohub.domain.model.Character
import com.livrohub.domain.model.Location
import com.livrohub.domain.repository.CharacterRepository
import com.livrohub.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterMentionSynchronizerTest {

    @Test
    fun `synchronize updates character and location references`() = runTest {
        val characters = FakeCharacterRepository(
            mutableListOf(
                Character(
                    id = 1,
                    bookId = 10,
                    name = "Lina",
                    surnames = "Silva, Vale",
                    chapters = "Prólogo",
                    imageUri = null
                )
            )
        )
        val locations = FakeLocationRepository(
            mutableListOf(
                Location(
                    id = 2,
                    bookId = 10,
                    name = "Torre Azul",
                    description = "fortaleza, penhasco",
                    chapters = "",
                    imageUri = null
                )
            )
        )
        val synchronizer = ChapterMentionSynchronizer(characters, locations)

        val result = synchronizer.synchronize(
            bookId = 10,
            chapterTitle = "Capítulo 1",
            content = "Lina atravessou o penhasco em direção à Torre Azul."
        )

        assertEquals(1, result.charactersUpdated)
        assertEquals(1, result.locationsUpdated)
        assertEquals("Prólogo, Capítulo 1", characters.items.single().chapters)
        assertEquals("Capítulo 1", locations.items.single().chapters)
    }

    @Test
    fun `synchronize does not duplicate exact chapter reference`() = runTest {
        val characters = FakeCharacterRepository(
            mutableListOf(
                Character(
                    id = 1,
                    bookId = 10,
                    name = "Lina",
                    surnames = "",
                    chapters = "Capítulo 1, Capítulo 10",
                    imageUri = null
                )
            )
        )
        val synchronizer = ChapterMentionSynchronizer(
            characters,
            FakeLocationRepository(mutableListOf())
        )

        val result = synchronizer.synchronize(
            bookId = 10,
            chapterTitle = "Capítulo 1",
            content = "Lina apareceu novamente."
        )

        assertEquals(0, result.charactersUpdated)
        assertEquals("Capítulo 1, Capítulo 10", characters.items.single().chapters)
    }

    private class FakeCharacterRepository(
        val items: MutableList<Character>
    ) : CharacterRepository {
        override fun observeCharacters(bookId: Long): Flow<List<Character>> =
            flowOf(items.filter { it.bookId == bookId })

        override suspend fun saveCharacter(character: Character): Long {
            val index = items.indexOfFirst { it.id == character.id }
            if (index >= 0) items[index] = character else items += character
            return character.id
        }

        override suspend fun deleteCharacter(character: Character) {
            items.removeAll { it.id == character.id }
        }
    }

    private class FakeLocationRepository(
        val items: MutableList<Location>
    ) : LocationRepository {
        override fun observeLocations(bookId: Long): Flow<List<Location>> =
            flowOf(items.filter { it.bookId == bookId })

        override suspend fun saveLocation(location: Location): Long {
            val index = items.indexOfFirst { it.id == location.id }
            if (index >= 0) items[index] = location else items += location
            return location.id
        }

        override suspend fun deleteLocation(location: Location) {
            items.removeAll { it.id == location.id }
        }
    }
}
