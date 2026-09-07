package com.livrohub.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livrohub.domain.model.ChapterVersion
import com.livrohub.domain.repository.ChapterRepository
import com.livrohub.ui.common.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado da tela de historico.
 *
 * @param bookTitle Titulo do livro atual.
 * @param versions Lista cronologica reversa (mais recente primeiro) de versoes.
 * @param isLoading Se os dados estao sendo carregados.
 * @param notice Mensagem efemera (snackbar) a ser exibida.
 * @param errorMessage Mensagem de erro caso a carga falhe.
 */
data class HistoryUiState(
    val bookTitle: String = "",
    val versions: List<ChapterVersion> = emptyList(),
    val isLoading: Boolean = true,
    val notice: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel que gerencia a exibicao e interacoes do historico de versoes de um capitulo.
 *
 * Permite restaurar versoes (criando uma nova entrada no topo do historico).
 */
class HistoryViewModel(
    private val chapterId: Long,
    private val repository: ChapterRepository
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeChapter(chapterId),
        repository.observeVersions(chapterId)
    ) { chapter, versions ->
        HistoryUiState(
            bookTitle = chapter?.title.orEmpty(),
            versions = versions,
            isLoading = false,
            errorMessage = if (chapter == null) "Capitulo nao encontrado." else null
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileUiSubscribed,
        initialValue = HistoryUiState()
    )

    fun restoreVersion(version: ChapterVersion) {
        viewModelScope.launch {
            runCatching {
                repository.saveVersion(
                    chapterId = chapterId,
                    content = version.content,
                    message = "Restaurado da versao #${version.sequenceNumber}"
                )
            }
        }
    }
}
