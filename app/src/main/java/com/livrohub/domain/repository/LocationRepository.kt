package com.livrohub.domain.repository

import com.livrohub.domain.model.Location
import kotlinx.coroutines.flow.Flow

/**
 * Contrato para operações CRUD de locais (cenários) de um livro.
 */
interface LocationRepository {
    /** Observa a lista de locais de um determinado livro. */
    fun observeLocations(bookId: Long): Flow<List<Location>>
    /** Salva um local (cria se o ID for 0, atualiza caso contrário) e retorna seu ID. */
    suspend fun saveLocation(location: Location): Long
    /** Exclui um local do banco de dados. */
    suspend fun deleteLocation(location: Location)
}
