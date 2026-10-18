package com.cybertran.enquadraplus.data.importer

import android.content.Context
import androidx.room.withTransaction
import com.cybertran.enquadraplus.data.local.CtbArticleEntity
import com.cybertran.enquadraplus.data.local.CtbArticleSearchEntity
import com.cybertran.enquadraplus.data.local.DatasetMetadataEntity
import com.cybertran.enquadraplus.data.local.EnquadraDatabase
import org.json.JSONArray
import org.json.JSONObject

class CtbAssetImporter(
    private val context: Context,
    private val database: EnquadraDatabase,
) {

    suspend fun importIfNeeded() {
        val dataset = loadDataset().validated()
        val dao = database.ctbDao()

        // O asset é apenas bootstrap. Uma geração já instalada pode ser mais nova
        // do que o APK e nunca deve ser substituída silenciosamente na inicialização.
        if (dao.getDatasetVersion(DATASET_ID) != null) return

        database.withTransaction {
            if (dao.getDatasetVersion(DATASET_ID) != null) {
                return@withTransaction
            }

            dao.clearSearchIndex()
            dao.clearArticles()
            dao.clearDatasetMetadata(DATASET_ID)

            dataset.records.forEach { record ->
                dao.insertArticle(record.toEntity(dataset))
                dao.insertSearch(record.toSearchEntity())
            }

            dao.insertDatasetMetadata(
                DatasetMetadataEntity(
                    datasetId = DATASET_ID,
                    version = dataset.version,
                    generatedAt = dataset.generatedAt,
                    verifiedAt = dataset.verifiedAt,
                    sourceDocument = dataset.sourceDocument,
                    sourceUrl = dataset.sourceUrl,
                    knownAmendments = "",
                    recordCount = dataset.records.size,
                ),
            )
        }
    }

    private fun loadDataset(): CtbDataset {
        val json = context.assets
            .open(ASSET_PATH)
            .bufferedReader()
            .use { it.readText() }

        return CtbDatasetParser.parse(json)
    }

    private companion object {
        const val DATASET_ID = "ctb"
        const val ASSET_PATH = "ctb/articles.json"
    }
}

private object CtbDatasetParser {

    fun parse(json: String): CtbDataset {
        val root = JSONObject(json)

        return CtbDataset(
            version = root.getString("version"),
            generatedAt = root.getString("generatedAt"),
            verifiedAt = root.getString("verifiedAt"),
            sourceDocument = root.getString("sourceDocument"),
            sourceUrl = root.getString("sourceUrl"),
            sourceSha256 = root.getString("sourceSha256"),
            contentSha256 = root.getString("contentSha256"),
            recordCount = root.getInt("recordCount"),
            records = root.getJSONArray("records").objects().map(::parseRecord),
        )
    }

    private fun parseRecord(json: JSONObject): CtbRecord =
        CtbRecord(
            id = json.getString("id"),
            label = json.getString("label"),
            number = json.getInt("number"),
            suffix = json.nullableString("suffix"),
            chapter = json.nullableString("chapter"),
            section = json.nullableString("section"),
            subsection = json.nullableString("subsection"),
            text = json.getString("text"),
        )

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key).takeIf(String::isNotBlank)

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).map(::getJSONObject)
}

private data class CtbDataset(
    val version: String,
    val generatedAt: String,
    val verifiedAt: String,
    val sourceDocument: String,
    val sourceUrl: String,
    val sourceSha256: String,
    val contentSha256: String,
    val recordCount: Int,
    val records: List<CtbRecord>,
) {
    fun validated(): CtbDataset {
        require(version.isNotBlank()) { "Dataset CTB sem versão." }
        require(sourceUrl.startsWith("https://www.planalto.gov.br/")) {
            "Dataset CTB deve apontar para a fonte oficial do Planalto."
        }
        require(sourceSha256.matches(Regex("[A-F0-9]{64}"))) {
            "Dataset CTB com hash de origem inválido."
        }
        require(contentSha256.matches(Regex("[A-F0-9]{64}"))) {
            "Dataset CTB com hash de conteúdo inválido."
        }
        val expectedVersion =
            verifiedAt + "-planalto-" + contentSha256.take(12).lowercase()
        require(version == expectedVersion) {
            "Versão do dataset CTB não corresponde ao hash de conteúdo."
        }
        require(recordCount == records.size) {
            "Contagem declarada do CTB diverge do número de artigos."
        }
        require(records.size >= 301) {
            "Dataset CTB incompleto: ${records.size} entradas."
        }
        require(records.any { it.id == "anexo-i" }) {
            "Dataset CTB sem o Anexo I."
        }

        val duplicateIds = records
            .groupingBy { it.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        require(duplicateIds.isEmpty()) {
            "Artigos duplicados no CTB: " + duplicateIds.joinToString()
        }

        records.forEach { record ->
            val isAnnex = record.id == "anexo-i"
            require(isAnnex || record.id.matches(Regex("art-\\d+(?:-[a-z])?"))) {
                "ID inválido no CTB: ${record.id}"
            }
            require(
                (isAnnex && record.label == "Anexo I") ||
                    (!isAnnex && record.label.startsWith("Art. "))
            ) {
                "Rótulo inválido no CTB: ${record.label}"
            }
            require(
                (isAnnex && record.text.startsWith("ANEXO I")) ||
                    (!isAnnex && record.text.startsWith("Art."))
            ) {
                "Texto inválido na entrada ${record.id}."
            }
        }

        return this
    }
}

private data class CtbRecord(
    val id: String,
    val label: String,
    val number: Int,
    val suffix: String?,
    val chapter: String?,
    val section: String?,
    val subsection: String?,
    val text: String,
) {
    fun toEntity(dataset: CtbDataset): CtbArticleEntity =
        CtbArticleEntity(
            id = id,
            label = label,
            number = number,
            suffix = suffix,
            chapter = chapter,
            section = section,
            subsection = subsection,
            text = text,
            sourceUrl = dataset.sourceUrl,
            lastCheckedAt = dataset.verifiedAt,
        )

    fun toSearchEntity(): CtbArticleSearchEntity =
        CtbArticleSearchEntity(
            articleId = id,
            label = label,
            context = listOfNotNull(chapter, section, subsection).joinToString(" "),
            text = text,
        )
}
