package com.livrohub.ui.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livrohub.domain.model.Location
import com.livrohub.domain.repository.LocationRepository
import com.livrohub.ui.common.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado da tela de catalogo de locais.
 *
 * @param locations Lista de locais do livro.
 * @param isLoading Se os dados estao sendo carregados.
 */
data class LocationsUiState(
    val locations: List<Location> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * ViewModel que gerencia a aba de locais do workspace.
 *
 * Observa os locais reativamente. Permite criar/atualizar e excluir locais.
 */
class LocationViewModel(
    private val bookId: Long,
    private val repository: LocationRepository
) : ViewModel() {

    val uiState: StateFlow<LocationsUiState> = repository.observeLocations(bookId)
        .map { LocationsUiState(locations = it) }
        .stateIn(
            scope = viewModelScope,
            started = WhileUiSubscribed,
            initialValue = LocationsUiState(isLoading = true)
        )

    fun saveLocation(location: Location) {
        viewModelScope.launch {
            repository.saveLocation(location.copy(bookId = bookId))
        }
    }

    fun deleteLocation(location: Location) {
        viewModelScope.launch {
            repository.deleteLocation(location)
        }
    }
}
