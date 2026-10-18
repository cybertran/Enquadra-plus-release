package com.cybertran.enquadraplus.data.importer

import android.content.Context
import androidx.room.withTransaction
import com.cybertran.enquadraplus.data.local.DatasetMetadataEntity
import com.cybertran.enquadraplus.data.local.EnquadraDatabase
import com.cybertran.enquadraplus.data.local.InfractionRelationEntity
import org.json.JSONArray
import org.json.JSONObject

class RelationsAssetImporter(
    private val context: Context,
    private val database: EnquadraDatabase,
) {

    suspend fun importIfNeeded() {
        val dataset = loadDataset().validated()
        val dao = database.infractionDao()

        // As relações embarcadas são somente bootstrap. Depois que o atualizador
        // promove MBFT + relações atomicamente, o asset antigo não participa mais.
        if (dao.getDatasetVersion(DATASET_ID) != null) return

        val currentMbftVersion = dao.getDatasetVersion(MBFT_DATASET_ID)
        require(currentMbftVersion == dataset.mbftVersion) {
            "Dataset de relações não corresponde à versão MBFT instalada."
        }

        database.withTransaction {
            val transactionMbftVersion = dao.getDatasetVersion(MBFT_DATASET_ID)
            require(transactionMbftVersion == dataset.mbftVersion) {
                "Versão MBFT mudou durante a importação das relações."
            }

            dao.clearRelations()
            dao.clearDatasetMetadata(DATASET_ID)

            val entities = dataset.records.map(RelationRecord::toEntity)
            if (entities.isNotEmpty()) {
                dao.insertRelations(entities)
            }

            dao.insertDatasetMetadata(
                DatasetMetadataEntity(
                    datasetId = DATASET_ID,
                    version = dataset.version,
                    generatedAt = dataset.generatedAt,
                    verifiedAt = dataset.verifiedAt,
                    sourceDocument = dataset.sourceDocument,
                    sourceUrl = "asset://mbft/infractions.json",
                    knownAmendments = "Derivado da versão MBFT ${dataset.mbftVersion}",
                    recordCount = dataset.records.size,
                ),
            )
        }
    }

    private fun loadDataset(): RelationDataset {
        val json = context.assets
            .open(ASSET_PATH)
            .bufferedReader()
            .use { it.readText() }

        return RelationDatasetParser.parse(json)
    }

    private companion object {
        const val DATASET_ID = "infraction-relations"
        const val MBFT_DATASET_ID = "mbft"
        const val ASSET_PATH = "relations/relations.json"
    }
}

private object RelationDatasetParser {

    fun parse(json: String): RelationDataset {
        val root = JSONObject(json)
        return RelationDataset(
            version = root.getString("version"),
            generatedAt = root.getString("generatedAt"),
            verifiedAt = root.getString("verifiedAt"),
            mbftVersion = root.getString("mbftVersion"),
            sourceDocument = root.getString("sourceDocument"),
            contentSha256 = root.getString("contentSha256"),
            recordCount = root.getInt("recordCount"),
            records = root.getJSONArray("records").objects().map(::parseRecord),
        )
    }

    private fun parseRecord(json: JSONObject): RelationRecord =
        RelationRecord(
            sourceCode = json.getString("sourceCode"),
            targetCode = json.getString("targetCode"),
            relationType = json.getString("relationType"),
            origin = json.getString("origin"),
            sourceField = json.nullableString("sourceField"),
            reason = json.nullableString("reason"),
            evidence = json.optJSONArray("evidence")?.strings().orEmpty(),
        )

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key).takeIf(String::isNotBlank)

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).map(::getJSONObject)

    private fun JSONArray.strings(): List<String> =
        (0 until length()).map(::getString)
}

private data class RelationDataset(
    val version: String,
    val generatedAt: String,
    val verifiedAt: String,
    val mbftVersion: String,
    val sourceDocument: String,
    val contentSha256: String,
    val recordCount: Int,
    val records: List<RelationRecord>,
) {
    fun validated(): RelationDataset {
        require(version.isNotBlank()) { "Dataset de relações sem versão." }
        require(mbftVersion.isNotBlank()) { "Dataset de relações sem versão MBFT." }
        require(contentSha256.matches(Regex("[A-F0-9]{64}"))) {
            "Dataset de relações com hash inválido."
        }
        val expectedVersion =
            verifiedAt + "-relations-" + contentSha256.take(12).lowercase()
        require(version == expectedVersion) {
            "Versão do dataset de relações não corresponde ao hash."
        }
        require(recordCount == records.size) {
            "Contagem declarada das relações diverge dos registros."
        }
        require(records.isNotEmpty()) { "Dataset de relações vazio." }

        val keys = records.map {
            Triple(it.sourceCode, it.targetCode, it.relationType)
        }
        require(keys.size == keys.toSet().size) {
            "Dataset de relações contém chaves duplicadas."
        }

        records.forEach { record ->
            require(record.sourceCode.matches(Regex("\\d{3}-\\d{2}"))) {
                "Código-fonte inválido: ${record.sourceCode}"
            }
            require(record.targetCode.matches(Regex("\\d{3}-\\d{2}"))) {
                "Código relacionado inválido: ${record.targetCode}"
            }
            require(record.sourceCode != record.targetCode) {
                "Relação reflexiva inválida: ${record.sourceCode}"
            }
            require(
                record.relationType in
                    setOf("ALTERNATIVE", "CONCOMITANT", "PREREQUISITE", "RELATED"),
            ) {
                "Tipo de relação inválido: ${record.relationType}"
            }

            if (record.relationType == "RELATED") {
                require(record.origin == "CURATED_PROJECT_RELATION") {
                    "Relação editorial com origem inválida."
                }
                require(!record.reason.isNullOrBlank()) {
                    "Relação editorial sem motivo."
                }
            } else {
                require(record.origin == "MBFT_OFFICIAL_FIELD") {
                    "Relação oficial com origem inválida."
                }
                require(record.sourceField in setOf("whenNotToCite", "procedures")) {
                    "Relação oficial com campo-fonte inválido."
                }
                require(record.evidence.isNotEmpty()) {
                    "Relação oficial sem evidência."
                }
                require(record.evidence.all { record.targetCode in it }) {
                    "Evidência não menciona explicitamente o código relacionado."
                }
            }
        }

        return this
    }
}

private data class RelationRecord(
    val sourceCode: String,
    val targetCode: String,
    val relationType: String,
    val origin: String,
    val sourceField: String?,
    val reason: String?,
    val evidence: List<String>,
) {
    fun toEntity(): InfractionRelationEntity =
        InfractionRelationEntity(
            infractionCode = sourceCode,
            relatedCode = targetCode,
            relationType = relationType,
            reason = reason,
            evidence = evidence.joinToString(EVIDENCE_SEPARATOR).ifBlank { null },
            sourceField = sourceField,
            origin = origin,
        )

    private companion object {
        const val EVIDENCE_SEPARATOR = "\n\n"
    }
}
