package com.cybertran.enquadraplus.data.feedback

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackContextProviderTest {
    @Test
    fun reusesExistingInstallationId() {
        val dir = Files.createTempDirectory("feedback-id").toFile()
        val file = File(dir, "id")
        val existing = "123e4567-e89b-12d3-a456-426614174000"
        file.writeText(existing)

        val result = loadOrCreateInstallationId(file) {
            error("Factory should not be called for a valid stored id")
        }

        assertEquals(existing, result)
    }

    @Test
    fun replacesMalformedInstallationIdAndPersistsReplacement() {
        val dir = Files.createTempDirectory("feedback-id").toFile()
        val file = File(dir, "id")
        file.writeText("not-a-valid-id")
        val replacement = "550e8400-e29b-41d4-a716-446655440000"

        val result = loadOrCreateInstallationId(file) { replacement }

        assertEquals(replacement, result)
        assertEquals(replacement, file.readText())
    }

    @Test
    fun buildsEncodedPrefilledUrl() {
        val url = buildPrefilledFeedbackUrl(
            baseUrl = "https://docs.google.com/forms/d/e/example/viewform",
            entryKey = "entry.123456",
            diagnosticPayload = "Dados técnicos do app — installation_id=abc; app_version=0.2.0-alpha.11; version_code=12",
        )

        assertTrue(url.startsWith("https://docs.google.com/forms/d/e/example/viewform?usp=pp_url&entry.123456="))
        assertTrue(url.contains("Dados+t%C3%A9cnicos+do+app+%E2%80%94+installation_id%3Dabc"))
        assertTrue(url.contains("app_version%3D0.2.0-alpha.11"))
        assertTrue(url.contains("version_code%3D12"))
        assertFalse(url.contains(" "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnexpectedEntryKey() {
        buildPrefilledFeedbackUrl(
            baseUrl = "https://example.com",
            entryKey = "bad-key",
            diagnosticPayload = "x",
        )
    }
}
