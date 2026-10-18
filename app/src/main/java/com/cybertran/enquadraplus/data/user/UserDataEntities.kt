package com.cybertran.enquadraplus.data.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_infraction_favorites")
data class FavoriteInfractionEntity(
    @PrimaryKey
    val code: String,
    val createdAt: Long,
)

@Entity(tableName = "user_infraction_history")
data class InfractionHistoryEntity(
    @PrimaryKey
    val code: String,
    val viewedAt: Long,
)

@Entity(tableName = "user_infraction_annotations")
data class InfractionAnnotationEntity(
    @PrimaryKey
    val code: String,
    val text: String,
    val updatedAt: Long,
)
