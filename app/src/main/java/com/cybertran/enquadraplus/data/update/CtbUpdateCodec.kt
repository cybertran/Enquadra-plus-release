package com.cybertran.enquadraplus.data.update

import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

data class CtbDatasetPayload(
    val version: String,
    val generatedAt: String,
    val verifiedAt: String,
    val sourceDocument: String,
    val sourceUrl: String,
    val sourceSha256: String,
    val contentSha256: String,
    val records: List<CtbUpdateRecord>,
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
                "contentSha256" to contentSha256,
                "recordCount" to records.size,
                "records" to records.map(CtbUpdateRecord::toMap),
            ),
        )
}

data class CtbUpdateRecord(
    val id: String,
    val label: String,
    val number: Int,
    val suffix: String?,
    val chapter: String?,
    val section: String?,
    val subsection: String?,
    val text: String,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "label" to label,
            "number" to number,
            "suffix" to suffix,
            "chapter" to chapter,
            "section" to section,
            "subsection" to subsection,
            "text" to text,
        )
}

object CtbUpdateCodec {

    const val PARSER_VERSION = "ctb-html-v1"

    private const val SOURCE_DOCUMENT =
        "Lei nº 9.503/1997 — Código de Trânsito Brasileiro — texto compilado"

    fun build(
        fetch: OfficialFetchResult,
        checkedAt: String,
    ): CtbDatasetPayload {
        OfficialSourcePolicy.validate(
            fetch.finalUrl,
            OfficialSourceKind.CTB_HTML,
        )

        val records = CtbHtmlParser.parse(fetch.decodeText())
        val recordMaps = records.map(CtbUpdateRecord::toMap)
        val contentSha = CanonicalJson.sha256Upper(CanonicalJson.encode(recordMaps))

        val dataset = CtbDatasetPayload(
            version = "$checkedAt-planalto-${contentSha.take(12).lowercase()}",
            generatedAt = checkedAt,
            verifiedAt = checkedAt,
            sourceDocument = SOURCE_DOCUMENT,
            sourceUrl = OfficialSourcePolicy.CTB_URL,
            sourceSha256 = fetch.sha256,
            contentSha256 = contentSha,
            records = records,
        )
        validate(dataset).requireValid("Candidata CTB inválida")
        return dataset
    }

    fun parse(json: String): CtbDatasetPayload {
        val root = JSONObject(json)
        val recordsJson = root.getJSONArray("records")
        val records = (0 until recordsJson.length()).map { index ->
            val item = recordsJson.getJSONObject(index)
            CtbUpdateRecord(
                id = item.getString("id"),
                label = item.getString("label"),
                number = item.getInt("number"),
                suffix = item.nullableString("suffix"),
                chapter = item.nullableString("chapter"),
                section = item.nullableString("section"),
                subsection = item.nullableString("subsection"),
                text = item.getString("text"),
            )
        }

        return CtbDatasetPayload(
            version = root.getString("version"),
            generatedAt = root.getString("generatedAt"),
            verifiedAt = root.getString("verifiedAt"),
            sourceDocument = root.getString("sourceDocument"),
            sourceUrl = root.getString("sourceUrl"),
            sourceSha256 = root.getString("sourceSha256"),
            contentSha256 = root.getString("contentSha256"),
            records = records,
        ).also { payload ->
            require(root.getInt("recordCount") == payload.records.size) {
                "Contagem declarada do CTB diverge dos registros."
            }
        }
    }

    fun validate(dataset: CtbDatasetPayload): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()

        if (dataset.sourceUrl != OfficialSourcePolicy.CTB_URL) {
            issues += ValidationIssue("ctb.source", "URL de origem não é a compilação oficial esperada.")
        }
        if (!dataset.sourceSha256.isSha256()) {
            issues += ValidationIssue("ctb.source_hash", "Hash da fonte é inválido.")
        }
        if (!dataset.contentSha256.isSha256()) {
            issues += ValidationIssue("ctb.content_hash", "Hash canônico é inválido.")
        }

        val calculated = CanonicalJson.sha256Upper(
            CanonicalJson.encode(dataset.records.map(CtbUpdateRecord::toMap)),
        )
        if (calculated != dataset.contentSha256) {
            issues += ValidationIssue("ctb.content_hash_mismatch", "Hash canônico não confere.")
        }

        val expectedVersion =
            "${dataset.verifiedAt}-planalto-${calculated.take(12).lowercase()}"
        if (dataset.version != expectedVersion) {
            issues += ValidationIssue("ctb.version", "Versão não corresponde ao conteúdo canônico.")
        }

        if (dataset.records.size < 300) {
            issues += ValidationIssue(
                "ctb.record_count",
                "Foram extraídas apenas ${dataset.records.size} entradas.",
            )
        }

        val duplicateIds = dataset.records
            .groupingBy(CtbUpdateRecord::id)
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicateIds.isNotEmpty()) {
            issues += ValidationIssue(
                "ctb.duplicate_ids",
                "IDs duplicados: ${duplicateIds.take(10).joinToString()}",
            )
        }

        val presentIds = dataset.records.mapTo(mutableSetOf(), CtbUpdateRecord::id)
        REQUIRED_IDS.filterNot(presentIds::contains).forEach { id ->
            issues += ValidationIssue("ctb.required_id", "Entrada obrigatória ausente: $id")
        }

        REQUIRED_NUMBERS.filterNot { required ->
            dataset.records.any { it.id != "anexo-i" && it.number == required }
        }.forEach { number ->
            issues += ValidationIssue("ctb.required_article", "Art. $number ausente.")
        }

        dataset.records.forEach { record ->
            val isAnnex = record.id == "anexo-i"
            if (!isAnnex && !ARTICLE_ID.matches(record.id)) {
                issues += ValidationIssue("ctb.id", "ID inválido: ${record.id}")
            }
            if (record.text.isBlank() || record.text.length > 200_000) {
                issues += ValidationIssue("ctb.text", "Texto inválido ou anormal em ${record.id}.")
            }
            if (isAnnex && !record.text.startsWith("ANEXO I")) {
                issues += ValidationIssue("ctb.annex", "Anexo I não começa pelo cabeçalho esperado.")
            }
            if (!isAnnex && !record.text.startsWith("Art.")) {
                issues += ValidationIssue("ctb.article_text", "${record.id} não começa por Art.")
            }
        }

        return ValidationReport(issues)
    }

    fun diff(
        active: CtbDatasetPayload,
        candidate: CtbDatasetPayload,
    ): UpdateDiff {
        val activeById = active.records.associateBy(CtbUpdateRecord::id)
        val candidateById = candidate.records.associateBy(CtbUpdateRecord::id)
        val added = candidateById.keys.count { it !in activeById }
        val removed = activeById.keys.count { it !in candidateById }
        val changed = activeById.keys.intersect(candidateById.keys).count { id ->
            CanonicalJson.encode(activeById.getValue(id).toMap()) !=
                CanonicalJson.encode(candidateById.getValue(id).toMap())
        }

        return UpdateDiff(
            activeCount = active.records.size,
            candidateCount = candidate.records.size,
            added = added,
            changed = changed,
            removed = removed,
        )
    }

    fun validateDiff(diff: UpdateDiff): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        if (diff.candidateCount < diff.activeCount) {
            issues += ValidationIssue(
                "ctb.regression_count",
                "A candidata reduziu o número total de entradas.",
            )
        }
        if (diff.removed != 0) {
            issues += ValidationIssue(
                "ctb.unexplained_removal",
                "Há ${diff.removed} entradas ausentes na candidata.",
            )
        }

        val maxChanged = maxOf(40, (diff.activeCount * 0.15).toInt())
        if (diff.changed > maxChanged) {
            issues += ValidationIssue(
                "ctb.mass_change",
                "Mudança em massa detectada: ${diff.changed} entradas.",
            )
        }
        if (diff.added > 50) {
            issues += ValidationIssue(
                "ctb.mass_addition",
                "Inclusão anormalmente grande: ${diff.added} entradas.",
            )
        }

        return ValidationReport(issues)
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key).takeIf(String::isNotBlank)

    private fun String.isSha256(): Boolean = matches(Regex("[A-F0-9]{64}"))

    private val ARTICLE_ID = Regex("art-\\d+(?:-[a-z])?")

    private val REQUIRED_NUMBERS = setOf(
        1, 20, 61, 162, 165, 181, 230, 244, 302, 306, 320, 341,
    )

    private val REQUIRED_IDS = setOf(
        "anexo-i",
        "art-139-a",
        "art-139-b",
        "art-148-a",
        "art-165-a",
        "art-268-a",
        "art-282-a",
        "art-326-c",
    )
}

internal object CtbHtmlParser {

    private val articleRegex =
        Regex("""^Art\.\s*(\d+)(?:[ºo°])?(?:-([A-Z]))?\.?\s*(.*)$""")
    private val chapterRegex =
        Regex("""^CAP[IÍ]TULO\s+(?:[IVXLCDM]+|ÚNICO)(?:-[A-Z])?$""")
    private val sectionRegex =
        Regex("""^Se[cç][aã]o\s+(?:[IVXLCDM]+|Única)(?:-[A-Z])?$""", RegexOption.IGNORE_CASE)
    private val subsectionRegex =
        Regex("""^Subse[cç][aã]o\s+(?:[IVXLCDM]+|Única)(?:-[A-Z])?$""", RegexOption.IGNORE_CASE)

    private val breakTags = setOf(
        "p", "div", "br", "li", "tr", "td", "th",
        "h1", "h2", "h3", "h4", "h5", "h6", "center", "blockquote",
    )

    fun parse(sourceHtml: String): List<CtbUpdateRecord> {
        val lines = visibleLines(sourceHtml)
        val records = parseArticles(lines).toMutableList()
        records += parseAnnex(lines)
        return records
    }

    internal fun visibleLines(sourceHtml: String): List<String> {
        val document = Jsoup.parse(sourceHtml)
        document.select("script,style,noscript,strike,s,del").remove()
        document.getAllElements()
            .filter { it.attr("style").contains("line-through", ignoreCase = true) }
            .toList()
            .forEach { element -> element.remove() }

        val parts = StringBuilder()
        appendNode(document.body(), parts)

        val rawLines = parts.toString()
            .lineSequence()
            .map(::normalizeLine)
            .filter(String::isNotBlank)
            .toList()

        val result = mutableListOf<String>()
        var index = 0
        while (index < rawLines.size) {
            val line = rawLines[index]
            if (
                line == "Art." &&
                index + 1 < rawLines.size &&
                Regex("""^\d+(?:[ºo°])?(?:-[A-Z])?\.?""").containsMatchIn(rawLines[index + 1])
            ) {
                result += "Art. " + rawLines[index + 1]
                index += 2
            } else {
                result += line
                index += 1
            }
        }
        return result
    }

    private fun appendNode(
        node: Node,
        output: StringBuilder,
    ) {
        if (node is Element && node.tagName() in breakTags) output.append('\n')
        if (node is TextNode) output.append(node.wholeText)
        node.childNodes().forEach { child -> appendNode(child, output) }
        if (node is Element && node.tagName() in breakTags) output.append('\n')
    }

    private fun normalizeLine(value: String): String =
        value
            .replace('\u00A0', ' ')
            .replace("\u00AD", "")
            .replace(Regex("[ \\t\\r\\f\\u000B]+"), " ")
            .trim()

    private fun parseArticles(lines: List<String>): List<CtbUpdateRecord> {
        val articles = mutableListOf<CtbUpdateRecord>()
        var currentNumber: Int? = null
        var currentSuffix: String? = null
        var currentChapter: String? = null
        var currentSection: String? = null
        var currentSubsection: String? = null
        var articleChapter: String? = null
        var articleSection: String? = null
        var articleSubsection: String? = null
        var currentText = mutableListOf<String>()

        fun flush() {
            val number = currentNumber ?: return
            val text = currentText.joinToString("\n").trim()
            require(text.isNotBlank()) { "Artigo $number sem texto." }
            articles += CtbUpdateRecord(
                id = articleId(number, currentSuffix),
                label = articleLabel(number, currentSuffix),
                number = number,
                suffix = currentSuffix,
                chapter = articleChapter,
                section = articleSection,
                subsection = articleSubsection,
                text = text,
            )
            currentNumber = null
            currentSuffix = null
            currentText = mutableListOf()
        }

        for (line in lines) {
            if (chapterRegex.matches(line)) {
                flush()
                currentChapter = line
                currentSection = null
                currentSubsection = null
                continue
            }
            if (sectionRegex.matches(line)) {
                flush()
                currentSection = line
                currentSubsection = null
                continue
            }
            if (subsectionRegex.matches(line)) {
                flush()
                currentSubsection = line
                continue
            }

            val match = articleRegex.matchEntire(line)
            if (match != null) {
                flush()
                currentNumber = match.groupValues[1].toInt()
                currentSuffix = match.groupValues[2].ifBlank { null }?.uppercase()
                articleChapter = currentChapter
                articleSection = currentSection
                articleSubsection = currentSubsection
                currentText = mutableListOf(line)
                continue
            }

            if (currentNumber != null) {
                if (line.uppercase().startsWith("ANEXO I")) {
                    flush()
                    break
                }
                currentText += line
            }
        }
        flush()
        return articles
    }

    private fun parseAnnex(lines: List<String>): CtbUpdateRecord {
        val start = lines.indexOf("ANEXO I")
        require(start >= 0) { "Anexo I do CTB não encontrado." }

        val annexLines = mutableListOf<String>()
        for (line in lines.drop(start)) {
            if (annexLines.isNotEmpty() && line.startsWith("(Vide Resolução")) break
            annexLines += line
        }

        val text = annexLines.joinToString("\n").trim()
        require("DOS CONCEITOS E DEFINIÇÕES" in text) {
            "Cabeçalho do Anexo I incompleto."
        }
        require("ACOSTAMENTO -" in text && "VIADUTO -" in text) {
            "Definições do Anexo I parecem incompletas."
        }

        return CtbUpdateRecord(
            id = "anexo-i",
            label = "Anexo I",
            number = 10_000,
            suffix = null,
            chapter = "ANEXO I",
            section = "DOS CONCEITOS E DEFINIÇÕES",
            subsection = null,
            text = text,
        )
    }

    private fun articleId(number: Int, suffix: String?): String =
        "art-$number" + (suffix?.let { "-${it.lowercase()}" } ?: "")

    private fun articleLabel(number: Int, suffix: String?): String =
        when {
            suffix != null -> "Art. $number-$suffix"
            number in 1..9 -> "Art. ${number}º"
            else -> "Art. $number"
        }
}
