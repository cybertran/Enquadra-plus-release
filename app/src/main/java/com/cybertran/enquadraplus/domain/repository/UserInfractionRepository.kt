package com.cybertran.enquadraplus.domain.repository

import com.cybertran.enquadraplus.domain.model.InfractionHistoryEntry
import kotlinx.coroutines.flow.Flow

interface UserInfractionRepository {
    fun observeFavoriteCodes(): Flow<Set<String>>

    suspend fun toggleFavorite(code: String)

    fun observeRecentViews(limit: Int): Flow<List<InfractionHistoryEntry>>

    suspend fun recordViewed(code: String)

    suspend fun getAnnotation(code: String): String?

    suspend fun saveAnnotation(code: String, text: String)
}
