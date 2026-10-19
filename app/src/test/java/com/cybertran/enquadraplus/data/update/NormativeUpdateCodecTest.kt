package com.cybertran.enquadraplus.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NormativeUpdateCodecTest {

    @Test
    fun parsesContranTableDeterministically() {
        val html = """
            <table>
              <tr>
                <th>Número</th><th>Data</th><th>Publicação</th><th>Assunto</th><th>Situação</th>
              </tr>
              <tr>
                <td><a href="/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/teste.pdf">Resolução 1031</a></td>
                <td>18/08/2026</td>
                <td>18/08/2026</td>
                <td>Dispõe sobre fiscalização.</td>
                <td>Em vigor</td>
              </tr>
            </table>
        """.trimIndent()

        val records = NormativeHtmlParser.parseContran(
            pageUrl = OfficialSourcePolicy.CONTRAN_INDEX_URL,
            sourceHtml = html,
            checkedAt = "2026-10-13",
        )

        assertEquals(1, records.size)
        assertEquals("contran-resolucao-2026-1031", records.single().id)
        assertEquals("CONTRAN", records.single().agency)
        assertTrue(records.single().officialUrl.startsWith("https://www.gov.br/transportes/"))
    }

    @Test
    fun parsesContranDeliberationsWithLongPublicationDate() {
        val html = """
            <table>
              <tr><th>Deliberação/Portaria</th><th>Publicação</th><th>Assunto</th><th>Observação</th></tr>
              <tr>
                <td><a href="/transportes/pt-br/assuntos/transito/conteudo-contran/deliberacoes/d279.pdf">Deliberação 279</a></td>
                <td>09 de Setembro de 2026</td>
                <td>Prorroga excepcionalmente a validade da CNH.</td>
                <td></td>
              </tr>
            </table>
        """.trimIndent()

        val record = NormativeHtmlParser.parseContranDeliberations(
            pageUrl = OfficialSourcePolicy.CONTRAN_DELIBERATIONS_INDEX_URL,
            sourceHtml = html,
            checkedAt = "2026-10-15",
        ).single()

        assertEquals("contran-deliberacao-2026-279", record.id)
        assertEquals("DELIBERAÇÃO", record.actType)
        assertEquals(2026, record.year)
    }

    @Test
    fun parsesSenatranTableWithPageYear() {
        val page = "https://www.gov.br/transportes/pt-br/assuntos/transito/senatran/portarias-2026"
        val html = """
            <table>
              <tr><td><a href="/transportes/pt-br/assuntos/transito/senatran/portaria-12.pdf">Portaria 12</a></td><td>10/01/2026</td><td>Assunto de teste.</td><td>Vigente</td></tr>
            </table>
        """.trimIndent()

        val record = NormativeHtmlParser.parseSenatran(
            pageUrl = page,
            sourceHtml = html,
            checkedAt = "2026-10-13",
            year = 2026,
        ).single()

        assertEquals("senatran-portaria-2026-12", record.id)
        assertEquals(2026, record.year)
    }

    @Test
    fun mismatchedSenatranPortariaLinkFallsBackToOfficialIndex() {
        val page = "https://www.gov.br/transportes/pt-br/assuntos/transito/senatran/portarias-2026"
        val html = """
            <table>
              <tr>
                <td><a href="/transportes/pt-br/assuntos/transito/arquivos-senatran/portarias/2026/portaria8582026.pdf">862</a></td>
                <td>16/10/2026</td>
                <td>Integra município ao Sistema Nacional de Trânsito.</td>
                <td>--</td>
              </tr>
            </table>
        """.trimIndent()

        val record = NormativeHtmlParser.parseSenatran(
            pageUrl = page,
            sourceHtml = html,
            checkedAt = "2026-10-17",
            year = 2026,
        ).single()

        assertEquals(862, record.number)
        assertEquals(page, record.officialUrl)
        assertEquals(page, record.sourcePageUrl)
    }

    @Test
    fun validationRejectsMismatchedSenatranPortariaDocument() {
        val record = NormativeUpdateRecord(
            id = "senatran-portaria-2026-862",
            agency = "SENATRAN",
            actType = "PORTARIA",
            number = 862,
            numberLabel = "862",
            year = 2026,
            actDate = null,
            publicationDate = "16/10/2026",
            subject = "Integra município ao Sistema Nacional de Trânsito.",
            status = null,
            officialUrl = "https://www.gov.br/transportes/pt-br/assuntos/transito/arquivos-senatran/portarias/2026/portaria8582026.pdf",
            sourcePageUrl = "https://www.gov.br/transportes/pt-br/assuntos/transito/senatran/portarias-2026",
            lastCheckedAt = "2026-10-17",
        )
        val payload = NormativeDatasetPayload(
            version = "invalid",
            generatedAt = "2026-10-17",
            verifiedAt = "2026-10-17",
            sourceDocument = "teste",
            contentSha256 = "A".repeat(64),
            sources = emptyList(),
            records = listOf(record),
        )

        val report = NormativeUpdateCodec.validate(payload)

        assertTrue(report.issues.any { it.code == "norms.portaria_url_mismatch" })
    }

    @Test
    fun transitionBlocksMissingKnownActsAndSources() {
        val source = NormativeUpdateSource(
            agency = "CONTRAN",
            actType = "RESOLUÇÃO",
            year = null,
            url = OfficialSourcePolicy.CONTRAN_INDEX_URL,
            sourceSha256 = "A".repeat(64),
            recordCount = 1,
        )
        val oldRecord = NormativeUpdateRecord(
            id = "contran-resolucao-2026-1031",
            agency = "CONTRAN",
            actType = "RESOLUÇÃO",
            number = 1031,
            numberLabel = "Resolução 1031",
            year = 2026,
            actDate = "18/08/2026",
            publicationDate = "18/08/2026",
            subject = "Teste",
            status = "Em vigor",
            officialUrl = OfficialSourcePolicy.CONTRAN_INDEX_URL,
            sourcePageUrl = OfficialSourcePolicy.CONTRAN_INDEX_URL,
            lastCheckedAt = "2026-10-13",
        )
        val active = NormativeDatasetPayload(
            version = "active",
            generatedAt = "2026-10-13",
            verifiedAt = "2026-10-13",
            sourceDocument = "teste",
            contentSha256 = "A".repeat(64),
            sources = listOf(source),
            records = listOf(oldRecord),
        )
        val candidate = active.copy(sources = emptyList(), records = emptyList())
        val diff = NormativeUpdateCodec.diff(active, candidate)
        val report = NormativeUpdateCodec.validateTransition(active, candidate, diff)

        assertFalse(report.isValid)
        assertEquals(1, diff.removed)
        assertTrue(report.issues.any { it.code == "norms.unexplained_removal" })
        assertTrue(report.issues.any { it.code == "norms.source_missing" })
    }
}
