package com.cybertran.enquadraplus.data.repository

import com.cybertran.enquadraplus.data.local.CtbArticleEntity
import com.cybertran.enquadraplus.data.local.CtbDao
import com.cybertran.enquadraplus.domain.model.CtbArticle
import com.cybertran.enquadraplus.domain.repository.CtbRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineCtbRepository(
    private val dao: CtbDao,
) : CtbRepository {

    override fun observeAll(): Flow<List<CtbArticle>> =
        dao.observeAll().map { items -> items.map(CtbArticleEntity::toDomain) }

    override fun search(query: String): Flow<List<CtbArticle>> {
        val ftsQuery = query.toFtsQuery()

        return if (ftsQuery.isBlank()) {
            observeAll()
        } else {
            dao.searchFts(ftsQuery)
                .map { items -> items.map(CtbArticleEntity::toDomain) }
        }
    }

    override suspend fun findById(id: String): CtbArticle? =
        dao.findById(id)?.toDomain()
}

private fun String.toFtsQuery(): String =
    Regex("[\\p{L}\\p{N}]+")
        .findAll(lowercase())
        .map { it.value }
        .filter { it.isNotBlank() }
        .joinToString(" AND ") { "$it*" }

private fun CtbArticleEntity.toDomain(): CtbArticle =
    CtbArticle(
        id = id,
        label = label,
        number = number,
        suffix = suffix,
        chapter = chapter,
        section = section,
        subsection = subsection,
        text = text,
        sourceUrl = sourceUrl,
        lastCheckedAt = lastCheckedAt,
    )
