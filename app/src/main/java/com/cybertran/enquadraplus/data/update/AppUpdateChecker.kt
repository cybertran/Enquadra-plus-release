package com.cybertran.enquadraplus.data.update

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AppReleaseManifest(
    val schemaVersion: Int,
    val versionCode: Long,
    val versionName: String,
    val releaseUrl: String,
    val apkUrl: String,
    val apkSha256: String,
    val signingCertificateSha256: String,
)

sealed interface AppUpdateCheckResult {
    data class UpdateAvailable(val release: AppReleaseManifest) : AppUpdateCheckResult
    data class UpToDate(val release: AppReleaseManifest) : AppUpdateCheckResult
    data class NotConfirmed(val release: AppReleaseManifest) : AppUpdateCheckResult
}

class AppUpdateChecker(
    private val manifestUrl: URL = URL(DEFAULT_MANIFEST_URL),
    private val requestNonce: () -> Long = System::currentTimeMillis,
) {

    suspend fun check(installedVersionCode: Long): AppUpdateCheckResult =
        withContext(Dispatchers.IO) {
            require(installedVersionCode > 0) {
                "Installed versionCode must be positive."
            }

            val release = fetchManifest()
            classifyAppUpdate(release, installedVersionCode)
        }

    private fun fetchManifest(): AppReleaseManifest {
        val freshUrl = AppUpdateRequestPolicy.freshUrl(manifestUrl, requestNonce())
        val connection = freshUrl.openConnection() as? HttpsURLConnection
            ?: throw IOException("Update manifest must use HTTPS.")

        return try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.useCaches = false
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
            connection.setRequestProperty("Pragma", "no-cache")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "EnquadraPlus-AppUpdate")

            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw IOException("Unexpected update manifest response: $responseCode")
            }

            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_MANIFEST_BYTES) {
                throw IOException("Update manifest is too large.")
            }

            val body = connection.inputStream.use(::readLimitedUtf8)
            AppUpdateManifestCodec.decode(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun readLimitedUtf8(input: java.io.InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var total = 0

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_MANIFEST_BYTES) {
                throw IOException("Update manifest is too large.")
            }
            output.write(buffer, 0, read)
        }

        return output.toString(Charsets.UTF_8.name())
    }

    companion object {
        const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/cybertran/Enquadra-plus-release/main/app-update.json"
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val MAX_MANIFEST_BYTES = 32 * 1024
    }
}

internal object AppUpdateRequestPolicy {

    fun freshUrl(base: URL, nonce: Long): URL {
        require(base.toExternalForm() == AppUpdateChecker.DEFAULT_MANIFEST_URL) {
            "Unexpected app update manifest endpoint."
        }
        require(nonce >= 0) { "Request nonce must be non-negative." }
        return URL("${AppUpdateChecker.DEFAULT_MANIFEST_URL}?check=$nonce")
    }
}

/**
 * A published version older than the installed build is inconclusive, not evidence
 * that the installed build is the newest. Only an equal version is up to date.
 */
internal fun classifyAppUpdate(
    release: AppReleaseManifest,
    installedVersionCode: Long,
): AppUpdateCheckResult {
    require(installedVersionCode > 0) { "Installed versionCode must be positive." }
    return when {
        release.versionCode > installedVersionCode -> AppUpdateCheckResult.UpdateAvailable(release)
        release.versionCode == installedVersionCode -> AppUpdateCheckResult.UpToDate(release)
        else -> AppUpdateCheckResult.NotConfirmed(release)
    }
}

internal object AppUpdateManifestCodec {

    private const val SCHEMA_VERSION = 1
    private const val RELEASE_PATH_PREFIX =
        "/cybertran/Enquadra-plus-release/releases/tag/"
    private const val DOWNLOAD_PATH_PREFIX =
        "/cybertran/Enquadra-plus-release/releases/download/"
    private const val APK_FILE_NAME = "Enquadra.apk"
    private const val OFFICIAL_CERTIFICATE_SHA256 =
        "36:37:7E:0A:E2:63:B8:ED:79:B2:A9:1F:C4:A4:82:69:34:BC:5B:9D:FB:D4:4E:B7:B9:3D:6F:01:7A:2B:F0:53"

    fun decode(raw: String): AppReleaseManifest {
        require(raw.toByteArray(Charsets.UTF_8).size <= 32 * 1024) {
            "Update manifest is too large."
        }

        val json = JSONObject(raw)
        val schemaVersion = json.getInt("schemaVersion")
        require(schemaVersion == SCHEMA_VERSION) {
            "Unsupported update manifest schema."
        }

        val versionCode = json.getLong("versionCode")
        require(versionCode > 0) {
            "Invalid versionCode."
        }

        val versionName = json.getString("versionName").trim()
        require(versionName.matches(Regex("[0-9A-Za-z][0-9A-Za-z.+-]{0,63}"))) {
            "Invalid versionName."
        }

        val releaseUrl = validateGithubUrl(
            raw = json.getString("releaseUrl"),
            expectedPath = "$RELEASE_PATH_PREFIX" + "v$versionName",
        )
        val apkUrl = validateGithubUrl(
            raw = json.getString("apkUrl"),
            expectedPath = "$DOWNLOAD_PATH_PREFIX" + "v$versionName/$APK_FILE_NAME",
        )

        val apkSha256 = json.getString("apkSha256").trim().uppercase()
        require(apkSha256.matches(Regex("[0-9A-F]{64}"))) {
            "Invalid APK SHA-256."
        }

        val certificateSha256 =
            normalizeCertificateFingerprint(json.getString("signingCertificateSha256"))
        require(certificateSha256 == normalizeCertificateFingerprint(OFFICIAL_CERTIFICATE_SHA256)) {
            "Unexpected signing certificate."
        }

        return AppReleaseManifest(
            schemaVersion = schemaVersion,
            versionCode = versionCode,
            versionName = versionName,
            releaseUrl = releaseUrl,
            apkUrl = apkUrl,
            apkSha256 = apkSha256,
            signingCertificateSha256 = certificateSha256,
        )
    }

    private fun validateGithubUrl(
        raw: String,
        expectedPath: String,
    ): String {
        val value = raw.trim()
        val uri = URI(value)
        require(uri.scheme == "https") {
            "Update URL must use HTTPS."
        }
        require(uri.host == "github.com") {
            "Update URL must point to GitHub."
        }
        require(uri.port == -1 && uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "Update URL contains unsupported components."
        }
        require(uri.path == expectedPath) {
            "Update URL does not match the official release path."
        }
        return value
    }

    private fun normalizeCertificateFingerprint(value: String): String {
        val compact = value
            .trim()
            .replace(":", "")
            .uppercase()

        require(compact.matches(Regex("[0-9A-F]{64}"))) {
            "Invalid certificate SHA-256."
        }

        return compact
            .chunked(2)
            .joinToString(":")
    }
}
