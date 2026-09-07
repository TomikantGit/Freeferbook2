package com.livrohub.ui.diff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.livrohub.domain.diff.DiffLine
import com.livrohub.domain.diff.DiffLineType
import com.livrohub.domain.diff.TextDiffEngine
import com.livrohub.domain.repository.ChapterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Estado da tela de diff.
 *
 * @param title Titulo do capitulo sendo comparado.
 * @param comparedVersionLabel Label da versao base (geralmente "Ultima versao").
 * @param targetLabel Label do texto alvo (geralmente "texto atual").
 * @param lines Linhas resultantes da diferenca.
 * @param addedCount Contagem total de palavras adicionadas.
 * @param removedCount Contagem total de palavras removidas.
 * @param isLoading Se os dados estao sendo calculados.
 */
data class DiffUiState(
    val title: String = "",
    val comparedVersionLabel: String = "Ultima versao",
    val targetLabel: String = "texto atual",
    val lines: List<DiffLine> = emptyList(),
    val addedCount: Int = 0,
    val removedCount: Int = 0,
    val isLoading: Boolean = true
)

/**
 * ViewModel que gerencia a tela de diff dinamico.
 *
 * O diff dinamico compara o texto atual (nao salvo) do editor
 * com a ultima versao salva no banco de dados.
 */
class DiffViewModel(
    chapterId: Long,
    currentContent: String,
    repository: ChapterRepository,
    private val diffEngine: TextDiffEngine = TextDiffEngine()
) : ViewModel() {
    val uiState: StateFlow<DiffUiState> = combine(
        repository.observeChapter(chapterId),
        repository.observeLatestVersion(chapterId)
    ) { chapter, latestVersion ->
        val oldContent = latestVersion?.content.orEmpty()
        val lines = diffEngine.compare(oldContent, currentContent)

        DiffUiState(
            title = chapter?.title.orEmpty(),
            comparedVersionLabel = latestVersion?.let { "Versao #${it.sequenceNumber}" }
                ?: "Sem versao salva",
            targetLabel = "texto atual",
            lines = lines,
            addedCount = lines.count { it.type == DiffLineType.Added },
            removedCount = lines.count { it.type == DiffLineType.Removed },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiffUiState()
    )
}

class DiffViewModelFactory(
    private val chapterId: Long,
    private val currentContent: String,
    private val repository: ChapterRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiffViewModel::class.java)) {
            return DiffViewModel(
                chapterId = chapterId,
                currentContent = currentContent,
                repository = repository
            ) as T
        }
        throw IllegalArgumentException("ViewModel desconhecido: ${modelClass.name}")
    }
}
