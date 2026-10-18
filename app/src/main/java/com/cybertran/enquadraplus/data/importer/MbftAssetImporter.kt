package com.cybertran.enquadraplus.data.importer

import android.content.Context
import androidx.room.withTransaction
import com.cybertran.enquadraplus.data.local.DatasetMetadataEntity
import com.cybertran.enquadraplus.data.local.EnquadraDatabase
import com.cybertran.enquadraplus.data.local.InfractionEntity
import com.cybertran.enquadraplus.data.local.InfractionNoteEntity
import com.cybertran.enquadraplus.data.local.InfractionLegalBasisEntity
import com.cybertran.enquadraplus.data.local.InfractionSearchEntity
import com.cybertran.enquadraplus.data.local.InfractionSourceCrossRef
import com.cybertran.enquadraplus.data.local.NoteKind
import com.cybertran.enquadraplus.data.local.OfficialSourceEntity
import org.json.JSONArray
import org.json.JSONObject

class MbftAssetImporter(
    private val context: Context,
    private val database: EnquadraDatabase,
) {

    suspend fun importIfNeeded() {
        val dataset = loadDataset().validated()
        val dao = database.infractionDao()

        // O asset MBFT é somente bootstrap. Gerações instaladas pelo atualizador
        // seguro nunca podem ser substituídas silenciosamente pelo APK.
        if (dao.getDatasetVersion(DATASET_ID) != null) return

        database.withTransaction {
            if (dao.getDatasetVersion(DATASET_ID) != null) {
                return@withTransaction
            }

            dao.clearSourceCrossRefs()
            dao.clearNotes()
            dao.clearLegalBasis()
            dao.clearSearchIndex()
            dao.clearInfractions()
            dao.clearSources()
            dao.clearDatasetMetadata(DATASET_ID)

            dataset.records.forEach { record ->
                dao.insertInfraction(record.toEntity())
                dao.insertSearch(record.toSearchEntity())

                val notes = record.toNotes()
                if (notes.isNotEmpty()) {
                    dao.insertNotes(notes)
                }

                val legalBasis = record.toLegalBasis()
                if (legalBasis.isNotEmpty()) {
                    dao.insertLegalBasis(legalBasis)
                }

                record.sources.forEach { source ->
                    dao.insertSource(source.toEntity())
                    dao.insertSourceCrossRef(
                        InfractionSourceCrossRef(
                            infractionCode = record.code,
                            sourceId = source.id,
                        ),
                    )
                }

            }

            dao.insertDatasetMetadata(
                DatasetMetadataEntity(
                    datasetId = DATASET_ID,
                    version = dataset.version,
                    generatedAt = dataset.generatedAt,
                    verifiedAt = dataset.verifiedAt,
                    sourceDocument = dataset.sourceDocument,
                    sourceUrl = dataset.sourceUrl,
                    knownAmendments = dataset.knownAmendments.joinToString(", "),
                    recordCount = dataset.records.size,
                ),
            )
        }
    }

    private fun loadDataset(): MbftDataset {
        val json = context.assets
            .open(ASSET_PATH)
            .bufferedReader()
            .use { it.readText() }

        return MbftDatasetParser.parse(json)
    }

    private companion object {
        const val DATASET_ID = "mbft"
        const val ASSET_PATH = "mbft/infractions.json"
    }
}

private object MbftDatasetParser {

    fun parse(json: String): MbftDataset {
        val root = JSONObject(json)

        return MbftDataset(
            version = root.getString("version"),
            generatedAt = root.getString("generatedAt"),
            verifiedAt = root.getString("verifiedAt"),
            sourceDocument = root.getString("sourceDocument"),
            sourceUrl = root.getString("sourceUrl"),
            knownAmendments = root.getJSONArray("knownAmendments").strings(),
            records = root.getJSONArray("records").objects().map(::parseRecord),
        )
    }

    private fun parseRecord(json: JSONObject): MbftRecord =
        MbftRecord(
            code = json.getString("code"),
            article = json.getString("article"),
            shortDescription = json.getString("shortDescription"),
            description = json.getString("description"),
            contentStatus = json.getString("status"),
            severity = json.getString("severity"),
            points = if (json.isNull("points")) null else json.getInt("points"),
            offenderResponsibility = json.nullableString("offenderResponsibility"),
            authority = json.nullableString("authority"),
            verificationMethod = json.nullableString("verificationMethod"),
            penalty = json.nullableString("penalty"),
            administrativeMeasure = json.nullableString("administrativeMeasure"),
            canConfigureTrafficCrime = json.optBoolean("canConfigureTrafficCrime", false),
            legalBasis = json.optJSONArray("legalBasis")?.strings().orEmpty()
                .ifEmpty { listOf(json.getString("article")) },
            whenToCite = json.optJSONArray("whenToCite")?.strings().orEmpty(),
            whenNotToCite = json.optJSONArray("whenNotToCite")?.strings().orEmpty(),
            procedures = json.optJSONArray("procedures")?.strings().orEmpty(),
            aitObservations = json.optJSONArray("aitObservations")?.strings().orEmpty(),
            related = json.optJSONArray("related")
                ?.objects()
                ?.map {
                    RelatedRecord(
                        code = it.getString("code"),
                        reason = it.nullableString("reason"),
                    )
                }
                .orEmpty(),
            sources = json.getJSONArray("sources")
                .objects()
                .map {
                    SourceRecord(
                        id = it.getString("id"),
                        agency = it.getString("agency"),
                        document = it.getString("document"),
                        section = it.nullableString("section"),
                        officialUrl = it.nullableString("officialUrl"),
                        effectiveFrom = it.nullableString("effectiveFrom"),
                        lastCheckedAt = it.getString("lastCheckedAt"),
                    )
                },
        )

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key).takeIf(String::isNotBlank)

    private fun JSONArray.strings(): List<String> =
        (0 until length()).map(::getString)

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).map(::getJSONObject)
}

private data class MbftDataset(
    val version: String,
    val generatedAt: String,
    val verifiedAt: String,
    val sourceDocument: String,
    val sourceUrl: String,
    val knownAmendments: List<String>,
    val records: List<MbftRecord>,
) {
    fun validated(): MbftDataset {
        require(version.isNotBlank()) { "Dataset MBFT sem versão." }
        require(records.isNotEmpty()) { "Dataset MBFT sem fichas." }

        val duplicateCodes = records
            .groupingBy { it.code }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        require(duplicateCodes.isEmpty()) {
            "Códigos de enquadramento duplicados: " + duplicateCodes.joinToString()
        }

        records.forEach { record ->
            require(record.code.isNotBlank()) { "Ficha sem código de enquadramento." }
            require(record.article.isNotBlank()) { "Ficha ${record.code} sem artigo." }
            require(record.description.isNotBlank()) { "Ficha ${record.code} sem tipificação." }
            require(record.contentStatus in setOf("VERIFIED", "PARTIAL_VALIDATION")) {
                "Status inválido na ficha ${record.code}: ${record.contentStatus}"
            }
            require(record.sources.isNotEmpty()) { "Ficha ${record.code} sem fonte oficial." }

            record.sources.forEach { source ->
                require(source.id.isNotBlank()) { "Fonte sem ID na ficha ${record.code}." }
                require(source.agency.isNotBlank()) { "Fonte sem órgão na ficha ${record.code}." }
                require(source.document.isNotBlank()) { "Fonte sem documento na ficha ${record.code}." }
                require(source.lastCheckedAt.isNotBlank()) {
                    "Fonte sem data de verificação na ficha ${record.code}."
                }
            }
        }

        return this
    }
}

private data class MbftRecord(
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
    val legalBasis: List<String>,
    val whenToCite: List<String>,
    val whenNotToCite: List<String>,
    val procedures: List<String>,
    val aitObservations: List<String>,
    val related: List<RelatedRecord>,
    val sources: List<SourceRecord>,
) {
    fun toEntity(): InfractionEntity =
        InfractionEntity(
            code = code,
            article = article,
            shortDescription = shortDescription,
            description = description,
            contentStatus = contentStatus,
            severity = severity,
            points = points,
            offenderResponsibility = offenderResponsibility,
            authority = authority,
            verificationMethod = verificationMethod,
            penalty = penalty,
            administrativeMeasure = administrativeMeasure,
            canConfigureTrafficCrime = canConfigureTrafficCrime,
        )

    fun toSearchEntity(): InfractionSearchEntity =
        InfractionSearchEntity(
            code = code,
            article = article,
            shortDescription = shortDescription,
            description = description,
            keywords = buildList {
                addAll(legalBasis)
                addAll(whenToCite)
                addAll(whenNotToCite)
                addAll(procedures)
                addAll(aitObservations)
                addAll(related.map { it.code })
                addAll(related.mapNotNull { it.reason })
            }.joinToString(" "),
        )

    fun toLegalBasis(): List<InfractionLegalBasisEntity> =
        legalBasis.mapIndexed { index, text ->
            InfractionLegalBasisEntity(
                infractionCode = code,
                position = index + 1,
                text = text,
            )
        }

    fun toNotes(): List<InfractionNoteEntity> =
        buildList {
            addNotes(NoteKind.WHEN_TO_CITE, whenToCite)
            addNotes(NoteKind.WHEN_NOT_TO_CITE, whenNotToCite)
            addNotes(NoteKind.PROCEDURE, procedures)
            addNotes(NoteKind.AIT_OBSERVATION, aitObservations)
        }

    private fun MutableList<InfractionNoteEntity>.addNotes(
        kind: String,
        values: List<String>,
    ) {
        values.forEachIndexed { index, text ->
            add(
                InfractionNoteEntity(
                    infractionCode = code,
                    kind = kind,
                    position = index + 1,
                    text = text,
                ),
            )
        }
    }
}

private data class RelatedRecord(
    val code: String,
    val reason: String?,
)

private data class SourceRecord(
    val id: String,
    val agency: String,
    val document: String,
    val section: String?,
    val officialUrl: String?,
    val effectiveFrom: String?,
    val lastCheckedAt: String,
) {
    fun toEntity(): OfficialSourceEntity =
        OfficialSourceEntity(
            sourceId = id,
            agency = agency,
            document = document,
            section = section,
            officialUrl = officialUrl,
            effectiveFrom = effectiveFrom,
            lastCheckedAt = lastCheckedAt,
        )
}
