package com.cybertran.enquadraplus.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CtbUpdateCodecTest {

    @Test
    fun parserIgnoresRevokedStrikeTextAndKeepsAnnex() {
        val html = """
            <html><body>
              <h2>CAPÍTULO I</h2>
              <p>Art. 1º Texto vigente.</p>
              <p><s>Art. 2º Texto revogado.</s></p>
              <p>Art. 3º Outro texto vigente.</p>
              <div>ANEXO I</div>
              <div>DOS CONCEITOS E DEFINIÇÕES</div>
              <div>ACOSTAMENTO - parte da via.</div>
              <div>VIADUTO - obra de arte.</div>
            </body></html>
        """.trimIndent()

        val records = CtbHtmlParser.parse(html)

        assertEquals(listOf("art-1", "art-3", "anexo-i"), records.map { it.id })
        assertFalse(records.joinToString("\n") { it.text }.contains("Texto revogado"))
        assertTrue(records.last().text.contains("ACOSTAMENTO -"))
    }

    @Test
    fun transitionBlocksAnyUnexplainedRemoval() {
        val report = CtbUpdateCodec.validateDiff(
            UpdateDiff(
                activeCount = 391,
                candidateCount = 390,
                added = 0,
                changed = 1,
                removed = 1,
            ),
        )

        assertFalse(report.isValid)
        assertTrue(report.issues.any { it.code == "ctb.unexplained_removal" })
    }
}
