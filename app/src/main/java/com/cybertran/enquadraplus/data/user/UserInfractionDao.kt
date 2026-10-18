package com.cybertran.enquadraplus.data.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserInfractionDao {

    @Query("SELECT code FROM user_infraction_favorites ORDER BY createdAt DESC")
    fun observeFavoriteCodes(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM user_infraction_favorites WHERE code = :code)")
    suspend fun isFavorite(code: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(entity: FavoriteInfractionEntity)

    @Query("DELETE FROM user_infraction_favorites WHERE code = :code")
    suspend fun deleteFavorite(code: String)

    @Query(
        "SELECT * FROM user_infraction_history " +
            "ORDER BY viewedAt DESC LIMIT :limit",
    )
    fun observeRecentViews(limit: Int): Flow<List<InfractionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entity: InfractionHistoryEntity)

    @Query("SELECT text FROM user_infraction_annotations WHERE code = :code LIMIT 1")
    suspend fun getAnnotation(code: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnnotation(entity: InfractionAnnotationEntity)

    @Query("DELETE FROM user_infraction_annotations WHERE code = :code")
    suspend fun deleteAnnotation(code: String)
}
