package com.cybertran.enquadraplus.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "infractions",
    indices = [Index(value = ["article"])],
)
data class InfractionEntity(
    @PrimaryKey
    val code: String,
    val article: String,
    val shortDescription: String,
    val description: String,
    val contentStatus: String,
    val severity: String,
    val points: Int?,
    val offenderResponsibility: String?,
    val authority: String?,
    val verificationMethod: String?,
    val penalty: String?,
    val administrativeMeasure: String?,
    val canConfigureTrafficCrime: Boolean,
)

@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "infraction_search")
data class InfractionSearchEntity(
    val code: String,
    val article: String,
    val shortDescription: String,
    val description: String,
    val keywords: String,
)


@Entity(
    tableName = "ctb_articles",
    indices = [Index(value = ["number", "suffix"])],
)
data class CtbArticleEntity(
    @PrimaryKey
    val id: String,
    val label: String,
    val number: Int,
    val suffix: String?,
    val chapter: String?,
    val section: String?,
    val subsection: String?,
    val text: String,
    val sourceUrl: String,
    val lastCheckedAt: String,
)

@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "ctb_article_search")
data class CtbArticleSearchEntity(
    val articleId: String,
    val label: String,
    val context: String,
    val text: String,
)

@Entity(
    tableName = "normative_acts",
    indices = [
        Index(value = ["agency"]),
        Index(value = ["year", "number"]),
    ],
)
data class NormativeActEntity(
    @PrimaryKey
    val id: String,
    val agency: String,
    val actType: String,
    val number: Int,
    val numberLabel: String,
    val year: Int,
    val actDate: String?,
    val publicationDate: String?,
    val subject: String,
    val status: String?,
    val officialUrl: String,
    val sourcePageUrl: String,
    val lastCheckedAt: String,
)

@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "normative_act_search")
data class NormativeActSearchEntity(
    val actId: String,
    val agency: String,
    val actType: String,
    val numberLabel: String,
    val year: String,
    val subject: String,
    val status: String,
)


@Entity(
    tableName = "normative_full_text",
    foreignKeys = [
        ForeignKey(
            entity = NormativeActEntity::class,
            parentColumns = ["id"],
            childColumns = ["actId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["actId"], unique = true)],
)
data class NormativeFullTextEntity(
    @PrimaryKey
    val actId: String,
    val sourceSha256: String,
    val mediaType: String,
    val pageCount: Int?,
    val text: String,
)

@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "normative_full_text_search")
data class NormativeFullTextSearchEntity(
    val actId: String,
    val text: String,
)

@Entity(
    tableName = "infraction_norm_links",
    primaryKeys = ["infractionCode", "actId", "relationKind"],
    foreignKeys = [
        ForeignKey(
            entity = InfractionEntity::class,
            parentColumns = ["code"],
            childColumns = ["infractionCode"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NormativeActEntity::class,
            parentColumns = ["id"],
            childColumns = ["actId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["infractionCode"]),
        Index(value = ["actId"]),
    ],
)
data class InfractionNormLinkEntity(
    val infractionCode: String,
    val actId: String,
    val relationKind: String,
    val evidence: String,
    val officialUrl: String,
)

@Entity(
    tableName = "infraction_notes",
    foreignKeys = [
        ForeignKey(
            entity = InfractionEntity::class,
            parentColumns = ["code"],
            childColumns = ["infractionCode"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["infractionCode"]),
        Index(value = ["infractionCode", "kind", "position"], unique = true),
    ],
)
data class InfractionNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val infractionCode: String,
    val kind: String,
    val position: Int,
    val text: String,
)


@Entity(
    tableName = "infraction_legal_basis",
    foreignKeys = [
        ForeignKey(
            entity = InfractionEntity::class,
            parentColumns = ["code"],
            childColumns = ["infractionCode"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["infractionCode"]),
        Index(value = ["infractionCode", "position"], unique = true),
    ],
)
data class InfractionLegalBasisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val infractionCode: String,
    val position: Int,
    val text: String,
)

@Entity(tableName = "official_sources")
data class OfficialSourceEntity(
    @PrimaryKey
    val sourceId: String,
    val agency: String,
    val document: String,
    val section: String?,
    val officialUrl: String?,
    val effectiveFrom: String?,
    val lastCheckedAt: String,
)

@Entity(
    tableName = "infraction_source_cross_ref",
    primaryKeys = ["infractionCode", "sourceId"],
    foreignKeys = [
        ForeignKey(
            entity = InfractionEntity::class,
            parentColumns = ["code"],
            childColumns = ["infractionCode"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = OfficialSourceEntity::class,
            parentColumns = ["sourceId"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["infractionCode"]),
        Index(value = ["sourceId"]),
    ],
)
data class InfractionSourceCrossRef(
    val infractionCode: String,
    val sourceId: String,
)

@Entity(
    tableName = "infraction_relations",
    primaryKeys = ["infractionCode", "relatedCode", "relationType"],
    foreignKeys = [
        ForeignKey(
            entity = InfractionEntity::class,
            parentColumns = ["code"],
            childColumns = ["infractionCode"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = InfractionEntity::class,
            parentColumns = ["code"],
            childColumns = ["relatedCode"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["infractionCode"]),
        Index(value = ["relatedCode"]),
    ],
)
data class InfractionRelationEntity(
    val infractionCode: String,
    val relatedCode: String,
    val relationType: String,
    val reason: String?,
    val evidence: String?,
    val sourceField: String?,
    val origin: String,
)

@Entity(tableName = "dataset_metadata")
data class DatasetMetadataEntity(
    @PrimaryKey
    val datasetId: String,
    val version: String,
    val generatedAt: String,
    val verifiedAt: String,
    val sourceDocument: String,
    val sourceUrl: String,
    val knownAmendments: String,
    val recordCount: Int,
)

data class InfractionWithDetails(
    @Embedded
    val infraction: InfractionEntity,
    @Relation(
        parentColumn = "code",
        entityColumn = "infractionCode",
    )
    val notes: List<InfractionNoteEntity>,
    @Relation(
        parentColumn = "code",
        entityColumn = "infractionCode",
    )
    val legalBasis: List<InfractionLegalBasisEntity>,
    @Relation(
        parentColumn = "code",
        entityColumn = "sourceId",
        associateBy = Junction(
            value = InfractionSourceCrossRef::class,
            parentColumn = "infractionCode",
            entityColumn = "sourceId",
        ),
    )
    val sources: List<OfficialSourceEntity>,
    @Relation(
        parentColumn = "code",
        entityColumn = "infractionCode",
    )
    val relations: List<InfractionRelationEntity>,
)
