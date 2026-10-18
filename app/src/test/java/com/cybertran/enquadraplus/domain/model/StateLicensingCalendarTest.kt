package com.cybertran.enquadraplus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StateLicensingCalendarTest {

    @Test
    fun auditedCalendarsHaveUniqueUfsAndCompletePlateCoverage() {
        val calendars = StateLicensingCalendars.audited2026
        assertEquals(calendars.size, calendars.map { it.uf }.toSet().size)

        calendars.forEach { calendar ->
            val covered = calendar.entries.flatMap { it.plateEndings }.sorted()
            assertEquals((0..9).toList(), covered)
        }
    }

    @Test
    fun knownOfficialDeadlinesResolveByPlateEnding() {
        assertEquals(
            "Prazo até 31/10/2026",
            StateLicensingCalendars.find("MT")?.deadlineFor(0)?.display,
        )
        assertEquals(
            "Prazo até 31/07/2026",
            StateLicensingCalendars.find("RJ")?.deadlineFor(1)?.display,
        )
        assertEquals(
            "CRLV 2026 exigido a partir de 01/11/2026",
            StateLicensingCalendars.find("MG")?.deadlineFor(9)?.display,
        )
        assertNotNull(StateLicensingCalendars.find("al"))
    }
    // CI de integração final após baseline de segurança na main.
    // Calendários auditados só podem apontar para fontes oficiais via HTTPS.
    @Test
    fun auditedCalendarSourcesUseHttps() {
        StateLicensingCalendars.audited2026.forEach { calendar ->
            assertTrue(calendar.sourceUrl.startsWith("https://"))
        }
    }

}
