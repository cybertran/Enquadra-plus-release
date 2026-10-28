package com.cybertran.enquadraplus.data.update

import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateRequestPolicyTest {

    @Test
    fun consecutiveChecksUseDifferentUrlsAtOfficialHost() {
        val base = URL(AppUpdateChecker.DEFAULT_MANIFEST_URL)

        val first = AppUpdateRequestPolicy.freshUrl(base, 1761670000001L)
        val second = AppUpdateRequestPolicy.freshUrl(base, 1761670000002L)

        assertEquals("https", first.protocol)
        assertEquals("raw.githubusercontent.com", first.host)
        assertEquals("/cybertran/Enquadra-plus-release/main/app-update.json", first.path)
        assertEquals("check=1761670000001", first.query)
        assertEquals("check=1761670000002", second.query)
        assertTrue(first.toExternalForm() != second.toExternalForm())
    }

    @Test
    fun rejectsUnexpectedManifestHostOrPath() {
        assertThrows(IllegalArgumentException::class.java) {
            AppUpdateRequestPolicy.freshUrl(URL("https://example.com/app-update.json"), 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AppUpdateRequestPolicy.freshUrl(
                URL("https://raw.githubusercontent.com/other/Enquadra-plus-release/main/app-update.json"),
                1,
            )
        }
    }

    @Test
    fun rejectsNegativeRequestNonce() {
        assertThrows(IllegalArgumentException::class.java) {
            AppUpdateRequestPolicy.freshUrl(URL(AppUpdateChecker.DEFAULT_MANIFEST_URL), -1)
        }
    }

    @Test
    fun newerOfficialVersionOffersUpdate() {
        assertTrue(classifyAppUpdate(manifest(16), 15) is AppUpdateCheckResult.UpdateAvailable)
    }

    @Test
    fun equalOfficialVersionConfirmsUpToDate() {
        assertTrue(classifyAppUpdate(manifest(15), 15) is AppUpdateCheckResult.UpToDate)
    }

    @Test
    fun olderOfficialVersionDoesNotClaimUpToDate() {
        assertTrue(classifyAppUpdate(manifest(14), 15) is AppUpdateCheckResult.NotConfirmed)
    }

    private fun manifest(code: Long) = AppReleaseManifest(
        schemaVersion = 1,
        versionCode = code,
        versionName = "0.2.0-alpha.15",
        releaseUrl = "https://github.com/cybertran/Enquadra-plus-release/releases/tag/v0.2.0-alpha.15",
        apkUrl = "https://github.com/cybertran/Enquadra-plus-release/releases/download/v0.2.0-alpha.15/Enquadra.apk",
        apkSha256 = "0".repeat(64),
        signingCertificateSha256 = "36:37:7E:0A:E2:63:B8:ED:79:B2:A9:1F:C4:A4:82:69:34:BC:5B:9D:FB:D4:4E:B7:B9:3D:6F:01:7A:2B:F0:53",
    )
}
