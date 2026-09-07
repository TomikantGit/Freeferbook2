package com.livrohub.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livrohub.domain.repository.ChapterRepository
import com.livrohub.domain.worldbuilding.ChapterMentionSynchronizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.launch

/**
 * Estado da tela do editor de capítulo.
 *
 * @param chapterTitle Título do capítulo sendo editado.
 * @param editorText Conteúdo atual no campo de texto.
 * @param latestSequenceNumber Número da última versão salva (exibido na toolbar).
 * @param isLoading Se está carregando dados do banco.
 * @param isSaving Se está salvando uma nova versão.
 * @param hasUnsavedChanges Se há alterações não salvas desde a última versão.
 * @param errorMessage Mensagem de erro (null se sem erro).
 * @param lastSavedNotice Mensagem de confirmação de salvamento (null após exibir).
 */
data class EditorUiState(
    val chapterTitle: String = "",
    val editorText: TextFieldValue = TextFieldValue(""),
    val latestSequenceNumber: Int? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val errorMessage: String? = null,
    val lastSavedNotice: String? = null
)

/**
 * ViewModel do editor de capítulo.
 *
 * Observa reativamente o capítulo e sua última versão salva via [ChapterRepository].
 * Quando não há alterações não salvas, atualiza o conteúdo do editor automaticamente
 * ao detectar uma nova versão (ex: após restauração no histórico).
 *
 * @param chapterId ID do capítulo sendo editado.
 * @param bookId ID do livro ao qual o capítulo pertence.
 * @param repository Repositório para operações de capítulo e versões.
 * @param mentionSynchronizer Serviço de domínio que sincroniza referências de worldbuilding.
 */
class EditorViewModel(
    private val chapterId: Long,
    private val bookId: Long,
    private val repository: ChapterRepository,
    private val mentionSynchronizer: ChapterMentionSynchronizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    /** ID da versão atualmente carregada no editor (para evitar recargas desnecessárias). */
    private var loadedVersionId: Long? = null

    init {
        viewModelScope.launch {
            combine(
                repository.observeChapter(chapterId),
                repository.observeLatestVersion(chapterId)
            ) { chapter, latestVersion ->
                chapter to latestVersion
            }.collect { (chapter, latestVersion) ->
                _uiState.update { current ->
                    val latestId = latestVersion?.id
                    val shouldLoadContent =
                        latestId != null &&
                            latestId != loadedVersionId &&
                            !current.hasUnsavedChanges

                    if (shouldLoadContent) {
                        loadedVersionId = latestId
                    }

                    current.copy(
                        chapterTitle = chapter?.title.orEmpty(),
                        editorText = if (shouldLoadContent) {
                            TextFieldValue(latestVersion?.content.orEmpty())
                        } else {
                            current.editorText
                        },
                        latestSequenceNumber = latestVersion?.sequenceNumber,
                        isLoading = false,
                        errorMessage = if (chapter == null) {
                            "Capitulo nao encontrado."
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    /**
     * Atualiza o texto do editor.
     *
     * Marca o estado como tendo alterações não salvas e limpa
     * qualquer notificação de salvamento anterior.
     * O wrap de texto é feito visualmente pelo Compose — não há
     * manipulação forçada do conteúdo digitado.
     *
     * @param text Novo conteúdo do campo de texto.
     */
    fun updateText(text: TextFieldValue) {
        _uiState.update { current ->
            current.copy(
                editorText = text,
                hasUnsavedChanges = true,
                lastSavedNotice = null
            )
        }
    }

    /**
     * Salva o conteúdo atual como uma nova versão.
     *
     * Cria uma nova entrada em `chapter_versions` preservando o histórico.
     * Em caso de sucesso, marca o estado como sem alterações não salvas.
     *
     * @param message Mensagem opcional descrevendo as alterações.
     */
    fun saveVersion(message: String?) {
        val content = _uiState.value.editorText.text
        val currentTitle = _uiState.value.chapterTitle
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            runCatching {
                repository.saveVersion(
                    chapterId = chapterId,
                    content = content,
                    message = message
                )
            }.onSuccess { versionId ->
                loadedVersionId = versionId
                _uiState.update { current ->
                    current.copy(
                        isSaving = false,
                        hasUnsavedChanges = false,
                        lastSavedNotice = "Versao salva."
                    )
                }
                mentionSynchronizer.synchronize(
                    bookId = bookId,
                    chapterTitle = currentTitle,
                    content = content
                )
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Nao foi possivel salvar a versao."
                    )
                }
            }
        }
    }
    /** Limpa a notificação de salvamento (chamado após exibir o Snackbar). */
    fun clearSavedNotice() {
        _uiState.update { it.copy(lastSavedNotice = null) }
    }
}
