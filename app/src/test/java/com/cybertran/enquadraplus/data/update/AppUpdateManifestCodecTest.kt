package com.cybertran.enquadraplus.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppUpdateManifestCodecTest {

    @Test
    fun decodesOfficialReleaseManifest() {
        val manifest = AppUpdateManifestCodec.decode(validManifest())

        assertEquals(11L, manifest.versionCode)
        assertEquals("0.2.0-alpha.10", manifest.versionName)
        assertEquals(
            "8EC9880AA9DCEA7D3CFFE7A6435224B7751DDD971E1C97A8DEAABC99A2EF6148",
            manifest.apkSha256,
        )
    }

    @Test
    fun rejectsNonHttpsDownload() {
        val invalid = validManifest().replace(
            "https://github.com/cybertran/Enquadra-plus-release/releases/download/",
            "http://github.com/cybertran/Enquadra-plus-release/releases/download/",
        )

        assertThrows(IllegalArgumentException::class.java) {
            AppUpdateManifestCodec.decode(invalid)
        }
    }

    @Test
    fun rejectsDifferentRepository() {
        val invalid = validManifest().replace(
            "cybertran/Enquadra-plus-release",
            "someone/Enquadra-plus-release",
        )

        assertThrows(IllegalArgumentException::class.java) {
            AppUpdateManifestCodec.decode(invalid)
        }
    }

    @Test
    fun rejectsUnexpectedCertificate() {
        val invalid = validManifest().replace(
            "36:37:7E:0A:E2:63:B8:ED:79:B2:A9:1F:C4:A4:82:69:34:BC:5B:9D:FB:D4:4E:B7:B9:3D:6F:01:7A:2B:F0:53",
            "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00",
        )

        assertThrows(IllegalArgumentException::class.java) {
            AppUpdateManifestCodec.decode(invalid)
        }
    }

    @Test
    fun rejectsVersionPathMismatch() {
        val invalid = validManifest().replace(
            "releases/tag/v0.2.0-alpha.10",
            "releases/tag/v0.2.0-alpha.8",
        )

        assertThrows(IllegalArgumentException::class.java) {
            AppUpdateManifestCodec.decode(invalid)
        }
    }

    private fun validManifest(): String =
        """
        {
          "schemaVersion": 1,
          "versionCode": 11,
          "versionName": "0.2.0-alpha.10",
          "releaseUrl": "https://github.com/cybertran/Enquadra-plus-release/releases/tag/v0.2.0-alpha.10",
          "apkUrl": "https://github.com/cybertran/Enquadra-plus-release/releases/download/v0.2.0-alpha.10/Enquadra.apk",
          "apkSha256": "8EC9880AA9DCEA7D3CFFE7A6435224B7751DDD971E1C97A8DEAABC99A2EF6148",
          "signingCertificateSha256": "36:37:7E:0A:E2:63:B8:ED:79:B2:A9:1F:C4:A4:82:69:34:BC:5B:9D:FB:D4:4E:B7:B9:3D:6F:01:7A:2B:F0:53"
        }
        """.trimIndent()
}
