package com.cybertran.enquadraplus.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OfficialSourcePolicyTest {

    @Test
    fun acceptsExactOfficialCtbSource() {
        val uri = OfficialSourcePolicy.validate(
            OfficialSourcePolicy.CTB_URL,
            OfficialSourceKind.CTB_HTML,
        )

        assertEquals("www.planalto.gov.br", uri.host)
    }

    @Test
    fun rejectsHttpAndLookalikeHosts() {
        assertThrows(IllegalArgumentException::class.java) {
            OfficialSourcePolicy.validate(
                "http://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm",
                OfficialSourceKind.CTB_HTML,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            OfficialSourcePolicy.validate(
                "https://www.gov.br.evil.example/transportes/pt-br/assuntos/transito/x",
                OfficialSourceKind.TRANSPORT_HTML,
            )
        }
    }

    @Test
    fun rejectsRedirectOutsideAllowlist() {
        assertThrows(IllegalArgumentException::class.java) {
            OfficialSourcePolicy.resolveRedirect(
                currentUrl = OfficialSourcePolicy.CONTRAN_INDEX_URL,
                location = "https://example.com/falsa-fonte",
                kind = OfficialSourceKind.TRANSPORT_HTML,
            )
        }
    }

    @Test
    fun mbftAcceptsOnlyAuditedOfficialContranPdf() {
        val resolution =
            "https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/resolucao10312026.pdf"

        assertEquals(
            "www.gov.br",
            OfficialSourcePolicy.validate(
                resolution,
                OfficialSourceKind.MBFT_PDF,
            ).host,
        )

        assertThrows(IllegalArgumentException::class.java) {
            OfficialSourcePolicy.validate(
                "https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/outra.pdf",
                OfficialSourceKind.MBFT_PDF,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            OfficialSourcePolicy.validate(
                "https://anexosportal.datalegis.net/arquivos/1932319.pdf",
                OfficialSourceKind.MBFT_PDF,
            )
        }
    }

    @Test
    fun sourceSetIncludesFixedContranIndexesAndGrowsByAnnualSenatranPage() {
        val sources2026 = OfficialSourcePolicy.normativeSources(2026)
        assertEquals(8, sources2026.size)
        assertEquals(9, OfficialSourcePolicy.normativeSources(2027).size)
        assertEquals(
            1,
            sources2026.count {
                it.url == OfficialSourcePolicy.CONTRAN_DELIBERATIONS_INDEX_URL
            },
        )
    }
}
