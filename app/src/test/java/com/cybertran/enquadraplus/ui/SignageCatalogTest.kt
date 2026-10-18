package com.cybertran.enquadraplus.ui

import com.cybertran.enquadraplus.ui.signage.SignageCatalogParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignageCatalogTest {

    @Test
    fun parserAcceptsNineOfficialVolumesAndPageAddressableEntry() {
        val volumes = (1..9).joinToString(",") { index ->
            """
            {
              "volume": "$index",
              "title": "Volume $index",
              "officialUrl": "$OFFICIAL_PDF",
              "sourceSha256": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
              "pageCount": 10,
              "entryCount": 1
            }
            """.trimIndent()
        }
        val json = """
            {
              "version": "2026-10-15-signage-test",
              "volumeCount": 9,
              "entryCount": 1,
              "volumes": [$volumes],
              "records": [
                {
                  "id": "test-entry",
                  "volume": "I",
                  "volumeTitle": "Regulamentação",
                  "kind": "SIGN",
                  "code": "R-1",
                  "title": "Parada obrigatória",
                  "pageIndex": 3,
                  "manualPage": 43,
                  "evidence": "Índice oficial.",
                  "officialUrl": "$OFFICIAL_PDF"
                }
              ]
            }
        """.trimIndent()

        val catalog = SignageCatalogParser.parse(json)

        assertEquals(9, catalog.manuals.size)
        assertEquals(1, catalog.entries.size)
        assertEquals("R-1", catalog.entries.single().code)
        assertEquals(3, catalog.entries.single().pageIndex)
    }

    @Test
    fun officialPdfAllowlistAcceptsTransportPdf() {
        val uri = validateOfficialPdfUrl(OFFICIAL_PDF)
        assertEquals("www.gov.br", uri.host)
    }

    @Test(expected = IllegalArgumentException::class)
    fun pdfViewerRejectsUntrustedOrigin() {
        validateOfficialPdfUrl("https://example.com/manual.pdf")
    }

    @Test
    fun officialPdfRuleKeepsDirectPdfRequirement() {
        assertTrue(OFFICIAL_PDF.endsWith(".pdf"))
    }

    private companion object {
        const val OFFICIAL_PDF =
            "https://www.gov.br/transportes/pt-br/assuntos/transito/arquivos-senatran/docs/manual.pdf"
    }
}
