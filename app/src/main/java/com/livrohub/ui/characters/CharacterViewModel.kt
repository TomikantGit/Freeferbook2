package com.livrohub.ui.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livrohub.domain.model.Character
import com.livrohub.domain.repository.CharacterRepository
import com.livrohub.ui.common.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado da tela de catalogo de personagens.
 *
 * @param characters Lista de personagens do livro.
 * @param isLoading Se os dados estao sendo carregados.
 */
data class CharactersUiState(
    val characters: List<Character> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * ViewModel que gerencia a aba de personagens do workspace.
 *
 * Observa os personagens reativamente. Permite criar/atualizar e excluir personagens.
 */
class CharacterViewModel(
    private val bookId: Long,
    private val repository: CharacterRepository
) : ViewModel() {

    val uiState: StateFlow<CharactersUiState> = repository.observeCharacters(bookId)
        .map { CharactersUiState(characters = it) }
        .stateIn(
            scope = viewModelScope,
            started = WhileUiSubscribed,
            initialValue = CharactersUiState(isLoading = true)
        )

    fun saveCharacter(character: Character) {
        viewModelScope.launch {
            repository.saveCharacter(character.copy(bookId = bookId))
        }
    }

    fun deleteCharacter(character: Character) {
        viewModelScope.launch {
            repository.deleteCharacter(character)
        }
    }
}
