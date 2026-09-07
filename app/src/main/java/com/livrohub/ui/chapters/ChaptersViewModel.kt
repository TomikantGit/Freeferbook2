package com.livrohub.ui.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livrohub.domain.model.Chapter
import com.livrohub.domain.repository.ChapterRepository
import com.livrohub.ui.common.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
            started = WhileUiSubscribed,
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
        val latestVersion = repository.getLatestVersion(chapterId)
        return latestVersion?.content ?: ""
    }
}
