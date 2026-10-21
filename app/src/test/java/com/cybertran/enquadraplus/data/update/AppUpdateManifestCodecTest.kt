package com.cybertran.enquadraplus.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppUpdateManifestCodecTest {

    @Test
    fun decodesOfficialReleaseManifest() {
        val manifest = AppUpdateManifestCodec.decode(validManifest())

        assertEquals(10L, manifest.versionCode)
        assertEquals("0.2.0-alpha.9", manifest.versionName)
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
            "50:D7:B4:DD:CA:B4:B9:37:62:C1:1A:3E:BB:1C:EF:44:4B:45:F3:EE:8F:F4:79:B7:3F:07:F2:51:15:C9:E1:18",
            "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00",
        )

        assertThrows(IllegalArgumentException::class.java) {
            AppUpdateManifestCodec.decode(invalid)
        }
    }

    @Test
    fun rejectsVersionPathMismatch() {
        val invalid = validManifest().replace(
            "releases/tag/v0.2.0-alpha.9",
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
          "versionCode": 10,
          "versionName": "0.2.0-alpha.9",
          "releaseUrl": "https://github.com/cybertran/Enquadra-plus-release/releases/tag/v0.2.0-alpha.9",
          "apkUrl": "https://github.com/cybertran/Enquadra-plus-release/releases/download/v0.2.0-alpha.9/Enquadra.apk",
          "apkSha256": "8EC9880AA9DCEA7D3CFFE7A6435224B7751DDD971E1C97A8DEAABC99A2EF6148",
          "signingCertificateSha256": "50:D7:B4:DD:CA:B4:B9:37:62:C1:1A:3E:BB:1C:EF:44:4B:45:F3:EE:8F:F4:79:B7:3F:07:F2:51:15:C9:E1:18"
        }
        """.trimIndent()
}
