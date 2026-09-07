package com.livrohub.ui.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livrohub.domain.model.ImageReference
import com.livrohub.domain.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImagesUiState(
    val images: List<ImageReference> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ImagesViewModel(
    private val bookId: Long,
    private val repository: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagesUiState())
    val uiState: StateFlow<ImagesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeImages(bookId).collect { images ->
                _uiState.update { it.copy(images = images, isLoading = false) }
            }
        }
    }

    fun addImage(url: String, description: String = "") {
        if (url.isBlank()) return
        viewModelScope.launch {
            try {
                repository.addImage(
                    ImageReference(
                        bookId = bookId,
                        url = url,
                        description = description
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erro ao adicionar imagem: ${e.message}") }
            }
        }
    }

    fun deleteImage(image: ImageReference) {
        viewModelScope.launch {
            try {
                repository.deleteImage(image)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erro ao deletar imagem: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
