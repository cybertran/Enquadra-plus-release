package com.cybertran.enquadraplus.data.feedback

import android.content.Context
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

private const val INSTALLATION_ID_FILE = "feedback_installation_id"

object FeedbackContextProvider {
    @Synchronized
    fun installationId(context: Context): String =
        loadOrCreateInstallationId(
            file = File(context.noBackupFilesDir, INSTALLATION_ID_FILE),
        )

    fun diagnosticPayload(
        installationId: String,
        versionName: String,
        versionCode: Long,
    ): String =
        "Dados técnicos do app — installation_id=$installationId; app_version=$versionName; version_code=$versionCode"

    fun prefilledUrl(
        baseUrl: String,
        entryKey: String,
        diagnosticPayload: String,
    ): String =
        buildPrefilledFeedbackUrl(
            baseUrl = baseUrl,
            entryKey = entryKey,
            diagnosticPayload = diagnosticPayload,
        )
}

internal fun loadOrCreateInstallationId(
    file: File,
    idFactory: () -> String = { UUID.randomUUID().toString() },
): String {
    val existing = runCatching { file.readText().trim() }.getOrNull()
    if (existing != null && isUuid(existing)) {
        return existing
    }

    val generated = idFactory().also {
        require(isUuid(it)) { "Generated installation identifier must be a UUID." }
    }

    file.parentFile?.mkdirs()
    file.writeText(generated)
    return generated
}

internal fun buildPrefilledFeedbackUrl(
    baseUrl: String,
    entryKey: String,
    diagnosticPayload: String,
): String {
    require(entryKey.matches(Regex("""entry\.\d+"""))) {
        "Google Forms entry key must use the entry.<number> format."
    }

    val separator = if ('?' in baseUrl) '&' else '?'
    val encoded = URLEncoder.encode(
        diagnosticPayload,
        StandardCharsets.UTF_8.name(),
    )
    return "$baseUrl${separator}usp=pp_url&$entryKey=$encoded"
}

private fun isUuid(value: String): Boolean =
    runCatching { UUID.fromString(value).toString() == value.lowercase() }
        .getOrDefault(false)
