package com.cybertran.enquadraplus.domain.repository

import com.cybertran.enquadraplus.domain.model.InfractionNormativeLink
import com.cybertran.enquadraplus.domain.model.NormativeAct
import kotlinx.coroutines.flow.Flow

interface NormativeRepository {
    fun observeAll(): Flow<List<NormativeAct>>

    fun search(query: String): Flow<List<NormativeAct>>

    suspend fun findById(id: String): NormativeAct?

    suspend fun findByReference(
        actType: String,
        number: Int,
        year: Int,
    ): NormativeAct?

    suspend fun findForInfraction(
        infractionCode: String,
    ): List<InfractionNormativeLink>
}
