package com.cybertran.enquadraplus.data.repository

import com.cybertran.enquadraplus.data.local.InfractionDao
import com.cybertran.enquadraplus.data.local.InfractionEntity
import com.cybertran.enquadraplus.data.local.InfractionWithDetails
import com.cybertran.enquadraplus.data.local.NoteKind
import com.cybertran.enquadraplus.domain.model.InfractionClassification
import com.cybertran.enquadraplus.domain.model.InfractionContentStatus
import com.cybertran.enquadraplus.domain.model.InfractionRelationType
import com.cybertran.enquadraplus.domain.model.OfficialSourceReference
import com.cybertran.enquadraplus.domain.model.RelatedInfractionReference
import com.cybertran.enquadraplus.domain.model.TrafficInfraction
import com.cybertran.enquadraplus.domain.repository.TrafficRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class OfflineTrafficRepository(
    private val dao: InfractionDao,
) : TrafficRepository {

    override fun observeAll(): Flow<List<TrafficInfraction>> =
        dao.observeAll()
            .map { items -> items.map(InfractionEntity::toDomainSummary) }
            .flowOn(Dispatchers.Default)

    override fun search(query: String): Flow<List<TrafficInfraction>> {
        val ftsQuery = query.toInfractionFtsQuery()

        return if (ftsQuery.isBlank()) {
            observeAll()
        } else {
            dao.searchFts(ftsQuery)
                .map { items ->
                    items
                        .map(InfractionEntity::toDomainSummary)
                        .rankForInfractionSearch(query)
                }
                .flowOn(Dispatchers.Default)
        }
    }

    override suspend fun findByCode(code: String): TrafficInfraction? =
        dao.findByCode(code)?.toDomain()
}

internal fun String.toInfractionFtsQuery(): String {
    // The official code 537-10 is indexed as separate FTS tokens ("537" and "10").
    // Make continuous numeric input (53710) searchable without requiring the hyphen.
    val compactCode = trim()
    if (compactCode.matches(Regex("""\d{4,5}"""))) {
        return "${compactCode.take(3)}* AND ${compactCode.drop(3)}*"
    }
    val tokens = Regex("[\\p{L}\\p{N}]+")
        .findAll(lowercase())
        .map { it.value }
        .filter { it.isNotBlank() }
        .toList()

    if (tokens.isEmpty()) {
        return ""
    }

    val contentTokens = tokens.filterNot(String::isInfractionSearchConnector)
    val searchableTokens = contentTokens.ifEmpty { tokens }

    val stableTokens =
        if (
            searchableTokens.size > 1 &&
            searchableTokens.last().length == 1 &&
            searchableTokens.last().all(Char::isLetter)
        ) {
            searchableTokens.dropLast(1)
        } else {
            searchableTokens
        }

    return stableTokens.joinToString(" AND ") { "$it*" }
}

private fun String.isInfractionSearchConnector(): Boolean =
    lowercase() in INFRACTION_SEARCH_CONNECTORS

private val INFRACTION_SEARCH_CONNECTORS =
    setOf(
        "a",
        "ao",
        "aos",
        "as",
        "com",
        "da",
        "das",
        "de",
        "do",
        "dos",
        "e",
        "em",
        "na",
        "nas",
        "no",
        "nos",
        "o",
        "os",
        "para",
        "pela",
        "pelas",
        "pelo",
        "pelos",
        "por",
        "sem",
    )

private fun InfractionEntity.toDomainSummary(): TrafficInfraction =
    TrafficInfraction(
        id = code,
        code = code,
        article = article,
        shortDescription = shortDescription,
        description = description,
        contentStatus = contentStatus.toContentStatus(),
        classification = severity.toClassification(),
        points = points,
        offenderResponsibility = offenderResponsibility,
        authority = authority,
        verificationMethod = verificationMethod,
        penalty = penalty,
        administrativeMeasure = administrativeMeasure,
        canConfigureTrafficCrime = canConfigureTrafficCrime,
        legalBasis = listOf(article),
        whenToCite = emptyList(),
        whenNotToCite = emptyList(),
        definitionsAndProcedures = emptyList(),
        aitOfficialObservations = emptyList(),
        sourceConflicts = emptyList(),
        relatedCodes = emptyList(),
        relatedInfractions = emptyList(),
        sources = emptyList(),
    )

private fun InfractionWithDetails.toDomain(): TrafficInfraction {
    fun notesOf(kind: String): List<String> =
        notes
            .filter { it.kind == kind }
            .sortedBy { it.position }
            .map { it.text }

    return TrafficInfraction(
        id = infraction.code,
        code = infraction.code,
        article = infraction.article,
        shortDescription = infraction.shortDescription,
        description = infraction.description,
        contentStatus = infraction.contentStatus.toContentStatus(),
        classification = infraction.severity.toClassification(),
        points = infraction.points,
        offenderResponsibility = infraction.offenderResponsibility,
        authority = infraction.authority,
        verificationMethod = infraction.verificationMethod,
        penalty = infraction.penalty,
        administrativeMeasure = infraction.administrativeMeasure,
        canConfigureTrafficCrime = infraction.canConfigureTrafficCrime,
        legalBasis = legalBasis
            .sortedBy { it.position }
            .map { it.text }
            .ifEmpty { listOf(infraction.article) },
        whenToCite = notesOf(NoteKind.WHEN_TO_CITE),
        whenNotToCite = notesOf(NoteKind.WHEN_NOT_TO_CITE),
        definitionsAndProcedures = notesOf(NoteKind.PROCEDURE),
        aitOfficialObservations = notesOf(NoteKind.AIT_OBSERVATION),
        sourceConflicts = notesOf(NoteKind.SOURCE_CONFLICT),
        relatedCodes = relations.map { it.relatedCode }.distinct(),
        relatedInfractions = relations
            .sortedWith(
                compareBy(
                    { it.relationType },
                    { it.relatedCode },
                ),
            )
            .map {
                RelatedInfractionReference(
                    code = it.relatedCode,
                    type = it.relationType.toRelationType(),
                    reason = it.reason,
                    evidence = it.evidence
                        ?.split("\n\n")
                        ?.filter(String::isNotBlank)
                        .orEmpty(),
                    sourceField = it.sourceField,
                    origin = it.origin,
                )
            },
        sources = sources.map {
            OfficialSourceReference(
                agency = it.agency,
                document = it.document,
                section = it.section,
                officialUrl = it.officialUrl,
                effectiveFrom = it.effectiveFrom,
                lastCheckedAt = it.lastCheckedAt,
            )
        },
    )
}

private fun String.toContentStatus(): InfractionContentStatus =
    when (uppercase()) {
        "VERIFIED" -> InfractionContentStatus.VERIFIED
        "SOURCE_CONFLICT" -> InfractionContentStatus.SOURCE_CONFLICT
        "MANUAL_REVIEW_REQUIRED" -> InfractionContentStatus.MANUAL_REVIEW_REQUIRED
        else -> InfractionContentStatus.PARTIAL_VALIDATION
    }

private fun String.toClassification(): InfractionClassification =
    when (lowercase()) {
        "leve" -> InfractionClassification.LIGHT
        "média", "media" -> InfractionClassification.MEDIUM
        "grave" -> InfractionClassification.SERIOUS
        "gravíssima", "gravissima" -> InfractionClassification.VERY_SERIOUS
        else -> InfractionClassification.OTHER
    }


private fun String.toRelationType(): InfractionRelationType =
    when (uppercase()) {
        "ALTERNATIVE" -> InfractionRelationType.ALTERNATIVE
        "CONCOMITANT" -> InfractionRelationType.CONCOMITANT
        "PREREQUISITE" -> InfractionRelationType.PREREQUISITE
        else -> InfractionRelationType.RELATED
    }
