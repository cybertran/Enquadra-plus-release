package com.cybertran.enquadraplus.data.update

data class GenerationSourceEvidence(
    val requestedUrl: String,
    val finalUrl: String,
    val contentType: String,
    val sizeBytes: Int,
    val sha256: String,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "requestedUrl" to requestedUrl,
            "finalUrl" to finalUrl,
            "contentType" to contentType,
            "sizeBytes" to sizeBytes,
            "sha256" to sha256,
        )

    companion object {
        fun fromFetch(fetch: OfficialFetchResult): GenerationSourceEvidence =
            GenerationSourceEvidence(
                requestedUrl = fetch.requestedUrl,
                finalUrl = fetch.finalUrl,
                contentType = fetch.contentType,
                sizeBytes = fetch.bytes.size,
                sha256 = fetch.sha256,
            )
    }
}

data class GenerationManifest(
    val datasetId: String,
    val version: String,
    val schemaVersion: Int,
    val parserVersion: String,
    val generatedAt: String,
    val verifiedAt: String,
    val contentSha256: String,
    val recordCount: Int,
    val baseVersion: String?,
    val sources: List<GenerationSourceEvidence>,
    val validationCodes: List<String>,
    val diff: UpdateDiff?,
    val status: String,
) {
    fun toJson(): String =
        CanonicalJson.encode(
            mapOf(
                "datasetId" to datasetId,
                "version" to version,
                "schemaVersion" to schemaVersion,
                "parserVersion" to parserVersion,
                "generatedAt" to generatedAt,
                "verifiedAt" to verifiedAt,
                "contentSha256" to contentSha256,
                "recordCount" to recordCount,
                "baseVersion" to baseVersion,
                "sources" to sources.map(GenerationSourceEvidence::toMap),
                "validationCodes" to validationCodes,
                "diff" to diff?.let {
                    mapOf(
                        "activeCount" to it.activeCount,
                        "candidateCount" to it.candidateCount,
                        "added" to it.added,
                        "changed" to it.changed,
                        "removed" to it.removed,
                        "brokenReferences" to it.brokenReferences,
                    )
                },
                "status" to status,
            ),
        )

    companion object {
        const val SCHEMA_VERSION = 1

        fun bootstrap(
            dataset: UpdateDataset,
            version: String,
            generatedAt: String,
            verifiedAt: String,
            contentSha256: String,
            recordCount: Int,
        ): GenerationManifest =
            GenerationManifest(
                datasetId = dataset.id,
                version = version,
                schemaVersion = SCHEMA_VERSION,
                parserVersion = "bundled-audited-known-good",
                generatedAt = generatedAt,
                verifiedAt = verifiedAt,
                contentSha256 = contentSha256,
                recordCount = recordCount,
                baseVersion = null,
                sources = emptyList(),
                validationCodes = listOf("BUNDLED_AUDITED_KNOWN_GOOD"),
                diff = null,
                status = "ACTIVE_VERIFIED",
            )

        fun approvedCandidate(
            dataset: UpdateDataset,
            version: String,
            parserVersion: String,
            generatedAt: String,
            verifiedAt: String,
            contentSha256: String,
            recordCount: Int,
            baseVersion: String,
            sources: List<GenerationSourceEvidence>,
            validationCodes: List<String>,
            diff: UpdateDiff,
        ): GenerationManifest =
            GenerationManifest(
                datasetId = dataset.id,
                version = version,
                schemaVersion = SCHEMA_VERSION,
                parserVersion = parserVersion,
                generatedAt = generatedAt,
                verifiedAt = verifiedAt,
                contentSha256 = contentSha256,
                recordCount = recordCount,
                baseVersion = baseVersion,
                sources = sources,
                validationCodes = validationCodes,
                diff = diff,
                status = "CANDIDATE_APPROVED",
            )
    }
}
