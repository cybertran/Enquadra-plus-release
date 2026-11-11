package com.cybertran.enquadraplus.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NormativeDiffDiagnosticsTest {

    @Test
    fun checkingDateAloneDoesNotChangeAnyAct() {
        val original = example(1)
        val active = payload(listOf(original))
        val candidate = payload(listOf(original.copy(lastCheckedAt = "2026-11-11")))

        assertEquals(0, NormativeUpdateCodec.diff(active, candidate).changed)
        assertTrue(NormativeUpdateCodec.changedFieldCounts(active, candidate).isEmpty())
    }

    @Test
    fun countsChangedSourceLinksWithoutCallingThemLegalTextChanges() {
        val original = example(1)
        val active = payload(listOf(original))
        val candidate = payload(listOf(original.copy(
            officialUrl = "https://www.gov.br/transportes/pt-br/assuntos/transito/other.pdf",
            lastCheckedAt = "2026-11-11",
        )))

        assertEquals(1, NormativeUpdateCodec.diff(active, candidate).changed)
        assertEquals(mapOf("officialUrl" to 1), NormativeUpdateCodec.changedFieldCounts(active, candidate))
    }

    @Test
    fun separatelyCountsSubjectStatusAndPublicationChanges() {
        val original = example(1)
        val active = payload(listOf(original))
        val candidate = payload(listOf(original.copy(
            subject = "Texto oficialmente modificado.",
            status = "Revogada",
            publicationDate = "02/11/2026",
        )))

        assertEquals(
            mapOf("publicationDate" to 1, "subject" to 1, "status" to 1),
            NormativeUpdateCodec.changedFieldCounts(active, candidate),
        )
    }

    @Test
    fun massChangeRemainsBlockedEvenWhenEveryChangeIsOnlyLinkMetadata() {
        val current = (1..250).map(::example)
        val active = payload(current)
        val candidate = payload(current.map { record ->
            record.copy(officialUrl = "${record.officialUrl}?version=2")
        })
        val diff = NormativeUpdateCodec.diff(active, candidate)
        val report = NormativeUpdateCodec.validateTransition(active, candidate, diff)

        assertEquals(250, diff.changed)
        assertEquals(mapOf("officialUrl" to 250), NormativeUpdateCodec.changedFieldCounts(active, candidate))
        assertTrue(report.issues.any { it.code == "norms.mass_change" })
    }

    private fun example(number: Int): NormativeUpdateRecord = NormativeUpdateRecord(
        id = "senatran-portaria-2025-$number",
        agency = "SENATRAN",
        actType = "PORTARIA",
        number = number,
        numberLabel = number.toString(),
        year = 2025,
        actDate = null,
        publicationDate = "08.01.2026",
        subject = "Esta Portaria integra ao Sistema Nacional de Trânsito um município.",
        status = null,
        officialUrl = "https://www.gov.br/transportes/pt-br/assuntos/transito/portaria$number.pdf",
        sourcePageUrl = "https://www.gov.br/transportes/pt-br/assuntos/transito/senatran/portarias-2025",
        lastCheckedAt = "2026-10-17",
    )

    private fun payload(records: List<NormativeUpdateRecord>): NormativeDatasetPayload =
        NormativeDatasetPayload(
            version = "test",
            generatedAt = "2026-10-17",
            verifiedAt = "2026-10-17",
            sourceDocument = "test",
            contentSha256 = "A".repeat(64),
            sources = emptyList(),
            records = records,
        )
}
