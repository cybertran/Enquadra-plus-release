package com.cybertran.enquadraplus.domain.repository

import com.cybertran.enquadraplus.domain.model.TrafficInfraction
import kotlinx.coroutines.flow.Flow

interface TrafficRepository {
    fun observeAll(): Flow<List<TrafficInfraction>>

    fun search(query: String): Flow<List<TrafficInfraction>>

    suspend fun findByCode(code: String): TrafficInfraction?
}
