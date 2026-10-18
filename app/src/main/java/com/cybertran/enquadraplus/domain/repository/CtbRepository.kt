package com.cybertran.enquadraplus.domain.repository

import com.cybertran.enquadraplus.domain.model.CtbArticle
import kotlinx.coroutines.flow.Flow

interface CtbRepository {
    fun observeAll(): Flow<List<CtbArticle>>

    fun search(query: String): Flow<List<CtbArticle>>

    suspend fun findById(id: String): CtbArticle?
}
