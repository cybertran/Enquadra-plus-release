package com.cybertran.enquadraplus.data.importer

import android.content.Context
import androidx.room.withTransaction
import com.cybertran.enquadraplus.data.local.DatasetMetadataEntity
import com.cybertran.enquadraplus.data.local.EnquadraDatabase
import com.cybertran.enquadraplus.data.local.NormativeActEntity
import com.cybertran.enquadraplus.data.local.NormativeActSearchEntity
import org.json.JSONArray
import org.json.JSONObject

class NormativeAssetImporter(
    private val context: Context,
    private val database: EnquadraDatabase,
) {

    suspend fun importIfNeeded() {
        val dataset = loadDataset().validated()
        val dao = database.normativeDao()

        // Uma geração instalada mais nova que o asset nunca é substituída.
        // A exceção controlada é a ampliação de cobertura do mesmo dia:
        // a v0.17 adiciona Deliberações CONTRAN ao catálogo canônico.
        if (!shouldInstallBundled(dataset)) return

        database.withTransaction {
            if (!shouldInstallBundled(dataset)) {
                return@withTransaction
            }

            dao.clearInfractionNormLinks()
            dao.clearFullTextSearchIndex()
            dao.clearFullTexts()
            dao.clearDatasetMetadata(FULLTEXT_DATASET_ID)
            dao.clearDatasetMetadata(LINKS_DATASET_ID)
            dao.clearSearchIndex()
            dao.clearActs()
            dao.clearDatasetMetadata(DATASET_ID)

            dataset.records.forEach { record ->
                dao.insertAct(record.toEntity())
                dao.insertSearch(record.toSearchEntity())
            }

            dao.insertDatasetMetadata(
                DatasetMetadataEntity(
                    datasetId = DATASET_ID,
                    version = dataset.version,
                    generatedAt = dataset.generatedAt,
                    verifiedAt = dataset.verifiedAt,
                    sourceDocument = dataset.sourceDocument,
                    sourceUrl = dataset.sources.joinToString(", ") { it.url },
                    knownAmendments = "",
                    recordCount = dataset.records.size,
                ),
            )
        }
    }

    private fun loadDataset(): NormativeDataset {
        val json = context.assets
            .open(ASSET_PATH)
            .bufferedReader()
            .use { it.readText() }

        return NormativeDatasetParser.parse(json)
    }

    private suspend fun shouldInstallBundled(dataset: NormativeDataset): Boolean {
        val dao = database.normativeDao()
        val installedVersion = dao.getDatasetVersion(DATASET_ID) ?: return true
        val installedDate = installedVersion.take(10)
        val bundledDate = dataset.verifiedAt.take(10)

        if (!installedDate.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) return false
        if (installedDate > bundledDate) return false
        if (installedDate < bundledDate) return true

        val bundledHasDeliberations =
            dataset.records.any { it.actType == "DELIBERAÇÃO" }
        val installedHasDeliberations =
            dao.countByActType("DELIBERAÇÃO") > 0

        return bundledHasDeliberations && !installedHasDeliberations
    }

    private companion object {
        const val DATASET_ID = "normative-catalog"
        const val FULLTEXT_DATASET_ID = "normative-fulltext"
        const val LINKS_DATASET_ID = "infraction-norm-links"
        const val ASSET_PATH = "norms/acts.json"
    }
}

private object NormativeDatasetParser {

    fun parse(json: String): NormativeDataset {
        val root = JSONObject(json)

        return NormativeDataset(
            version = root.getString("version"),
            generatedAt = root.getString("generatedAt"),
            verifiedAt = root.getString("verifiedAt"),
            sourceDocument = root.getString("sourceDocument"),
            contentSha256 = root.getString("contentSha256"),
            recordCount = root.getInt("recordCount"),
            sources = root.getJSONArray("sources").objects().map(::parseSource),
            records = root.getJSONArray("records").objects().map(::parseRecord),
        )
    }

    private fun parseSource(json: JSONObject): NormativeSource =
        NormativeSource(
            agency = json.getString("agency"),
            actType = json.getString("actType"),
            year = if (json.isNull("year")) null else json.getInt("year"),
            url = json.getString("url"),
            sourceSha256 = json.getString("sourceSha256"),
            recordCount = json.getInt("recordCount"),
        )

    private fun parseRecord(json: JSONObject): NormativeRecord =
        NormativeRecord(
            id = json.getString("id"),
            agency = json.getString("agency"),
            actType = json.getString("actType"),
            number = json.getInt("number"),
            numberLabel = json.getString("numberLabel"),
            year = json.getInt("year"),
            actDate = json.nullableString("actDate"),
            publicationDate = json.nullableString("publicationDate"),
            subject = json.getString("subject"),
            status = json.nullableString("status"),
            officialUrl = json.getString("officialUrl"),
            sourcePageUrl = json.getString("sourcePageUrl"),
            lastCheckedAt = json.getString("lastCheckedAt"),
        )

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key).takeIf(String::isNotBlank)

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).map(::getJSONObject)
}

private data class NormativeDataset(
    val version: String,
    val generatedAt: String,
    val verifiedAt: String,
    val sourceDocument: String,
    val contentSha256: String,
    val recordCount: Int,
    val sources: List<NormativeSource>,
    val records: List<NormativeRecord>,
) {
    fun validated(): NormativeDataset {
        require(version.isNotBlank()) { "Catálogo normativo sem versão." }
        require(contentSha256.matches(Regex("[A-F0-9]{64}"))) {
            "Catálogo normativo com hash de conteúdo inválido."
        }
        val expectedVersion =
            verifiedAt + "-norms-" + contentSha256.take(12).lowercase()
        require(version == expectedVersion) {
            "Versão do catálogo normativo não corresponde ao hash de conteúdo."
        }
        require(sources.size >= 8) {
            "Catálogo normativo sem todas as fontes esperadas."
        }
        require(recordCount == records.size) {
            "Contagem declarada do catálogo normativo diverge dos registros."
        }
        require(sources.sumOf { it.recordCount } == records.size) {
            "Contagem das páginas-fonte diverge dos registros do catálogo."
        }
        require(records.size >= 300) {
            "Catálogo normativo incompleto: ${records.size} atos."
        }

        sources.forEach { source ->
            require(source.agency in setOf("CONTRAN", "SENATRAN")) {
                "Órgão inválido na fonte normativa: ${source.agency}"
            }
            require(source.sourceSha256.matches(Regex("[A-F0-9]{64}"))) {
                "Hash de fonte inválido: ${source.url}"
            }
            require(source.url.isOfficialTransportUrl()) {
                "Fonte normativa não oficial: ${source.url}"
            }
        }

        val duplicateIds = records
            .groupingBy { it.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        require(duplicateIds.isEmpty()) {
            "Atos normativos duplicados: " + duplicateIds.joinToString()
        }

        records.forEach { record ->
            require(record.agency in setOf("CONTRAN", "SENATRAN")) {
                "Órgão inválido no ato ${record.id}."
            }
            require(record.actType in setOf("RESOLUÇÃO", "DELIBERAÇÃO", "PORTARIA")) {
                "Tipo inválido no ato ${record.id}."
            }
            require(record.number > 0 && record.year >= 1900) {
                "Número ou ano inválido no ato ${record.id}."
            }
            require(record.subject.isNotBlank()) {
                "Ato ${record.id} sem assunto."
            }
            require(record.officialUrl.isOfficialTransportUrl()) {
                "URL oficial inválida no ato ${record.id}."
            }
            require(record.sourcePageUrl.isOfficialTransportUrl()) {
                "Página-fonte inválida no ato ${record.id}."
            }
        }

        require(records.any { it.agency == "CONTRAN" && it.number == 1031 && it.year == 2026 }) {
            "Resolução CONTRAN 1031/2026 ausente."
        }
        require(records.any { it.agency == "CONTRAN" && it.number == 985 && it.year == 2022 }) {
            "Resolução CONTRAN 985/2022 ausente."
        }
        require(
            records.any {
                it.agency == "CONTRAN" &&
                    it.actType == "DELIBERAÇÃO" &&
                    it.number == 279 &&
                    it.year == 2026
            },
        ) {
            "Deliberação CONTRAN 279/2026 ausente."
        }
        require(records.any { it.agency == "SENATRAN" && it.number == 354 && it.year == 2022 }) {
            "Portaria SENATRAN 354/2022 ausente."
        }

        return this
    }
}

private fun String.isOfficialTransportUrl(): Boolean =
    startsWith("https://www.gov.br/transportes/pt-br/assuntos/transito/") ||
        startsWith("https://gov.br/transportes/pt-br/assuntos/transito/")

private data class NormativeSource(
    val agency: String,
    val actType: String,
    val year: Int?,
    val url: String,
    val sourceSha256: String,
    val recordCount: Int,
)

private data class NormativeRecord(
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
) {
    fun toEntity(): NormativeActEntity =
        NormativeActEntity(
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
        )

    fun toSearchEntity(): NormativeActSearchEntity =
        NormativeActSearchEntity(
            actId = id,
            agency = agency,
            actType = actType,
            numberLabel = numberLabel,
            year = year.toString(),
            subject = subject,
            status = status.orEmpty(),
        )
}
