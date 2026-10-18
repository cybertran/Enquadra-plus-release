package com.cybertran.enquadraplus.data.repository

import com.cybertran.enquadraplus.data.local.NormativeActEntity
import com.cybertran.enquadraplus.data.local.NormativeDao
import com.cybertran.enquadraplus.domain.model.InfractionNormativeLink
import com.cybertran.enquadraplus.domain.model.NormativeAct
import com.cybertran.enquadraplus.domain.repository.NormativeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineNormativeRepository(
    private val dao: NormativeDao,
) : NormativeRepository {

    override fun observeAll(): Flow<List<NormativeAct>> =
        dao.observeAll().map { items -> items.map { it.toDomain() } }

    override fun search(query: String): Flow<List<NormativeAct>> {
        val ftsQuery = query.toFtsQuery()
        return if (ftsQuery.isBlank()) {
            observeAll()
        } else {
            dao.searchFts(ftsQuery)
                .map { items -> items.map { it.toDomain() } }
        }
    }

    override suspend fun findById(id: String): NormativeAct? {
        val act = dao.findById(id) ?: return null
        val fullText = dao.findFullTextByActId(id)?.text
        return act.toDomain(fullText)
    }

    override suspend fun findByReference(
        actType: String,
        number: Int,
        year: Int,
    ): NormativeAct? =
        dao.findByReference(actType, number, year)?.toDomain()

    override suspend fun findForInfraction(
        infractionCode: String,
    ): List<InfractionNormativeLink> =
        dao.findLinksForInfraction(infractionCode).mapNotNull { link ->
            val act = dao.findById(link.actId) ?: return@mapNotNull null
            InfractionNormativeLink(
                act = act.toDomain(),
                relationKind = link.relationKind,
                evidence = link.evidence,
            )
        }
}

private fun String.toFtsQuery(): String =
    Regex("[\\p{L}\\p{N}]+")
        .findAll(lowercase())
        .map { it.value }
        .filter { it.isNotBlank() }
        .joinToString(" AND ") { "$it*" }

private fun NormativeActEntity.toDomain(
    fullText: String? = null,
): NormativeAct =
    NormativeAct(
        id = id,
        agency = agency,
        actType = actType,
        number = number,
        numberLabel = numberLabel,
        year = year,
        actDate = actDate,
        publicationDate = publicationDate,
        subject = subject,
        status = status,
        officialUrl = officialUrl,
        sourcePageUrl = sourcePageUrl,
        lastCheckedAt = lastCheckedAt,
        fullText = fullText,
    )
