package com.livrohub.ui.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.livrohub.domain.model.Chapter
import com.livrohub.domain.repository.ChapterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

/**
 * Estado da tela de lista de capitulos.
 *
 * @param chapters Lista ordenada de capitulos do livro.
 * @param isLoading Se os dados estao sendo carregados.
 * @param errorMessage Mensagem de erro caso a carga falhe.
 */
data class ChaptersUiState(
    val chapters: List<Chapter> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * ViewModel que gerencia a aba de capitulos do workspace.
 *
 * Observa os capitulos reativamente. Permite criar, renomear e excluir capitulos.
 */
class ChaptersViewModel(
    private val bookId: Long,
    private val repository: ChapterRepository
) : ViewModel() {
    val uiState: StateFlow<ChaptersUiState> = repository.observeChapters(bookId)
        .map { chapters ->
            ChaptersUiState(
                chapters = chapters,
                isLoading = false
            )
        }
        .catch { error ->
            emit(
                ChaptersUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Erro ao carregar capitulos."
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChaptersUiState()
        )

    fun createChapter(title: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return

        viewModelScope.launch {
            repository.createChapter(bookId = bookId, title = cleanTitle)
        }
    }

    fun renameChapter(chapterId: Long, title: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return

        viewModelScope.launch {
            repository.renameChapter(chapterId = chapterId, newTitle = cleanTitle)
        }
    }

    fun deleteChapter(chapterId: Long) {
        viewModelScope.launch {
            repository.deleteChapter(chapterId)
        }
    }

    suspend fun getChapterContent(chapterId: Long): String {
        val latestVersion = repository.observeLatestVersion(chapterId).firstOrNull()
        return latestVersion?.content ?: ""
    }
}

class ChaptersViewModelFactory(
    private val bookId: Long,
    private val repository: ChapterRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChaptersViewModel::class.java)) {
            return ChaptersViewModel(bookId, repository) as T
        }
        throw IllegalArgumentException("ViewModel desconhecido")
    }
}
