package com.cybertran.enquadraplus.data.update

import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URI
import java.text.Normalizer

data class NormativeDatasetPayload(
    val version: String,
    val generatedAt: String,
    val verifiedAt: String,
    val sourceDocument: String,
    val contentSha256: String,
    val sources: List<NormativeUpdateSource>,
    val records: List<NormativeUpdateRecord>,
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
                "recordCount" to records.size,
                "contentSha256" to contentSha256,
                "sources" to sources.map(NormativeUpdateSource::toMap),
                "records" to records.map(NormativeUpdateRecord::toMap),
            ),
        )
}

data class NormativeUpdateSource(
    val agency: String,
    val actType: String,
    val year: Int?,
    val url: String,
    val sourceSha256: String,
    val recordCount: Int,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "agency" to agency,
            "actType" to actType,
            "year" to year,
            "url" to url,
            "sourceSha256" to sourceSha256,
            "recordCount" to recordCount,
        )
}

private val SENATRAN_PORTARIA_PDF_RE =
    Regex("""(?i)portaria[_-]?(\d+)[_-]?(\d{4})\.pdf$""")

private fun hasMismatchedSenatranPortariaDocument(
    url: String,
    number: Int,
    year: Int,
): Boolean {
    val filename = runCatching { URI(url).path.substringAfterLast('/') }
        .getOrNull()
        .orEmpty()
    val match = SENATRAN_PORTARIA_PDF_RE.find(filename) ?: return false
    val linkedNumber = match.groupValues[1].toIntOrNull() ?: return false
    val linkedYear = match.groupValues[2].toIntOrNull() ?: return false
    return linkedNumber != number || linkedYear != year
}

data class NormativeUpdateRecord(
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
    fun toMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "agency" to agency,
            "actType" to actType,
            "number" to number,
            "numberLabel" to numberLabel,
            "year" to year,
            "actDate" to actDate,
            "publicationDate" to publicationDate,
            "subject" to subject,
            "status" to status,
            "officialUrl" to officialUrl,
            "sourcePageUrl" to sourcePageUrl,
            "lastCheckedAt" to lastCheckedAt,
        )
}

object NormativeUpdateCodec {

    const val PARSER_VERSION = "normative-html-v2"

    private const val SOURCE_DOCUMENT =
        "Índices oficiais de Resoluções e Deliberações CONTRAN e Portarias SENATRAN"

    fun build(
        pages: List<Pair<OfficialSourceSpec, OfficialFetchResult>>,
        checkedAt: String,
    ): NormativeDatasetPayload {
        val allRecords = mutableListOf<NormativeUpdateRecord>()
        val sources = mutableListOf<NormativeUpdateSource>()

        pages.forEach { (spec, fetch) ->
            OfficialSourcePolicy.validate(
                fetch.finalUrl,
                OfficialSourceKind.TRANSPORT_HTML,
            )

            val isContranResolution =
                spec.url == OfficialSourcePolicy.CONTRAN_INDEX_URL
            val isContranDeliberation =
                spec.url == OfficialSourcePolicy.CONTRAN_DELIBERATIONS_INDEX_URL
            val year =
                if (isContranResolution || isContranDeliberation) {
                    null
                } else {
                    parseSourceYear(spec.url)
                }
            val records = when {
                isContranResolution ->
                    NormativeHtmlParser.parseContran(
                        pageUrl = spec.url,
                        sourceHtml = fetch.decodeText(),
                        checkedAt = checkedAt,
                    )
                isContranDeliberation ->
                    NormativeHtmlParser.parseContranDeliberations(
                        pageUrl = spec.url,
                        sourceHtml = fetch.decodeText(),
                        checkedAt = checkedAt,
                    )
                else ->
                    NormativeHtmlParser.parseSenatran(
                        pageUrl = spec.url,
                        sourceHtml = fetch.decodeText(),
                        checkedAt = checkedAt,
                        year = requireNotNull(year),
                    )
            }

            require(records.isNotEmpty()) {
                "Nenhum ato foi extraído de ${spec.url}."
            }

            allRecords += records
            sources += NormativeUpdateSource(
                agency = if (isContranResolution || isContranDeliberation) {
                    "CONTRAN"
                } else {
                    "SENATRAN"
                },
                actType = when {
                    isContranResolution -> "RESOLUÇÃO"
                    isContranDeliberation -> "DELIBERAÇÃO"
                    else -> "PORTARIA"
                },
                year = year,
                url = spec.url,
                sourceSha256 = fetch.sha256,
                recordCount = records.size,
            )
        }

        val records = dedupeAndSort(allRecords)
        val contentSha = CanonicalJson.sha256Upper(
            CanonicalJson.encode(records.map(NormativeUpdateRecord::toMap)),
        )
        val dataset = NormativeDatasetPayload(
            version = "$checkedAt-norms-${contentSha.take(12).lowercase()}",
            generatedAt = checkedAt,
            verifiedAt = checkedAt,
            sourceDocument = SOURCE_DOCUMENT,
            contentSha256 = contentSha,
            sources = sources,
            records = records,
        )
        validate(dataset).requireValid("Candidata normativa inválida")
        return dataset
    }

    fun parse(json: String): NormativeDatasetPayload {
        val root = JSONObject(json)
        val sourceArray = root.getJSONArray("sources")
        val sources = (0 until sourceArray.length()).map { index ->
            val item = sourceArray.getJSONObject(index)
            NormativeUpdateSource(
                agency = item.getString("agency"),
                actType = item.getString("actType"),
                year = if (item.isNull("year")) null else item.getInt("year"),
                url = item.getString("url"),
                sourceSha256 = item.getString("sourceSha256"),
                recordCount = item.getInt("recordCount"),
            )
        }
        val recordArray = root.getJSONArray("records")
        val records = (0 until recordArray.length()).map { index ->
            val item = recordArray.getJSONObject(index)
            NormativeUpdateRecord(
                id = item.getString("id"),
                agency = item.getString("agency"),
                actType = item.getString("actType"),
                number = item.getInt("number"),
                numberLabel = item.getString("numberLabel"),
                year = item.getInt("year"),
                actDate = item.nullableString("actDate"),
                publicationDate = item.nullableString("publicationDate"),
                subject = item.getString("subject"),
                status = item.nullableString("status"),
                officialUrl = item.getString("officialUrl"),
                sourcePageUrl = item.getString("sourcePageUrl"),
                lastCheckedAt = item.getString("lastCheckedAt"),
            )
        }

        return NormativeDatasetPayload(
            version = root.getString("version"),
            generatedAt = root.getString("generatedAt"),
            verifiedAt = root.getString("verifiedAt"),
            sourceDocument = root.getString("sourceDocument"),
            contentSha256 = root.getString("contentSha256"),
            sources = sources,
            records = records,
        ).also { payload ->
            require(root.getInt("recordCount") == payload.records.size) {
                "Contagem declarada do catálogo normativo diverge dos registros."
            }
        }
    }

    fun validate(dataset: NormativeDatasetPayload): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()

        if (!dataset.contentSha256.matches(Regex("[A-F0-9]{64}"))) {
            issues += ValidationIssue("norms.content_hash", "Hash de conteúdo inválido.")
        }

        val calculated = CanonicalJson.sha256Upper(
            CanonicalJson.encode(dataset.records.map(NormativeUpdateRecord::toMap)),
        )
        if (calculated != dataset.contentSha256) {
            issues += ValidationIssue("norms.content_hash_mismatch", "Hash canônico não confere.")
        }

        val expectedVersion =
            "${dataset.verifiedAt}-norms-${calculated.take(12).lowercase()}"
        if (dataset.version != expectedVersion) {
            issues += ValidationIssue("norms.version", "Versão não corresponde ao conteúdo canônico.")
        }

        if (dataset.sources.size < 8) {
            issues += ValidationIssue("norms.sources", "Conjunto de fontes oficiais está incompleto.")
        }
        if (dataset.records.size < 3_100) {
            issues += ValidationIssue(
                "norms.record_count",
                "Catálogo inesperadamente pequeno: ${dataset.records.size} atos.",
            )
        }
        if (dataset.sources.sumOf(NormativeUpdateSource::recordCount) != dataset.records.size) {
            issues += ValidationIssue(
                "norms.source_count",
                "Somatório das páginas-fonte diverge do catálogo.",
            )
        }

        dataset.sources.forEach { source ->
            runCatching {
                OfficialSourcePolicy.validate(source.url, OfficialSourceKind.TRANSPORT_HTML)
            }.onFailure {
                issues += ValidationIssue("norms.source_url", "Fonte não autorizada: ${source.url}")
            }
            if (!source.sourceSha256.matches(Regex("[A-F0-9]{64}"))) {
                issues += ValidationIssue("norms.source_hash", "Hash inválido: ${source.url}")
            }
            if (source.recordCount <= 0) {
                issues += ValidationIssue("norms.empty_source", "Fonte sem atos: ${source.url}")
            }
        }

        val duplicateIds = dataset.records
            .groupingBy(NormativeUpdateRecord::id)
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicateIds.isNotEmpty()) {
            issues += ValidationIssue(
                "norms.duplicate_ids",
                "IDs duplicados: ${duplicateIds.take(10).joinToString()}",
            )
        }

        val verifiedYear = dataset.verifiedAt.take(4).toIntOrNull()
        if (verifiedYear == null) {
            issues += ValidationIssue("norms.verified_at", "Data de verificação inválida.")
        }

        dataset.records.forEach { record ->
            if (record.agency !in setOf("CONTRAN", "SENATRAN")) {
                issues += ValidationIssue("norms.agency", "Órgão inválido em ${record.id}.")
            }
            if (record.actType !in setOf("RESOLUÇÃO", "DELIBERAÇÃO", "PORTARIA")) {
                issues += ValidationIssue("norms.type", "Tipo inválido em ${record.id}.")
            }
            if (record.number <= 0 || record.year < 1900) {
                issues += ValidationIssue("norms.identity", "Número/ano inválido em ${record.id}.")
            }
            if (verifiedYear != null && record.year > verifiedYear) {
                issues += ValidationIssue("norms.future_year", "Ano futuro em ${record.id}.")
            }
            if (record.subject.isBlank()) {
                issues += ValidationIssue("norms.subject", "Ato sem assunto: ${record.id}.")
            }
            if (!isOfficialTransportDocumentUrl(record.officialUrl)) {
                issues += ValidationIssue("norms.official_url", "URL inválida em ${record.id}.")
            }
            if (
                record.agency == "SENATRAN" &&
                record.actType == "PORTARIA" &&
                hasMismatchedSenatranPortariaDocument(
                    record.officialUrl,
                    record.number,
                    record.year,
                )
            ) {
                issues += ValidationIssue(
                    "norms.portaria_url_mismatch",
                    "Documento da Portaria não corresponde ao número/ano em ${record.id}.",
                )
            }
            runCatching {
                OfficialSourcePolicy.validate(
                    record.sourcePageUrl,
                    OfficialSourceKind.TRANSPORT_HTML,
                )
            }.onFailure {
                issues += ValidationIssue("norms.page_url", "Página-fonte inválida em ${record.id}.")
            }
        }

        REQUIRED_ACTS.forEach { required ->
            if (dataset.records.none {
                    it.agency == required.first &&
                        it.number == required.second &&
                        it.year == required.third
                }
            ) {
                issues += ValidationIssue(
                    "norms.required_act",
                    "Ato obrigatório ausente: ${required.first} ${required.second}/${required.third}.",
                )
            }
        }

        return ValidationReport(issues)
    }

    fun diff(
        active: NormativeDatasetPayload,
        candidate: NormativeDatasetPayload,
    ): UpdateDiff {
        val activeById = active.records.associateBy(NormativeUpdateRecord::id)
        val candidateById = candidate.records.associateBy(NormativeUpdateRecord::id)
        val added = candidateById.keys.count { it !in activeById }
        val removed = activeById.keys.count { it !in candidateById }
        val changed = activeById.keys.intersect(candidateById.keys).count { id ->
            val old = activeById.getValue(id).toMap() - "lastCheckedAt"
            val new = candidateById.getValue(id).toMap() - "lastCheckedAt"
            CanonicalJson.encode(old) != CanonicalJson.encode(new)
        }
        return UpdateDiff(
            activeCount = active.records.size,
            candidateCount = candidate.records.size,
            added = added,
            changed = changed,
            removed = removed,
        )
    }

    fun validateTransition(
        active: NormativeDatasetPayload,
        candidate: NormativeDatasetPayload,
        diff: UpdateDiff,
    ): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()

        if (diff.candidateCount < diff.activeCount) {
            issues += ValidationIssue(
                "norms.regression_count",
                "A candidata reduziu o total de atos.",
            )
        }
        if (diff.removed != 0) {
            issues += ValidationIssue(
                "norms.unexplained_removal",
                "Há ${diff.removed} atos ausentes na candidata.",
            )
        }
        if (candidate.sources.size < active.sources.size) {
            issues += ValidationIssue(
                "norms.source_regression",
                "A candidata perdeu páginas-fonte anteriormente cobertas.",
            )
        }

        val activeSourceUrls = active.sources.mapTo(mutableSetOf(), NormativeUpdateSource::url)
        val candidateSourceUrls = candidate.sources.mapTo(mutableSetOf(), NormativeUpdateSource::url)
        val missingSources = activeSourceUrls - candidateSourceUrls
        if (missingSources.isNotEmpty()) {
            issues += ValidationIssue(
                "norms.source_missing",
                "Páginas-fonte desapareceram: ${missingSources.joinToString()}",
            )
        }

        val maxChanged = maxOf(200, (diff.activeCount * 0.10).toInt())
        if (diff.changed > maxChanged) {
            issues += ValidationIssue(
                "norms.mass_change",
                "Mudança em massa detectada: ${diff.changed} atos.",
            )
        }
        if (diff.added > 1_500) {
            issues += ValidationIssue(
                "norms.mass_addition",
                "Inclusão anormalmente grande: ${diff.added} atos.",
            )
        }

        return ValidationReport(issues)
    }

    private fun dedupeAndSort(records: List<NormativeUpdateRecord>): List<NormativeUpdateRecord> {
        val byId = linkedMapOf<String, NormativeUpdateRecord>()
        records.forEach { record ->
            val existing = byId[record.id]
            require(existing == null || existing == record) {
                "Colisão normativa com conteúdo divergente: ${record.id}"
            }
            byId[record.id] = record
        }

        return byId.values.sortedWith(
            compareBy<NormativeUpdateRecord> { it.agency }
                .thenBy { it.actType }
                .thenByDescending { it.year }
                .thenByDescending { it.number }
                .thenBy { it.id },
        )
    }

    private fun parseSourceYear(url: String): Int =
        Regex("""portarias-(\d{4})$""")
            .find(URI(url).path)
            ?.groupValues
            ?.get(1)
            ?.toInt()
            ?: error("Ano da página SENATRAN não reconhecido: $url")

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key).takeIf(String::isNotBlank)

    private val REQUIRED_ACTS = setOf(
        Triple("CONTRAN", 985, 2022),
        Triple("CONTRAN", 1031, 2026),
        Triple("CONTRAN", 279, 2026),
        Triple("SENATRAN", 354, 2022),
    )
}

internal object NormativeHtmlParser {

    private data class Cell(
        val text: String,
        val href: String?,
    )

    fun parseContran(
        pageUrl: String,
        sourceHtml: String,
        checkedAt: String,
    ): List<NormativeUpdateRecord> {
        val rows = parseRows(sourceHtml)
        return buildList {
            rows.forEach { row ->
                if (row.size < 5) return@forEach
                val numberLabel = row[0]
                val actDate = row[1]
                val publicationDate = row[2]
                val subject = row[3]
                val status = row[4]

                val number = parseNumber(numberLabel.text) ?: return@forEach
                val year = parseYear(actDate.text)
                    ?: parseYear(publicationDate.text)
                    ?: return@forEach
                if (subject.text.isBlank()) return@forEach

                add(
                    NormativeUpdateRecord(
                        id = uniqueId(
                            agency = "CONTRAN",
                            actType = "RESOLUÇÃO",
                            year = year,
                            number = number,
                            numberLabel = numberLabel.text,
                        ),
                        agency = "CONTRAN",
                        actType = "RESOLUÇÃO",
                        number = number,
                        numberLabel = numberLabel.text,
                        year = year,
                        actDate = actDate.text.ifBlank { null },
                        publicationDate = publicationDate.text.ifBlank { null },
                        subject = subject.text,
                        status = status.text.takeUnless { it.isBlank() || it == "--" },
                        officialUrl = normalizeHref(pageUrl, numberLabel.href),
                        sourcePageUrl = pageUrl,
                        lastCheckedAt = checkedAt,
                    ),
                )
            }
        }
    }

    fun parseContranDeliberations(
        pageUrl: String,
        sourceHtml: String,
        checkedAt: String,
    ): List<NormativeUpdateRecord> {
        val rows = parseRows(sourceHtml)
        return buildList {
            rows.forEach { row ->
                if (row.size < 3) return@forEach
                val numberLabel = row[0]
                val publicationDate = row[1]
                val subject = row[2]
                val observation = row.getOrNull(3)

                if (!numberLabel.text.contains("delibera", ignoreCase = true)) {
                    return@forEach
                }

                val number = parseNumber(numberLabel.text) ?: return@forEach
                val year = parseYear(publicationDate.text) ?: return@forEach
                if (subject.text.isBlank()) return@forEach

                add(
                    NormativeUpdateRecord(
                        id = uniqueId(
                            agency = "CONTRAN",
                            actType = "DELIBERAÇÃO",
                            year = year,
                            number = number,
                            numberLabel = numberLabel.text,
                        ),
                        agency = "CONTRAN",
                        actType = "DELIBERAÇÃO",
                        number = number,
                        numberLabel = numberLabel.text,
                        year = year,
                        actDate = null,
                        publicationDate = publicationDate.text.ifBlank { null },
                        subject = subject.text,
                        status = observation?.text?.takeUnless { it.isBlank() || it == "--" },
                        officialUrl = normalizeHref(pageUrl, numberLabel.href),
                        sourcePageUrl = pageUrl,
                        lastCheckedAt = checkedAt,
                    ),
                )
            }
        }
    }

    fun parseSenatran(
        pageUrl: String,
        sourceHtml: String,
        checkedAt: String,
        year: Int,
    ): List<NormativeUpdateRecord> {
        val rows = parseRows(sourceHtml)
        return buildList {
            rows.forEach { row ->
                if (row.size < 3) return@forEach
                val numberLabel = row[0]
                val publicationDate = row[1]
                val subject = row[2]
                val observation = row.getOrNull(3)

                val number = parseNumber(numberLabel.text) ?: return@forEach
                if (subject.text.isBlank()) return@forEach

                add(
                    NormativeUpdateRecord(
                        id = uniqueId(
                            agency = "SENATRAN",
                            actType = "PORTARIA",
                            year = year,
                            number = number,
                            numberLabel = numberLabel.text,
                        ),
                        agency = "SENATRAN",
                        actType = "PORTARIA",
                        number = number,
                        numberLabel = numberLabel.text,
                        year = year,
                        actDate = null,
                        publicationDate = publicationDate.text.ifBlank { null },
                        subject = subject.text,
                        status = observation?.text?.takeUnless { it.isBlank() || it == "--" },
                        officialUrl = normalizePortariaHref(
                            pageUrl = pageUrl,
                            href = numberLabel.href,
                            number = number,
                            year = year,
                        ),
                        sourcePageUrl = pageUrl,
                        lastCheckedAt = checkedAt,
                    ),
                )
            }
        }
    }

    private fun parseRows(sourceHtml: String): List<List<Cell>> {
        val document = Jsoup.parse(sourceHtml)
        return document.select("tr").mapNotNull { row ->
            val cells = row.children()
                .filter { child -> child.tagName() == "td" || child.tagName() == "th" }
                .map { cell ->
                    Cell(
                        text = normalizeText(cell.text()),
                        href = cell.selectFirst("a[href]")?.attr("href")?.takeIf(String::isNotBlank),
                    )
                }
            cells.takeIf { it.isNotEmpty() }
        }
    }

    private fun normalizeText(value: String): String =
        value.replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun parseYear(value: String): Int? {
        val normalized = normalizeText(value)
        val short = Regex("""^(\d{1,2})[./](\d{1,2})[./](\d{2,4})$""")
            .matchEntire(normalized)
        if (short != null) {
            val raw = short.groupValues[3].toInt()
            return if (raw >= 100) raw else if (raw >= 90) 1900 + raw else 2000 + raw
        }

        return Regex("""\b(19\d{2}|20\d{2})\b""")
            .find(normalized)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    }

    private fun parseNumber(value: String): Int? =
        Regex("""(\d{1,4})""").find(value)?.groupValues?.get(1)?.toIntOrNull()

    private fun normalizeHref(
        pageUrl: String,
        href: String?,
    ): String {
        if (href.isNullOrBlank()) return pageUrl
        val target = runCatching { URI(pageUrl).resolve(href).toString() }.getOrNull()
            ?: return pageUrl
        return target.takeIf(::isOfficialTransportDocumentUrl) ?: pageUrl
    }

    private fun normalizePortariaHref(
        pageUrl: String,
        href: String?,
        number: Int,
        year: Int,
    ): String {
        val target = normalizeHref(pageUrl, href)
        if (target == pageUrl) return pageUrl
        return target.takeUnless {
            hasMismatchedSenatranPortariaDocument(it, number, year)
        } ?: pageUrl
    }

    private fun uniqueId(
        agency: String,
        actType: String,
        year: Int,
        number: Int,
        numberLabel: String,
    ): String {
        val base = "${slugify(agency)}-${slugify(actType)}-$year-$number"
        val remainder = normalizeText(
            Regex("""\d{1,4}""").replaceFirst(numberLabel, ""),
        )
        val suffix = slugify(remainder)
        return if (suffix.isNotBlank() && suffix != slugify(actType)) "$base-$suffix" else base
    }

    private fun slugify(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
        val ascii = normalized.filterNot { char ->
            Character.getType(char) == Character.NON_SPACING_MARK.toInt()
        }
        return ascii.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(80)
    }
}

internal fun isOfficialTransportDocumentUrl(url: String): Boolean =
    runCatching {
        val uri = URI(url)
        val host = uri.host?.lowercase()
        uri.scheme.equals("https", ignoreCase = true) &&
            host in setOf("gov.br", "www.gov.br") &&
            (uri.port == -1 || uri.port == 443) &&
            uri.userInfo == null &&
            uri.fragment == null &&
            uri.path.startsWith("/transportes/pt-br/assuntos/transito/") &&
            uri.rawPath?.contains("..") != true
    }.getOrDefault(false)
