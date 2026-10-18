package com.cybertran.enquadraplus.data.update

import org.json.JSONArray
import org.json.JSONObject

data class MbftSourceRecord(
    val id: String,
    val agency: String,
    val document: String,
    val section: String?,
    val officialUrl: String?,
    val effectiveFrom: String?,
    val lastCheckedAt: String,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "agency" to agency,
            "document" to document,
            "section" to section,
            "officialUrl" to officialUrl,
            "effectiveFrom" to effectiveFrom,
            "lastCheckedAt" to lastCheckedAt,
        )
}

data class MbftRelatedRecord(
    val code: String,
    val reason: String?,
) {
    fun toMap(): Map<String, Any?> =
        mapOf("code" to code, "reason" to reason)
}

data class MbftUpdateRecord(
    val code: String,
    val status: String,
    val article: String,
    val shortDescription: String,
    val description: String,
    val severity: String,
    val points: Int?,
    val offenderResponsibility: String?,
    val authority: String?,
    val verificationMethod: String?,
    val penalty: String?,
    val administrativeMeasure: String?,
    val canConfigureTrafficCrime: Boolean,
    val whenToCite: List<String>,
    val whenNotToCite: List<String>,
    val procedures: List<String>,
    val aitObservations: List<String>,
    val related: List<MbftRelatedRecord>,
    val sources: List<MbftSourceRecord>,
    val legalBasis: List<String>,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "code" to code,
            "status" to status,
            "article" to article,
            "shortDescription" to shortDescription,
            "description" to description,
            "severity" to severity,
            "points" to points,
            "offenderResponsibility" to offenderResponsibility,
            "authority" to authority,
            "verificationMethod" to verificationMethod,
            "penalty" to penalty,
            "administrativeMeasure" to administrativeMeasure,
            "canConfigureTrafficCrime" to canConfigureTrafficCrime,
            "whenToCite" to whenToCite,
            "whenNotToCite" to whenNotToCite,
            "procedures" to procedures,
            "aitObservations" to aitObservations,
            "related" to related.map(MbftRelatedRecord::toMap),
            "sources" to sources.map(MbftSourceRecord::toMap),
            "legalBasis" to legalBasis,
        )
}

data class MbftDatasetPayload(
    val version: String,
    val generatedAt: String,
    val verifiedAt: String,
    val sourceDocument: String,
    val sourceUrl: String,
    val sourceSha256: String,
    val knownAmendments: List<String>,
    val appliedAmendments: List<String>,
    val contentSha256: String,
    val records: List<MbftUpdateRecord>,
    val legacyFormat: Boolean = false,
) {
    val recordCount: Int
        get() = records.size

    fun toJson(): String =
        CanonicalJson.encode(
            mapOf(
                "version" to version,
                "generatedAt" to generatedAt,
                "verifiedAt" to verifiedAt,
                "sourceDocument" to sourceDocument,
                "sourceUrl" to sourceUrl,
                "sourceSha256" to sourceSha256,
                "knownAmendments" to knownAmendments,
                "appliedAmendments" to appliedAmendments,
                "contentSha256" to contentSha256,
                "recordCount" to records.size,
                "records" to records.map(MbftUpdateRecord::toMap),
            ),
        )
}

data class MbftSupportedPatch(
    val number: Int,
    val year: Int,
    val resolution: String,
    val publicationDate: String,
    val effectiveFrom: String,
    val officialUrl: String,
    val expectedSha256: String,
    val removeCodes: List<String>,
    val upserts: List<MbftUpdateRecord>,
)

data class MbftPatchRegistry(
    val schemaVersion: Int,
    val parserVersion: String,
    val recognizedAmendments: Set<Pair<Int, Int>>,
    val patches: List<MbftSupportedPatch>,
)

object MbftUpdateCodec {

    const val PARSER_VERSION = "mbft-hash-patch-v1"
    const val REGISTRY_ASSET_PATH = "updates/mbft_supported_amendments.json"
    private const val MIN_RECORDS = 400

    fun parse(json: String): MbftDatasetPayload {
        val root = JSONObject(json)
        val records = root.getJSONArray("records").objects().map(::parseRecord)
        val calculated = canonicalHash(records)
        val declared = root.optString("contentSha256")
            .takeIf { it.isNotBlank() }

        val payload = MbftDatasetPayload(
            version = root.getString("version"),
            generatedAt = root.getString("generatedAt"),
            verifiedAt = root.getString("verifiedAt"),
            sourceDocument = root.getString("sourceDocument"),
            sourceUrl = root.getString("sourceUrl"),
            sourceSha256 = root.getString("sourceSha256"),
            knownAmendments = root.optJSONArray("knownAmendments")?.strings().orEmpty(),
            appliedAmendments = root.optJSONArray("appliedAmendments")?.strings().orEmpty(),
            contentSha256 = declared ?: calculated,
            records = records,
            legacyFormat = declared == null,
        )

        root.optInt("recordCount", records.size).let { declaredCount ->
            require(declaredCount == records.size) {
                "Contagem declarada do MBFT diverge dos registros."
            }
        }
        return payload
    }

    fun parseRegistry(json: String): MbftPatchRegistry {
        val root = JSONObject(json)
        val parserVersion = root.getString("parserVersion")
        require(parserVersion == PARSER_VERSION) {
            "Registro de patches MBFT pertence a parser incompatível."
        }

        val recognized = root.getJSONArray("recognizedAmendments")
            .objects()
            .map { it.getInt("number") to it.getInt("year") }
            .toSet()

        val patches = root.getJSONArray("patches").objects().map { item ->
            MbftSupportedPatch(
                number = item.getInt("number"),
                year = item.getInt("year"),
                resolution = item.getString("resolution"),
                publicationDate = item.getString("publicationDate"),
                effectiveFrom = item.getString("effectiveFrom"),
                officialUrl = item.getString("officialUrl"),
                expectedSha256 = item.getString("expectedSha256"),
                removeCodes = item.getJSONArray("removeCodes").strings(),
                upserts = item.getJSONArray("upserts").objects().map(::parseRecord),
            )
        }

        return MbftPatchRegistry(
            schemaVersion = root.getInt("schemaVersion"),
            parserVersion = parserVersion,
            recognizedAmendments = recognized,
            patches = patches,
        ).also(::validateRegistry)
    }

    fun validate(payload: MbftDatasetPayload): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        val calculated = canonicalHash(payload.records)

        if (!payload.sourceSha256.isSha256()) {
            issues += ValidationIssue("mbft.source_hash", "Hash da fonte-base é inválido.")
        }
        if (!payload.contentSha256.isSha256()) {
            issues += ValidationIssue("mbft.content_hash", "Hash canônico é inválido.")
        }
        if (calculated != payload.contentSha256) {
            issues += ValidationIssue("mbft.content_hash_mismatch", "Hash canônico não confere.")
        }
        if (!payload.legacyFormat) {
            val expectedVersion =
                "${payload.verifiedAt}-mbft-${calculated.take(12).lowercase()}"
            if (payload.version != expectedVersion) {
                issues += ValidationIssue(
                    "mbft.version",
                    "Versão não corresponde ao conteúdo canônico.",
                )
            }
        }

        if (payload.records.size < MIN_RECORDS) {
            issues += ValidationIssue(
                "mbft.record_count",
                "Base inesperadamente pequena: ${payload.records.size} fichas.",
            )
        }

        val duplicateCodes = payload.records
            .groupingBy(MbftUpdateRecord::code)
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicateCodes.isNotEmpty()) {
            issues += ValidationIssue(
                "mbft.duplicate_codes",
                "Códigos duplicados: ${duplicateCodes.take(10).joinToString()}.",
            )
        }

        val known = payload.knownAmendments.toSet()
        payload.appliedAmendments.filterNot(known::contains).forEach { amendment ->
            issues += ValidationIssue(
                "mbft.applied_not_known",
                "Emenda aplicada não consta como conhecida: $amendment",
            )
        }

        payload.records.forEach { record ->
            if (!CODE_REGEX.matches(record.code)) {
                issues += ValidationIssue("mbft.code", "Código inválido: ${record.code}")
            }
            if (record.status !in VALID_STATUSES) {
                issues += ValidationIssue(
                    "mbft.status",
                    "Status inválido em ${record.code}: ${record.status}",
                )
            }
            if (
                record.article.isBlank() ||
                record.shortDescription.isBlank() ||
                record.description.isBlank() ||
                record.severity.isBlank()
            ) {
                issues += ValidationIssue(
                    "mbft.required_field",
                    "Ficha ${record.code} contém campo obrigatório vazio.",
                )
            }
            if (record.points != null && record.points !in VALID_POINTS) {
                issues += ValidationIssue(
                    "mbft.points",
                    "Pontuação inválida em ${record.code}.",
                )
            }
            if (record.sources.isEmpty()) {
                issues += ValidationIssue(
                    "mbft.sources",
                    "Ficha ${record.code} sem fonte oficial.",
                )
            }

            record.related.forEach { relation ->
                if (!CODE_REGEX.matches(relation.code) || relation.code == record.code) {
                    issues += ValidationIssue(
                        "mbft.related",
                        "Relação editorial inválida em ${record.code}.",
                    )
                }
            }

            record.sources.forEach { source ->
                if (
                    source.id.isBlank() ||
                    source.agency.isBlank() ||
                    source.document.isBlank() ||
                    source.lastCheckedAt.isBlank()
                ) {
                    issues += ValidationIssue(
                        "mbft.source_fields",
                        "Fonte incompleta em ${record.code}.",
                    )
                }
                source.officialUrl?.let { url ->
                    if (!isApprovedMbftDocumentUrl(url)) {
                        issues += ValidationIssue(
                            "mbft.source_url",
                            "Fonte não autorizada em ${record.code}: $url",
                        )
                    }
                }
            }
        }

        return ValidationReport(issues)
    }

    fun validateTransition(
        active: MbftDatasetPayload,
        candidate: MbftDatasetPayload,
        diff: UpdateDiff,
    ): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()

        if (candidate.records.size < active.records.size - 5) {
            issues += ValidationIssue(
                "mbft.regression_count",
                "A candidata reduziu anormalmente o número de fichas.",
            )
        }
        if (diff.removed > 5) {
            issues += ValidationIssue(
                "mbft.mass_removal",
                "Remoção em massa detectada: ${diff.removed} fichas.",
            )
        }
        if (diff.changed > 40) {
            issues += ValidationIssue(
                "mbft.mass_change",
                "Mudança em massa detectada: ${diff.changed} fichas.",
            )
        }
        if (diff.added > 20) {
            issues += ValidationIssue(
                "mbft.mass_addition",
                "Inclusão anormalmente grande: ${diff.added} fichas.",
            )
        }
        if (diff.brokenReferences > 0) {
            issues += ValidationIssue(
                "mbft.broken_references",
                "A candidata criou ${diff.brokenReferences} novas referências não resolvidas.",
            )
        }

        return ValidationReport(issues)
    }

    fun diff(
        active: MbftDatasetPayload,
        candidate: MbftDatasetPayload,
        brokenReferences: Int = 0,
    ): UpdateDiff {
        val activeByCode = active.records.associateBy(MbftUpdateRecord::code)
        val candidateByCode = candidate.records.associateBy(MbftUpdateRecord::code)
        val added = candidateByCode.keys.count { it !in activeByCode }
        val removed = activeByCode.keys.count { it !in candidateByCode }
        val changed = activeByCode.keys.intersect(candidateByCode.keys).count { code ->
            CanonicalJson.encode(activeByCode.getValue(code).toMap()) !=
                CanonicalJson.encode(candidateByCode.getValue(code).toMap())
        }

        return UpdateDiff(
            activeCount = active.records.size,
            candidateCount = candidate.records.size,
            added = added,
            changed = changed,
            removed = removed,
            brokenReferences = brokenReferences,
        )
    }

    fun applyPatch(
        active: MbftDatasetPayload,
        patch: MbftSupportedPatch,
        checkedAt: String,
        legalDate: String,
        resolutionFetch: OfficialFetchResult,
    ): MbftDatasetPayload {
        require(ISO_DATE.matches(legalDate) && legalDate >= patch.effectiveFrom) {
            "Emenda ${patch.resolution} ainda não está em vigor para o MBFT."
        }
        require(resolutionFetch.requestedUrl == patch.officialUrl) {
            "Fonte principal da emenda não corresponde ao registro aprovado."
        }
        OfficialSourcePolicy.validate(
            resolutionFetch.finalUrl,
            OfficialSourceKind.MBFT_PDF,
        )
        require(resolutionFetch.sha256 == patch.expectedSha256) {
            "Hash do ato oficial com o Anexo III diverge do artefato auditado."
        }

        val records = active.records.associateBy(MbftUpdateRecord::code).toMutableMap()
        patch.removeCodes.forEach { code ->
            require(records.remove(code) != null) {
                "Patch esperava remover código inexistente: $code"
            }
        }
        patch.upserts.forEach { record ->
            records[record.code] = record.copy(
                sources = record.sources.map { source ->
                    source.copy(lastCheckedAt = checkedAt)
                },
            )
        }

        val sorted = records.values.sortedBy(MbftUpdateRecord::code)
        val digest = canonicalHash(sorted)
        val known = (active.knownAmendments + patch.resolution).distinct()
        val applied = (active.appliedAmendments + patch.resolution).distinct()

        return MbftDatasetPayload(
            version = "$checkedAt-mbft-${digest.take(12).lowercase()}",
            generatedAt = checkedAt,
            verifiedAt = checkedAt,
            sourceDocument = active.sourceDocument,
            sourceUrl = active.sourceUrl,
            sourceSha256 = active.sourceSha256,
            knownAmendments = known,
            appliedAmendments = applied,
            contentSha256 = digest,
            records = sorted,
            legacyFormat = false,
        ).also { candidate ->
            validate(candidate).requireValid("Candidata MBFT inválida")
        }
    }

    fun canonicalHash(records: List<MbftUpdateRecord>): String =
        CanonicalJson.sha256Upper(
            CanonicalJson.encode(records.map(MbftUpdateRecord::toMap)),
        )

    fun findPatch(
        registry: MbftPatchRegistry,
        number: Int,
        year: Int,
    ): MbftSupportedPatch? =
        registry.patches.firstOrNull { it.number == number && it.year == year }

    fun amendmentKey(value: String): Pair<Int, Int>? {
        val match = AMENDMENT_KEY.find(value) ?: return null
        val number = match.groupValues[1].replace(".", "").toIntOrNull() ?: return null
        val year = match.groupValues[2].toIntOrNull() ?: return null
        if (number !in 1..9_999 || year !in 1900..2_100) return null
        return number to year
    }

    private fun validateRegistry(registry: MbftPatchRegistry) {
        require(registry.schemaVersion == 1) {
            "Schema do registro de patches MBFT não suportado."
        }
        require(registry.parserVersion == PARSER_VERSION) {
            "Versão do parser MBFT divergente."
        }

        val patchKeys = registry.patches.map { it.number to it.year }
        require(patchKeys.size == patchKeys.toSet().size) {
            "Registro MBFT contém patches duplicados."
        }
        require(registry.recognizedAmendments.containsAll(patchKeys)) {
            "Patch MBFT não consta entre emendas reconhecidas."
        }

        registry.patches.forEach { patch ->
            require(ISO_DATE.matches(patch.publicationDate) && ISO_DATE.matches(patch.effectiveFrom)) {
                "Datas de vigência inválidas em ${patch.resolution}."
            }
            require(patch.effectiveFrom >= patch.publicationDate) {
                "Vigência anterior à publicação em ${patch.resolution}."
            }
            OfficialSourcePolicy.validate(
                patch.officialUrl,
                OfficialSourceKind.MBFT_PDF,
            )
            require(patch.expectedSha256.isSha256()) {
                "Hash principal inválido em ${patch.resolution}."
            }
            require(patch.removeCodes.all(CODE_REGEX::matches)) {
                "Patch contém código inválido para remoção."
            }
            require(patch.upserts.isNotEmpty()) {
                "Patch MBFT sem fichas auditadas."
            }
            patch.upserts.forEach { record ->
                require(record.status == "VERIFIED") {
                    "Patch só pode promover ficha previamente auditada como VERIFIED."
                }
            }
        }
    }

    private fun parseRecord(json: JSONObject): MbftUpdateRecord =
        MbftUpdateRecord(
            code = json.getString("code"),
            status = json.getString("status"),
            article = json.getString("article"),
            shortDescription = json.getString("shortDescription"),
            description = json.getString("description"),
            severity = json.getString("severity"),
            points = if (json.isNull("points")) null else json.getInt("points"),
            offenderResponsibility = json.nullableString("offenderResponsibility"),
            authority = json.nullableString("authority"),
            verificationMethod = json.nullableString("verificationMethod"),
            penalty = json.nullableString("penalty"),
            administrativeMeasure = json.nullableString("administrativeMeasure"),
            canConfigureTrafficCrime = json.optBoolean("canConfigureTrafficCrime", false),
            whenToCite = json.optJSONArray("whenToCite")?.strings().orEmpty(),
            whenNotToCite = json.optJSONArray("whenNotToCite")?.strings().orEmpty(),
            procedures = json.optJSONArray("procedures")?.strings().orEmpty(),
            aitObservations = json.optJSONArray("aitObservations")?.strings().orEmpty(),
            related = json.optJSONArray("related")?.objects()?.map { item ->
                MbftRelatedRecord(
                    code = item.getString("code"),
                    reason = item.nullableString("reason"),
                )
            }.orEmpty(),
            sources = json.getJSONArray("sources").objects().map { item ->
                MbftSourceRecord(
                    id = item.getString("id"),
                    agency = item.getString("agency"),
                    document = item.getString("document"),
                    section = item.nullableString("section"),
                    officialUrl = item.nullableString("officialUrl"),
                    effectiveFrom = item.nullableString("effectiveFrom"),
                    lastCheckedAt = item.getString("lastCheckedAt"),
                )
            },
            legalBasis = json.optJSONArray("legalBasis")?.strings()
                .orEmpty()
                .ifEmpty { listOf(json.getString("article")) },
        )

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key).takeIf(String::isNotBlank)

    private fun JSONArray.strings(): List<String> =
        (0 until length()).map(::getString)

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).map(::getJSONObject)

    private fun String.isSha256(): Boolean =
        matches(Regex("[A-F0-9]{64}"))

    private val CODE_REGEX = Regex("\\d{3}-\\d{2}")
    private val ISO_DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
    private val AMENDMENT_KEY =
        Regex("""(\d{1,4}|\d{1,3}(?:\.\d{3})+)/(\d{4})""")
    private val VALID_STATUSES = setOf("VERIFIED", "PARTIAL_VALIDATION")
    private val VALID_POINTS = setOf(2, 3, 4, 5, 7)
}

private val AUDITED_PARTICIPA_MBFT_FICHE_PATHS = setOf(
    "/participamaisbrasil/blob/baixar/6308",
    "/participamaisbrasil/blob/baixar/6392",
    "/participamaisbrasil/blob/baixar/6456",
    "/participamaisbrasil/blob/baixar/6458",
)

internal fun isApprovedMbftDocumentUrl(url: String): Boolean =
    runCatching {
        val uri = java.net.URI(url)
        val host = uri.host?.lowercase()
        val approvedPath =
            uri.path.startsWith("/transportes/pt-br/assuntos/transito/") ||
                uri.path in AUDITED_PARTICIPA_MBFT_FICHE_PATHS
        uri.scheme.equals("https", ignoreCase = true) &&
            host in setOf("gov.br", "www.gov.br") &&
            approvedPath &&
            (uri.port == -1 || uri.port == 443) &&
            uri.userInfo == null &&
            uri.fragment == null &&
            uri.rawQuery == null &&
            uri.rawPath?.contains("..") != true
    }.getOrDefault(false)
