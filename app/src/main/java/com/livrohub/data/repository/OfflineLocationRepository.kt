package com.livrohub.data.repository

import com.livrohub.data.local.LocationDao
import com.livrohub.data.local.toEntity
import com.livrohub.domain.model.Location
import com.livrohub.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineLocationRepository(
    private val locationDao: LocationDao
) : LocationRepository {

    override fun observeLocations(bookId: Long): Flow<List<Location>> {
        return locationDao.getLocationsByBook(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveLocation(location: Location): Long {
        return if (location.id == 0L) {
            locationDao.insertLocation(location.toEntity())
        } else {
            locationDao.updateLocation(location.toEntity())
            location.id
        }
    }

    override suspend fun deleteLocation(location: Location) {
        locationDao.deleteLocation(location.toEntity())
    }
}
