package com.cybertran.enquadraplus.data.update

import org.json.JSONArray
import org.json.JSONObject

data class InfractionRelationUpdateRecord(
    val sourceCode: String,
    val targetCode: String,
    val relationType: String,
    val origin: String,
    val sourceField: String?,
    val reason: String?,
    val evidence: List<String>,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "sourceCode" to sourceCode,
            "targetCode" to targetCode,
            "relationType" to relationType,
            "origin" to origin,
            "sourceField" to sourceField,
            "reason" to reason,
            "evidence" to evidence,
        )
}

data class InfractionRelationsPayload(
    val version: String,
    val generatedAt: String,
    val verifiedAt: String,
    val mbftVersion: String,
    val sourceDocument: String,
    val contentSha256: String,
    val records: List<InfractionRelationUpdateRecord>,
) {
    val recordCount: Int
        get() = records.size

    fun toJson(): String =
        CanonicalJson.encode(
            mapOf(
                "version" to version,
                "generatedAt" to generatedAt,
                "verifiedAt" to verifiedAt,
                "mbftVersion" to mbftVersion,
                "sourceDocument" to sourceDocument,
                "contentSha256" to contentSha256,
                "recordCount" to records.size,
                "records" to records.map(InfractionRelationUpdateRecord::toMap),
            ),
        )
}

object InfractionRelationsUpdateCodec {

    fun parse(json: String): InfractionRelationsPayload {
        val root = JSONObject(json)
        val records = root.getJSONArray("records").objects().map { item ->
            InfractionRelationUpdateRecord(
                sourceCode = item.getString("sourceCode"),
                targetCode = item.getString("targetCode"),
                relationType = item.getString("relationType"),
                origin = item.getString("origin"),
                sourceField = item.nullableString("sourceField"),
                reason = item.nullableString("reason"),
                evidence = item.optJSONArray("evidence")?.strings().orEmpty(),
            )
        }
        return InfractionRelationsPayload(
            version = root.getString("version"),
            generatedAt = root.getString("generatedAt"),
            verifiedAt = root.getString("verifiedAt"),
            mbftVersion = root.getString("mbftVersion"),
            sourceDocument = root.getString("sourceDocument"),
            contentSha256 = root.getString("contentSha256"),
            records = records,
        ).also { payload ->
            require(root.getInt("recordCount") == payload.records.size) {
                "Contagem declarada das relações diverge dos registros."
            }
        }
    }

    fun build(mbft: MbftDatasetPayload): InfractionRelationsPayload {
        val validCodes = mbft.records.mapTo(mutableSetOf(), MbftUpdateRecord::code)
        val relationMap =
            linkedMapOf<Triple<String, String, String>, InfractionRelationUpdateRecord>()

        mbft.records.forEach { record ->
            if (record.status != "VERIFIED") return@forEach
            collectOfficialRelations(
                record = record,
                field = "whenNotToCite",
                lines = record.whenNotToCite,
                validCodes = validCodes,
                target = relationMap,
            )
            collectOfficialRelations(
                record = record,
                field = "procedures",
                lines = record.procedures,
                validCodes = validCodes,
                target = relationMap,
            )
        }

        val officialPairs = relationMap.values
            .filter { it.relationType in OFFICIAL_TYPES }
            .mapTo(mutableSetOf()) { it.sourceCode to it.targetCode }

        mbft.records.forEach { record ->
            if (record.status != "VERIFIED") return@forEach
            record.related.forEach relatedLoop@{ related ->
                if (
                    related.code !in validCodes ||
                    related.code == record.code ||
                    (record.code to related.code) in officialPairs
                ) {
                    return@relatedLoop
                }

                val key = Triple(record.code, related.code, "RELATED")
                val existing = relationMap[key]
                relationMap[key] = if (existing == null) {
                    InfractionRelationUpdateRecord(
                        sourceCode = record.code,
                        targetCode = related.code,
                        relationType = "RELATED",
                        origin = "CURATED_PROJECT_RELATION",
                        sourceField = "related",
                        reason = related.reason,
                        evidence = emptyList(),
                    )
                } else if (existing.reason.isNullOrBlank() && !related.reason.isNullOrBlank()) {
                    existing.copy(reason = related.reason)
                } else {
                    existing
                }
            }
        }

        val records = relationMap.values.sortedWith(
            compareBy<InfractionRelationUpdateRecord> { it.sourceCode }
                .thenBy { it.relationType }
                .thenBy { it.targetCode },
        )
        val digest = canonicalHash(records)
        val payload = InfractionRelationsPayload(
            version = "${mbft.verifiedAt}-relations-${digest.take(12).lowercase()}",
            generatedAt = mbft.verifiedAt,
            verifiedAt = mbft.verifiedAt,
            mbftVersion = mbft.version,
            sourceDocument =
                "Relações derivadas das fichas verificadas do Manual Brasileiro " +
                    "de Fiscalização de Trânsito",
            contentSha256 = digest,
            records = records,
        )
        validate(payload, mbft).requireValid("Relações derivadas inválidas")
        return payload
    }

    fun validate(
        payload: InfractionRelationsPayload,
        mbft: MbftDatasetPayload,
    ): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        val codes = mbft.records.mapTo(mutableSetOf(), MbftUpdateRecord::code)
        val verified = mbft.records
            .filter { it.status == "VERIFIED" }
            .mapTo(mutableSetOf(), MbftUpdateRecord::code)

        if (payload.mbftVersion != mbft.version) {
            issues += ValidationIssue(
                "relations.mbft_version",
                "Relações não correspondem à versão MBFT candidata.",
            )
        }

        val calculated = canonicalHash(payload.records)
        if (payload.contentSha256 != calculated) {
            issues += ValidationIssue(
                "relations.content_hash",
                "Hash das relações não confere.",
            )
        }
        val expectedVersion =
            "${payload.verifiedAt}-relations-${calculated.take(12).lowercase()}"
        if (payload.version != expectedVersion) {
            issues += ValidationIssue(
                "relations.version",
                "Versão das relações não corresponde ao hash.",
            )
        }

        val keys = payload.records.map {
            Triple(it.sourceCode, it.targetCode, it.relationType)
        }
        if (keys.size != keys.toSet().size) {
            issues += ValidationIssue(
                "relations.duplicate",
                "Relações contêm chaves duplicadas.",
            )
        }

        payload.records.forEach { record ->
            if (record.sourceCode !in codes || record.targetCode !in codes) {
                issues += ValidationIssue(
                    "relations.orphan",
                    "Relação órfã: ${record.sourceCode} → ${record.targetCode}.",
                )
            }
            if (record.sourceCode == record.targetCode) {
                issues += ValidationIssue(
                    "relations.self",
                    "Relação reflexiva inválida em ${record.sourceCode}.",
                )
            }
            if (record.relationType !in ALL_TYPES) {
                issues += ValidationIssue(
                    "relations.type",
                    "Tipo de relação inválido: ${record.relationType}.",
                )
            }

            if (record.relationType in OFFICIAL_TYPES) {
                if (record.sourceCode !in verified) {
                    issues += ValidationIssue(
                        "relations.unverified_source",
                        "Relação oficial parte de ficha não VERIFIED.",
                    )
                }
                if (record.origin != "MBFT_OFFICIAL_FIELD") {
                    issues += ValidationIssue(
                        "relations.origin",
                        "Relação oficial com origem inválida.",
                    )
                }
                if (record.sourceField !in setOf("whenNotToCite", "procedures")) {
                    issues += ValidationIssue(
                        "relations.source_field",
                        "Campo-fonte oficial inválido.",
                    )
                }
                if (record.evidence.isEmpty() || record.evidence.any {
                        record.targetCode !in CODE_REGEX.findAll(it).map(MatchResult::value).toSet()
                    }
                ) {
                    issues += ValidationIssue(
                        "relations.evidence",
                        "Evidência não menciona explicitamente o código relacionado.",
                    )
                }
            } else if (record.relationType == "RELATED") {
                if (
                    record.origin != "CURATED_PROJECT_RELATION" ||
                    record.reason.isNullOrBlank()
                ) {
                    issues += ValidationIssue(
                        "relations.curated",
                        "Relação curada sem origem/motivo válido.",
                    )
                }
            }
        }

        if (payload.records.none { it.relationType == "ALTERNATIVE" }) {
            issues += ValidationIssue("relations.alternative", "Sem relações alternativas.")
        }
        if (payload.records.none { it.relationType == "CONCOMITANT" }) {
            issues += ValidationIssue("relations.concomitant", "Sem relações concomitantes.")
        }
        if (payload.records.none { it.relationType == "PREREQUISITE" }) {
            issues += ValidationIssue("relations.prerequisite", "Sem relações de pré-requisito.")
        }

        return ValidationReport(issues)
    }

    fun unresolvedReferenceCount(mbft: MbftDatasetPayload): Int {
        val validCodes = mbft.records.mapTo(mutableSetOf(), MbftUpdateRecord::code)
        var count = 0
        mbft.records.forEach { record ->
            if (record.status != "VERIFIED") return@forEach
            listOf(
                "whenNotToCite" to record.whenNotToCite,
                "procedures" to record.procedures,
            ).forEach { (field, lines) ->
                lines.forEach { line ->
                    val relationType = classify(field, line) ?: return@forEach
                    val relevant = relevantText(line, relationType) ?: return@forEach
                    val missing = CODE_REGEX.findAll(relevant)
                        .map(MatchResult::value)
                        .filter { it !in validCodes && it != record.code }
                        .toSet()
                    if (missing.isNotEmpty()) count += 1
                }
            }
        }
        return count
    }

    fun canonicalHash(records: List<InfractionRelationUpdateRecord>): String =
        CanonicalJson.sha256Upper(
            CanonicalJson.encode(records.map(InfractionRelationUpdateRecord::toMap)),
        )

    private fun collectOfficialRelations(
        record: MbftUpdateRecord,
        field: String,
        lines: List<String>,
        validCodes: Set<String>,
        target: MutableMap<
            Triple<String, String, String>,
            InfractionRelationUpdateRecord,
            >,
    ) {
        lines.forEach { evidence ->
            val relationType = classify(field, evidence) ?: return@forEach
            val relevant = relevantText(evidence, relationType) ?: return@forEach
            val targets = CODE_REGEX.findAll(relevant)
                .map(MatchResult::value)
                .filter { it in validCodes && it != record.code }
                .toSortedSet()

            targets.forEach { targetCode ->
                val key = Triple(record.code, targetCode, relationType)
                val current = target[key]
                if (current == null) {
                    target[key] = InfractionRelationUpdateRecord(
                        sourceCode = record.code,
                        targetCode = targetCode,
                        relationType = relationType,
                        origin = "MBFT_OFFICIAL_FIELD",
                        sourceField = field,
                        reason = null,
                        evidence = listOf(evidence),
                    )
                } else if (evidence !in current.evidence) {
                    target[key] = current.copy(evidence = current.evidence + evidence)
                }
            }
        }
    }

    private fun classify(field: String, text: String): String? =
        when {
            field == "whenNotToCite" -> "ALTERNATIVE"
            field == "procedures" && CONCOMITANT_REGEX.containsMatchIn(text) -> "CONCOMITANT"
            field == "procedures" && PREREQUISITE_REGEX.containsMatchIn(text) -> "PREREQUISITE"
            else -> null
        }

    private fun relevantText(text: String, relationType: String): String? =
        when (relationType) {
            "CONCOMITANT" -> CONCOMITANT_REGEX.find(text)?.range?.first?.let(text::substring)
            "PREREQUISITE" -> PREREQUISITE_REGEX.find(text)?.range?.first?.let(text::substring)
            else -> text
        }

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key).takeIf(String::isNotBlank)

    private fun JSONArray.strings(): List<String> =
        (0 until length()).map(::getString)

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).map(::getJSONObject)

    private val CODE_REGEX = Regex("\\b\\d{3}-\\d{2}\\b")
    private val CONCOMITANT_REGEX = Regex(
        "(?:utilizar|usar)[^.;:]{0,80}concomitantemente" +
            "|lavrar\\s+tamb[eé]m(?:\\s+outro)?\\s+ait" +
            "|autuar\\s+tamb[eé]m" +
            "|lavrar\\s+outro\\s+ait",
        RegexOption.IGNORE_CASE,
    )
    private val PREREQUISITE_REGEX = Regex(
        "(precedid[ao]|preceder|deve ser precedida)",
        RegexOption.IGNORE_CASE,
    )
    private val OFFICIAL_TYPES = setOf("ALTERNATIVE", "CONCOMITANT", "PREREQUISITE")
    private val ALL_TYPES = OFFICIAL_TYPES + "RELATED"
}
