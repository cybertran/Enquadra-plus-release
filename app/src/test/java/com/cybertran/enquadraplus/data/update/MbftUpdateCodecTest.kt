package com.cybertran.enquadraplus.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MbftUpdateCodecTest {

    @Test
    fun kotlinRelationDerivationReproducesAuditedCurrentDatasetExactly() {
        val mbft = MbftUpdateCodec.parse(readAsset("mbft/infractions.json"))
        MbftUpdateCodec.validate(mbft).requireValid("fixture MBFT")

        val expected = InfractionRelationsUpdateCodec.parse(
            readAsset("relations/relations.json"),
        )
        val rebuilt = InfractionRelationsUpdateCodec.build(mbft)

        assertEquals(expected.recordCount, rebuilt.recordCount)
        assertEquals(expected.contentSha256, rebuilt.contentSha256)
        assertEquals(expected.version, rebuilt.version)
        assertEquals(expected.records.map { it.toMap() }, rebuilt.records.map { it.toMap() })
    }

    @Test
    fun mbftSourceAllowlistAcceptsOnlyAuditedArchivedFiches() {
        listOf(
            "https://www.gov.br/participamaisbrasil/blob/baixar/6308",
            "https://www.gov.br/participamaisbrasil/blob/baixar/6392",
            "https://www.gov.br/participamaisbrasil/blob/baixar/6456",
            "https://www.gov.br/participamaisbrasil/blob/baixar/6458",
        ).forEach { url ->
            assertTrue(isApprovedMbftDocumentUrl(url))
        }

        assertFalse(
            isApprovedMbftDocumentUrl(
                "https://www.gov.br/participamaisbrasil/blob/baixar/9999",
            ),
        )
        assertFalse(
            isApprovedMbftDocumentUrl(
                "https://www.gov.br/participamaisbrasil/blob/baixar/6392?download=1",
            ),
        )
        assertFalse(
            isApprovedMbftDocumentUrl(
                "https://www.gov.br.evil.example/participamaisbrasil/blob/baixar/6392",
            ),
        )
    }

    @Test
    fun resolution1031CannotApplyBeforeItsMbftEffectiveDate() {
        val active = MbftUpdateCodec.parse(readAsset("mbft/infractions.json"))
        val registry = MbftUpdateCodec.parseRegistry(
            readAsset(MbftUpdateCodec.REGISTRY_ASSET_PATH),
        )
        val patch = requireNotNull(MbftUpdateCodec.findPatch(registry, 1031, 2026))

        assertThrows(IllegalArgumentException::class.java) {
            MbftUpdateCodec.applyPatch(
                active = active,
                patch = patch,
                checkedAt = "2026-10-13",
                legalDate = "2026-10-13",
                resolutionFetch = fakeFetch(
                    patch.officialUrl,
                    patch.expectedSha256,
                ),
            )
        }
    }

    @Test
    fun resolution1031ProducesExpectedFourAuditedSheetsAndRemovesLegacyRefusalCode() {
        val active = MbftUpdateCodec.parse(readAsset("mbft/infractions.json"))
        val registry = MbftUpdateCodec.parseRegistry(
            readAsset(MbftUpdateCodec.REGISTRY_ASSET_PATH),
        )
        val patch = requireNotNull(MbftUpdateCodec.findPatch(registry, 1031, 2026))
        val candidate = MbftUpdateCodec.applyPatch(
            active = active,
            patch = patch,
            checkedAt = "2026-10-17",
            legalDate = "2026-10-17",
            resolutionFetch = fakeFetch(
                patch.officialUrl,
                patch.expectedSha256,
            ),
        )

        val byCode = candidate.records.associateBy(MbftUpdateRecord::code)
        val activeCodes = active.records.map(MbftUpdateRecord::code).toSet()
        val expectedCandidateCodes =
            (activeCodes - patch.removeCodes.toSet()) + patch.upserts.map(MbftUpdateRecord::code)
        assertEquals(expectedCandidateCodes, byCode.keys)
        assertEquals(expectedCandidateCodes.size, candidate.recordCount)
        assertFalse("757-90" in byCode)
        listOf("516-91", "516-92", "757-91", "757-92").forEach { code ->
            assertEquals("VERIFIED", byCode.getValue(code).status)
        }

        val diff = MbftUpdateCodec.diff(active, candidate)
        val upsertCodes = patch.upserts.map(MbftUpdateRecord::code).toSet()
        assertEquals(upsertCodes.count { it !in activeCodes }, diff.added)
        assertEquals(
            patch.removeCodes.count { it in activeCodes && it !in upsertCodes },
            diff.removed,
        )
        assertTrue(diff.changed <= patch.upserts.size)
        assertTrue(candidate.appliedAmendments.contains(patch.resolution))

        val relations = InfractionRelationsUpdateCodec.build(candidate)
        InfractionRelationsUpdateCodec.validate(relations, candidate)
            .requireValid("relações 1031")
        assertEquals(candidate.version, relations.mbftVersion)
        assertTrue(
            InfractionRelationsUpdateCodec.unresolvedReferenceCount(candidate) <=
                InfractionRelationsUpdateCodec.unresolvedReferenceCount(active),
        )
    }

    @Test
    fun resolution1031RejectsAnySourceHashDrift() {
        val active = MbftUpdateCodec.parse(readAsset("mbft/infractions.json"))
        val registry = MbftUpdateCodec.parseRegistry(
            readAsset(MbftUpdateCodec.REGISTRY_ASSET_PATH),
        )
        val patch = requireNotNull(MbftUpdateCodec.findPatch(registry, 1031, 2026))

        assertThrows(IllegalArgumentException::class.java) {
            MbftUpdateCodec.applyPatch(
                active = active,
                patch = patch,
                checkedAt = "2026-10-17",
                legalDate = "2026-10-17",
                resolutionFetch = fakeFetch(
                    patch.officialUrl,
                    "A".repeat(64),
                ),
            )
        }
    }

    @Test
    fun amendmentIdentityTreatsOfficialThousandsSeparatorAsSameAct() {
        assertEquals(1031 to 2026, MbftUpdateCodec.amendmentKey("Resolução CONTRAN nº 1031/2026"))
        assertEquals(1031 to 2026, MbftUpdateCodec.amendmentKey("Resolução CONTRAN nº 1.031/2026"))
        assertEquals(1003 to 2023, MbftUpdateCodec.amendmentKey("Resolução CONTRAN nº 1.003/2023"))
    }

    @Test
    fun registryRecognizesOnlyExplicitlyAuditedMbftAmendments() {
        val registry = MbftUpdateCodec.parseRegistry(
            readAsset(MbftUpdateCodec.REGISTRY_ASSET_PATH),
        )

        assertEquals(
            setOf(
                1003 to 2023,
                1009 to 2024,
                1012 to 2024,
                1013 to 2024,
                1022 to 2026,
                1023 to 2026,
                1031 to 2026,
            ),
            registry.recognizedAmendments,
        )
        assertEquals(setOf(1031 to 2026), registry.patches.map { it.number to it.year }.toSet())
    }

    private fun fakeFetch(url: String, hash: String): OfficialFetchResult =
        OfficialFetchResult(
            requestedUrl = url,
            finalUrl = url,
            contentType = "application/pdf",
            bytes = byteArrayOf(1),
            sha256 = hash,
        )

    private fun readAsset(relativePath: String): String {
        val candidates = listOf(
            File("src/main/assets", relativePath),
            File("app/src/main/assets", relativePath),
        )
        return candidates.firstOrNull(File::isFile)
            ?.readText(Charsets.UTF_8)
            ?: error("Asset de teste não encontrado: $relativePath")
    }
}
