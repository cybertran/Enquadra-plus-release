package com.cybertran.enquadraplus.data.importer

import android.content.Context
import androidx.room.withTransaction
import com.cybertran.enquadraplus.data.local.DatasetMetadataEntity
import com.cybertran.enquadraplus.data.local.EnquadraDatabase
import com.cybertran.enquadraplus.data.local.InfractionNormLinkEntity
import com.cybertran.enquadraplus.data.local.NormativeFullTextEntity
import com.cybertran.enquadraplus.data.local.NormativeFullTextSearchEntity
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import org.json.JSONArray
import org.json.JSONObject

class NormativeFullTextAssetImporter(
    private val context: Context,
    private val database: EnquadraDatabase,
) {

    suspend fun importIfAvailable() {
        val manifestJson = readAssetOrNull(MANIFEST_PATH) ?: return
        val manifest = parseManifest(manifestJson)
        val normativeDao = database.normativeDao()
        val infractionDao = database.infractionDao()
        val installedCatalog = normativeDao.getDatasetVersion(NORMATIVE_DATASET_ID)

        if (installedCatalog != manifest.catalogVersion) {
            database.withTransaction {
                normativeDao.clearInfractionNormLinks()
                normativeDao.clearFullTextSearchIndex()
                normativeDao.clearFullTexts()
                normativeDao.clearDatasetMetadata(FULLTEXT_DATASET_ID)
                normativeDao.clearDatasetMetadata(LINKS_DATASET_ID)
            }
            return
        }

        val installedFullText = normativeDao.getDatasetVersion(FULLTEXT_DATASET_ID)
        if (installedFullText == manifest.version) {
            return
        }

        val linksJson = readAssetOrNull(LINKS_PATH)
        val installedMbftVersion =
            infractionDao.getDatasetVersion(MBFT_DATASET_ID)
        val linkImport =
            if (linksJson == null) {
                LinkImport(
                    version = manifest.version + "-links-unavailable",
                    records = emptyList(),
                )
            } else {
                parseLinksIfCompatible(
                    json = linksJson,
                    expectedCatalogVersion = manifest.catalogVersion,
                    installedMbftVersion = installedMbftVersion,
                )
            }
        val links = linkImport.records

        database.withTransaction {
            val transactionCatalog =
                normativeDao.getDatasetVersion(NORMATIVE_DATASET_ID)
            require(transactionCatalog == manifest.catalogVersion) {
                "Catálogo normativo mudou durante a importação do texto integral."
            }

            normativeDao.clearInfractionNormLinks()
            normativeDao.clearFullTextSearchIndex()
            normativeDao.clearFullTexts()
            normativeDao.clearDatasetMetadata(FULLTEXT_DATASET_ID)
            normativeDao.clearDatasetMetadata(LINKS_DATASET_ID)

            var importedFullTextCount = 0
            for (chunk in manifest.chunks) {
                val raw = readGzipAsset("norms/fulltext/" + chunk.file)
                require(sha256(raw) == chunk.contentSha256) {
                    "Hash divergente no pacote normativo " + chunk.file
                }

                val payload = JSONObject(raw.decodeToString())
                require(payload.getString("catalogVersion") == manifest.catalogVersion) {
                    "Pacote de texto integral não corresponde ao catálogo normativo."
                }

                val records = payload.getJSONArray("records")
                require(records.length() == chunk.recordCount) {
                    "Contagem divergente no pacote " + chunk.file
                }

                val fullTextBatch = mutableListOf<NormativeFullTextEntity>()
                val searchBatch = mutableListOf<NormativeFullTextSearchEntity>()
                records.objects().forEach { item ->
                    val actId = item.getString("actId")
                    val text = item.getString("text")
                    require(text.isNotBlank()) {
                        "Texto integral vazio para " + actId
                    }
                    fullTextBatch += NormativeFullTextEntity(
                        actId = actId,
                        sourceSha256 = item.getString("sourceSha256"),
                        mediaType = item.getString("mediaType"),
                        pageCount =
                            if (item.isNull("pageCount")) null else item.getInt("pageCount"),
                        text = text,
                    )
                    searchBatch += NormativeFullTextSearchEntity(
                        actId = actId,
                        text = text,
                    )
                }

                normativeDao.insertFullTexts(fullTextBatch)
                normativeDao.insertFullTextSearchEntries(searchBatch)
                importedFullTextCount += fullTextBatch.size
            }

            require(importedFullTextCount == manifest.recordCount) {
                "Contagem total do texto integral diverge do manifesto."
            }

            for (batch in links.chunked(500)) {
                normativeDao.insertInfractionNormLinks(batch)
            }

            normativeDao.insertDatasetMetadata(
                DatasetMetadataEntity(
                    datasetId = FULLTEXT_DATASET_ID,
                    version = manifest.version,
                    generatedAt = manifest.generatedAt,
                    verifiedAt = manifest.generatedAt,
                    sourceDocument = "Texto integral de atos normativos oficiais",
                    sourceUrl = "asset://norms/fulltext",
                    knownAmendments =
                        "Vinculado ao catálogo " + manifest.catalogVersion,
                    recordCount = importedFullTextCount,
                ),
            )

            normativeDao.insertDatasetMetadata(
                DatasetMetadataEntity(
                    datasetId = LINKS_DATASET_ID,
                    version = linkImport.version,
                    generatedAt = manifest.generatedAt,
                    verifiedAt = manifest.generatedAt,
                    sourceDocument = "Vínculos auditáveis entre infrações e atos normativos",
                    sourceUrl = "asset://infraction_norm_links.json",
                    knownAmendments =
                        "Somente referência explícita no MBFT ou citação direta do artigo do CTB",
                    recordCount = links.size,
                ),
            )
        }
    }

    private fun parseManifest(json: String): FullTextManifest {
        val root = JSONObject(json)
        val catalogVersion = root.getString("catalogVersion")
        val contentSha256 = root.getString("contentSha256")
        require(contentSha256.matches(Regex("[A-F0-9]{64}"))) {
            "Manifesto de texto integral com hash inválido."
        }
        return FullTextManifest(
            catalogVersion = catalogVersion,
            generatedAt = root.optString("generatedAt").ifBlank {
                catalogVersion.take(10)
            },
            version =
                catalogVersion + "-fulltext-" +
                    contentSha256.take(12).lowercase(),
            contentSha256 = contentSha256,
            recordCount = root.getInt("recordCount"),
            chunks = root.getJSONArray("chunks").objects().map {
                FullTextChunk(
                    file = it.getString("file"),
                    recordCount = it.getInt("recordCount"),
                    contentSha256 = it.getString("contentSha256"),
                )
            },
        )
    }

    private fun parseLinksIfCompatible(
        json: String,
        expectedCatalogVersion: String,
        installedMbftVersion: String?,
    ): LinkImport {
        val root = JSONObject(json)
        val version = root.getString("version")
        val compatible =
            root.getString("catalogVersion") == expectedCatalogVersion &&
                root.getString("mbftVersion") == installedMbftVersion

        if (!compatible) {
            return LinkImport(
                version = version + "-skipped-incompatible",
                records = emptyList(),
            )
        }

        val contentSha256 = root.getString("contentSha256")
        require(contentSha256.matches(Regex("[A-F0-9]{64}"))) {
            "Dataset de vínculos normativos com hash inválido."
        }

        val records = root.getJSONArray("records").objects().map {
            InfractionNormLinkEntity(
                infractionCode = it.getString("infractionCode"),
                actId = it.getString("actId"),
                relationKind = it.getString("relationKind"),
                evidence = it.getString("evidence"),
                officialUrl = it.getString("officialUrl"),
            )
        }
        require(records.size == root.getInt("recordCount")) {
            "Contagem divergente no dataset de vínculos normativos."
        }
        require(
            records.all {
                it.relationKind in setOf(
                    "EXPLICIT_MBFT_REFERENCE",
                    "DIRECT_CTB_ARTICLE_REFERENCE",
                )
            },
        ) {
            "Dataset de vínculos contém origem não auditável."
        }

        return LinkImport(
            version = version,
            records = records,
        )
    }

    private fun readAssetOrNull(path: String): String? =
        runCatching {
            context.assets.open(path).bufferedReader().use { it.readText() }
        }.getOrNull()

    private fun readGzipAsset(path: String): ByteArray =
        context.assets.open(path).use { input ->
            GZIPInputStream(input).use { gzip ->
                ByteArrayOutputStream().use { output ->
                    gzip.copyTo(output)
                    output.toByteArray()
                }
            }
        }

    private fun sha256(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value)
            .joinToString("") { "%02X".format(it) }

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).map(::getJSONObject)

    private companion object {
        const val NORMATIVE_DATASET_ID = "normative-catalog"
        const val MBFT_DATASET_ID = "mbft"
        const val FULLTEXT_DATASET_ID = "normative-fulltext"
        const val LINKS_DATASET_ID = "infraction-norm-links"
        const val MANIFEST_PATH = "norms/fulltext/manifest.json"
        const val LINKS_PATH = "infraction_norm_links.json"
    }
}

private data class FullTextManifest(
    val catalogVersion: String,
    val generatedAt: String,
    val version: String,
    val contentSha256: String,
    val recordCount: Int,
    val chunks: List<FullTextChunk>,
)

private data class FullTextChunk(
    val file: String,
    val recordCount: Int,
    val contentSha256: String,
)


private data class LinkImport(
    val version: String,
    val records: List<InfractionNormLinkEntity>,
)
