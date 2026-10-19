package com.cybertran.enquadraplus.data.update

import androidx.room.withTransaction
import com.cybertran.enquadraplus.data.local.CtbArticleEntity
import com.cybertran.enquadraplus.data.local.CtbArticleSearchEntity
import com.cybertran.enquadraplus.data.local.DatasetMetadataEntity
import com.cybertran.enquadraplus.data.local.EnquadraDatabase
import com.cybertran.enquadraplus.data.local.NormativeActEntity
import com.cybertran.enquadraplus.data.local.NormativeActSearchEntity
import com.cybertran.enquadraplus.data.local.InfractionEntity
import com.cybertran.enquadraplus.data.local.InfractionLegalBasisEntity
import com.cybertran.enquadraplus.data.local.InfractionNoteEntity
import com.cybertran.enquadraplus.data.local.InfractionRelationEntity
import com.cybertran.enquadraplus.data.local.InfractionSearchEntity
import com.cybertran.enquadraplus.data.local.InfractionSourceCrossRef
import com.cybertran.enquadraplus.data.local.OfficialSourceEntity

class SafeDatasetPromoter(
    private val database: EnquadraDatabase,
) {

    suspend fun promote(
        dataset: UpdateDataset,
        json: String,
    ): String =
        when (dataset) {
            UpdateDataset.CTB -> promoteCtb(json)
            UpdateDataset.NORMATIVE_CATALOG -> promoteNormativeCatalog(json)
            UpdateDataset.MBFT -> promoteMbft(json)
        }

    private suspend fun promoteCtb(json: String): String {
        val payload = CtbUpdateCodec.parse(json)
        CtbUpdateCodec.validate(payload).requireValid("Snapshot CTB inválido")

        database.withTransaction {
            val dao = database.ctbDao()
            dao.clearSearchIndex()
            dao.clearArticles()
            dao.clearDatasetMetadata(UpdateDataset.CTB.id)

            dao.insertArticles(
                payload.records.map { record ->
                    CtbArticleEntity(
                        id = record.id,
                        label = record.label,
                        number = record.number,
                        suffix = record.suffix,
                        chapter = record.chapter,
                        section = record.section,
                        subsection = record.subsection,
                        text = record.text,
                        sourceUrl = payload.sourceUrl,
                        lastCheckedAt = payload.verifiedAt,
                    )
                },
            )
            dao.insertSearchEntries(
                payload.records.map { record ->
                    CtbArticleSearchEntity(
                        articleId = record.id,
                        label = record.label,
                        context = listOfNotNull(
                            record.chapter,
                            record.section,
                            record.subsection,
                        ).joinToString(" "),
                        text = record.text,
                    )
                },
            )
            dao.insertDatasetMetadata(
                DatasetMetadataEntity(
                    datasetId = UpdateDataset.CTB.id,
                    version = payload.version,
                    generatedAt = payload.generatedAt,
                    verifiedAt = payload.verifiedAt,
                    sourceDocument = payload.sourceDocument,
                    sourceUrl = payload.sourceUrl,
                    knownAmendments = "",
                    recordCount = payload.recordCount,
                ),
            )
        }

        return payload.version
    }

    private suspend fun promoteMbft(json: String): String {
        val payload = MbftUpdateCodec.parse(json)
        MbftUpdateCodec.validate(payload).requireValid("Snapshot MBFT inválido")
        val relations = InfractionRelationsUpdateCodec.build(payload)
        InfractionRelationsUpdateCodec.validate(relations, payload)
            .requireValid("Relações MBFT inválidas")

        database.withTransaction {
            val dao = database.infractionDao()
            val normativeDao = database.normativeDao()

            normativeDao.clearInfractionNormLinks()
            normativeDao.clearDatasetMetadata(INFRACTION_NORM_LINKS_DATASET_ID)

            dao.clearRelations()
            dao.clearSourceCrossRefs()
            dao.clearNotes()
            dao.clearLegalBasis()
            dao.clearSearchIndex()
            dao.clearInfractions()
            dao.clearSources()
            dao.clearDatasetMetadata(RELATIONS_DATASET_ID)
            dao.clearDatasetMetadata(UpdateDataset.MBFT.id)

            dao.insertInfractions(
                payload.records.map { record ->
                    InfractionEntity(
                        code = record.code,
                        article = record.article,
                        shortDescription = record.shortDescription,
                        description = record.description,
                        contentStatus = record.status,
                        severity = record.severity,
                        points = record.points,
                        offenderResponsibility = record.offenderResponsibility,
                        authority = record.authority,
                        verificationMethod = record.verificationMethod,
                        penalty = record.penalty,
                        administrativeMeasure = record.administrativeMeasure,
                        canConfigureTrafficCrime = record.canConfigureTrafficCrime,
                    )
                },
            )

            dao.insertSearchEntries(
                payload.records.map { record ->
                    InfractionSearchEntity(
                        code = record.code,
                        article = record.article,
                        shortDescription = record.shortDescription,
                        description = record.description,
                        keywords = buildList {
                            addAll(record.legalBasis)
                            addAll(record.whenToCite)
                            addAll(record.whenNotToCite)
                            addAll(record.procedures)
                            addAll(record.aitObservations)
                            addAll(record.sourceConflicts)
                            addAll(record.related.map { it.code })
                            addAll(record.related.mapNotNull { it.reason })
                        }.joinToString(" "),
                    )
                },
            )

            val notes = buildList {
                payload.records.forEach { record ->
                    fun appendNotes(kind: String, values: List<String>) {
                        values.forEachIndexed { index, text ->
                            add(
                                InfractionNoteEntity(
                                    infractionCode = record.code,
                                    kind = kind,
                                    position = index + 1,
                                    text = text,
                                ),
                            )
                        }
                    }
                    appendNotes("WHEN_TO_CITE", record.whenToCite)
                    appendNotes("WHEN_NOT_TO_CITE", record.whenNotToCite)
                    appendNotes("PROCEDURE", record.procedures)
                    appendNotes("AIT_OBSERVATION", record.aitObservations)
                    appendNotes("SOURCE_CONFLICT", record.sourceConflicts)
                }
            }
            if (notes.isNotEmpty()) dao.insertNotes(notes)

            val legalBasis = payload.records.flatMap { record ->
                record.legalBasis.mapIndexed { index, text ->
                    InfractionLegalBasisEntity(
                        infractionCode = record.code,
                        position = index + 1,
                        text = text,
                    )
                }
            }
            if (legalBasis.isNotEmpty()) dao.insertLegalBasis(legalBasis)

            val sources = payload.records
                .flatMap(MbftUpdateRecord::sources)
                .distinctBy(MbftSourceRecord::id)
                .map { source ->
                    OfficialSourceEntity(
                        sourceId = source.id,
                        agency = source.agency,
                        document = source.document,
                        section = source.section,
                        officialUrl = source.officialUrl,
                        effectiveFrom = source.effectiveFrom,
                        lastCheckedAt = source.lastCheckedAt,
                    )
                }
            if (sources.isNotEmpty()) dao.insertSources(sources)

            val crossRefs = payload.records.flatMap { record ->
                record.sources.map { source ->
                    InfractionSourceCrossRef(
                        infractionCode = record.code,
                        sourceId = source.id,
                    )
                }
            }
            if (crossRefs.isNotEmpty()) dao.insertSourceCrossRefs(crossRefs)

            val relationEntities = relations.records.map { relation ->
                InfractionRelationEntity(
                    infractionCode = relation.sourceCode,
                    relatedCode = relation.targetCode,
                    relationType = relation.relationType,
                    reason = relation.reason,
                    evidence = relation.evidence.joinToString("\n\n").ifBlank { null },
                    sourceField = relation.sourceField,
                    origin = relation.origin,
                )
            }
            if (relationEntities.isNotEmpty()) dao.insertRelations(relationEntities)

            dao.insertDatasetMetadata(
                DatasetMetadataEntity(
                    datasetId = UpdateDataset.MBFT.id,
                    version = payload.version,
                    generatedAt = payload.generatedAt,
                    verifiedAt = payload.verifiedAt,
                    sourceDocument = payload.sourceDocument,
                    sourceUrl = payload.sourceUrl,
                    knownAmendments = payload.knownAmendments.joinToString(", "),
                    recordCount = payload.recordCount,
                ),
            )
            dao.insertDatasetMetadata(
                DatasetMetadataEntity(
                    datasetId = RELATIONS_DATASET_ID,
                    version = relations.version,
                    generatedAt = relations.generatedAt,
                    verifiedAt = relations.verifiedAt,
                    sourceDocument = relations.sourceDocument,
                    sourceUrl = "derived://mbft/${payload.version}",
                    knownAmendments = "Derivado da versão MBFT ${payload.version}",
                    recordCount = relations.recordCount,
                ),
            )
        }

        return payload.version
    }

    private suspend fun promoteNormativeCatalog(json: String): String {
        val payload = NormativeUpdateCodec.parse(json)
        NormativeUpdateCodec.validate(payload).requireValid("Snapshot normativo inválido")

        database.withTransaction {
            val dao = database.normativeDao()
            dao.clearInfractionNormLinks()
            dao.clearFullTextSearchIndex()
            dao.clearFullTexts()
            dao.clearDatasetMetadata(NORMATIVE_FULLTEXT_DATASET_ID)
            dao.clearDatasetMetadata(INFRACTION_NORM_LINKS_DATASET_ID)
            dao.clearSearchIndex()
            dao.clearActs()
            dao.clearDatasetMetadata(UpdateDataset.NORMATIVE_CATALOG.id)

            dao.insertActs(
                payload.records.map { record ->
                    NormativeActEntity(
                        id = record.id,
                        agency = record.agency,
                        actType = record.actType,
                        number = record.number,
                        numberLabel = record.numberLabel,
                        year = record.year,
                        actDate = record.actDate,
                        publicationDate = record.publicationDate,
                        subject = record.subject,
                        status = record.status,
                        officialUrl = record.officialUrl,
                        sourcePageUrl = record.sourcePageUrl,
                        lastCheckedAt = record.lastCheckedAt,
                    )
                },
            )
            dao.insertSearchEntries(
                payload.records.map { record ->
                    NormativeActSearchEntity(
                        actId = record.id,
                        agency = record.agency,
                        actType = record.actType,
                        numberLabel = record.numberLabel,
                        year = record.year.toString(),
                        subject = record.subject,
                        status = record.status.orEmpty(),
                    )
                },
            )
            dao.insertDatasetMetadata(
                DatasetMetadataEntity(
                    datasetId = UpdateDataset.NORMATIVE_CATALOG.id,
                    version = payload.version,
                    generatedAt = payload.generatedAt,
                    verifiedAt = payload.verifiedAt,
                    sourceDocument = payload.sourceDocument,
                    sourceUrl = payload.sources.joinToString(", ") { it.url },
                    knownAmendments = "",
                    recordCount = payload.recordCount,
                ),
            )
        }

        return payload.version
    }

    private companion object {
        const val RELATIONS_DATASET_ID = "infraction-relations"
        const val NORMATIVE_FULLTEXT_DATASET_ID = "normative-fulltext"
        const val INFRACTION_NORM_LINKS_DATASET_ID = "infraction-norm-links"
    }
}
