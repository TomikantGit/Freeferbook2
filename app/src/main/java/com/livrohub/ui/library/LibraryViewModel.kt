package com.livrohub.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livrohub.domain.model.BookWithStats
import com.livrohub.domain.repository.BookRepository
import com.livrohub.ui.common.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado da tela da biblioteca.
 *
 * @param books Lista de livros com suas estatisticas agregadas.
 * @param isLoading Se os dados estao sendo carregados inicialmente.
 * @param errorMessage Mensagem de erro caso a carga falhe.
 */
data class LibraryUiState(
    val books: List<BookWithStats> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * ViewModel que gerencia a tela da biblioteca de livros.
 *
 * Observa o repositorio de livros reativamente para atualizar o UI state
 * sempre que um livro for criado, renomeado ou excluido.
 */
class LibraryViewModel(
    private val repository: BookRepository
) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> = repository.observeBooksWithStats()
        .map { books ->
            LibraryUiState(
                books = books,
                isLoading = false
            )
        }
        .catch { error ->
            emit(
                LibraryUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Nao foi possivel carregar os livros."
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = WhileUiSubscribed,
            initialValue = LibraryUiState()
        )

    fun createBook(title: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return

        viewModelScope.launch {
            repository.createBook(title = cleanTitle)
        }
    }

    fun renameBook(bookId: Long, title: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return

        viewModelScope.launch {
            repository.renameBook(bookId = bookId, newTitle = cleanTitle)
        }
    }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            repository.deleteBook(bookId)
        }
    }
}
