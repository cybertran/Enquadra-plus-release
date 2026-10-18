package com.cybertran.enquadraplus.data.update

enum class UpdateDataset(
    val id: String,
    val displayName: String,
    val bundledAssetPath: String,
) {
    CTB(
        id = "ctb",
        displayName = "CTB",
        bundledAssetPath = "ctb/articles.json",
    ),
    NORMATIVE_CATALOG(
        id = "normative-catalog",
        displayName = "CONTRAN / SENATRAN",
        bundledAssetPath = "norms/acts.json",
    ),
    MBFT(
        id = "mbft",
        displayName = "MBFT",
        bundledAssetPath = "mbft/infractions.json",
    ),
}

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    CANDIDATE_APPROVED,
    CANDIDATE_BLOCKED,
    APPLYING,
    APPLIED,
    ROLLING_BACK,
    ROLLED_BACK,
    PROTECTED,
    SOURCE_UNAVAILABLE,
    ERROR,
}

data class UpdateDiff(
    val activeCount: Int,
    val candidateCount: Int,
    val added: Int,
    val changed: Int,
    val removed: Int,
    val brokenReferences: Int = 0,
) {
    val hasSemanticChanges: Boolean
        get() = added != 0 || changed != 0 || removed != 0 || brokenReferences != 0

    fun shortSummary(): String =
        "adicionados=$added, alterados=$changed, removidos=$removed, referências quebradas=$brokenReferences"
}

data class DatasetUpdateState(
    val dataset: UpdateDataset,
    val status: UpdateStatus = UpdateStatus.IDLE,
    val activeVersion: String? = null,
    val previousVersion: String? = null,
    val candidateVersion: String? = null,
    val lastCheckedAt: String? = null,
    val message: String = "",
    val diff: UpdateDiff? = null,
) {
    val canApply: Boolean
        get() = status == UpdateStatus.CANDIDATE_APPROVED && candidateVersion != null

    val canRollback: Boolean
        get() = previousVersion != null &&
            status !in setOf(UpdateStatus.CHECKING, UpdateStatus.APPLYING, UpdateStatus.ROLLING_BACK)
}

data class ValidationIssue(
    val code: String,
    val message: String,
)

data class ValidationReport(
    val issues: List<ValidationIssue>,
) {
    val isValid: Boolean
        get() = issues.isEmpty()

    fun requireValid(prefix: String) {
        require(isValid) {
            buildString {
                append(prefix)
                if (issues.isNotEmpty()) {
                    append(": ")
                    append(issues.joinToString(" | ") { "${it.code}: ${it.message}" })
                }
            }
        }
    }

    companion object {
        val OK = ValidationReport(emptyList())
    }
}

data class OfficialFetchResult(
    val requestedUrl: String,
    val finalUrl: String,
    val contentType: String,
    val bytes: ByteArray,
    val sha256: String,
) {
    fun decodeText(): String {
        val headerCharset = Regex(
            pattern = """charset\s*=\s*["']?([A-Za-z0-9._-]+)""",
            option = RegexOption.IGNORE_CASE,
        ).find(contentType)?.groupValues?.getOrNull(1)

        val asciiHead = bytes
            .copyOfRange(0, minOf(bytes.size, 4_096))
            .toString(Charsets.US_ASCII)
        val documentCharset = Regex(
            pattern = """charset\s*=\s*["']?([A-Za-z0-9._-]+)""",
            option = RegexOption.IGNORE_CASE,
        ).find(asciiHead)?.groupValues?.getOrNull(1)

        val candidates = buildList {
            headerCharset?.let(::add)
            documentCharset?.let(::add)
            add("UTF-8")
            add("windows-1252")
            add("ISO-8859-1")
        }.distinctBy(String::lowercase)

        candidates.forEach { name ->
            val decoded = runCatching {
                val decoder = java.nio.charset.Charset
                    .forName(name)
                    .newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            }.getOrNull()
            if (decoded != null) return decoded
        }

        error("A fonte oficial usa codificação de texto não reconhecida.")
    }
}
