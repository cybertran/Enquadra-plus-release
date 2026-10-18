package com.cybertran.enquadraplus.data.repository

import com.cybertran.enquadraplus.data.user.FavoriteInfractionEntity
import com.cybertran.enquadraplus.data.user.InfractionAnnotationEntity
import com.cybertran.enquadraplus.data.user.InfractionHistoryEntity
import com.cybertran.enquadraplus.data.user.UserInfractionDao
import com.cybertran.enquadraplus.domain.model.InfractionHistoryEntry
import com.cybertran.enquadraplus.domain.repository.UserInfractionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineUserInfractionRepository(
    private val dao: UserInfractionDao,
) : UserInfractionRepository {

    override fun observeFavoriteCodes(): Flow<Set<String>> =
        dao.observeFavoriteCodes().map { it.toSet() }

    override suspend fun toggleFavorite(code: String) {
        if (dao.isFavorite(code)) {
            dao.deleteFavorite(code)
        } else {
            dao.upsertFavorite(
                FavoriteInfractionEntity(
                    code = code,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override fun observeRecentViews(limit: Int): Flow<List<InfractionHistoryEntry>> =
        dao.observeRecentViews(limit).map { entries ->
            entries.map {
                InfractionHistoryEntry(
                    code = it.code,
                    viewedAt = it.viewedAt,
                )
            }
        }

    override suspend fun recordViewed(code: String) {
        dao.upsertHistory(
            InfractionHistoryEntity(
                code = code,
                viewedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun getAnnotation(code: String): String? =
        dao.getAnnotation(code)

    override suspend fun saveAnnotation(code: String, text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) {
            dao.deleteAnnotation(code)
        } else {
            dao.upsertAnnotation(
                InfractionAnnotationEntity(
                    code = code,
                    text = normalized,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}
