package com.cybertran.enquadraplus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NormativeReferenceTest {

    @Test
    fun alwaysIncludesMbftApprovalResolutionAndExtractsExplicitActs() {
        val item = infraction(
            legalBasis = listOf(
                "Resolução CONTRAN nº 993/2023",
                "Deliberação CONTRAN nº 279/2026",
            ),
        )

        val references = item.explicitNormativeReferences().toSet()

        assertTrue(NormativeReference("RESOLUÇÃO", 985, 2022) in references)
        assertTrue(NormativeReference("RESOLUÇÃO", 993, 2023) in references)
        assertTrue(NormativeReference("DELIBERAÇÃO", 279, 2026) in references)
    }

    @Test
    fun doesNotInferRegulationFromUnnumberedSubjectText() {
        val references = infraction(
            description = "Equipamentos obrigatórios e sinalização de trânsito.",
        ).explicitNormativeReferences()

        assertEquals(
            listOf(NormativeReference("RESOLUÇÃO", 985, 2022)),
            references,
        )
    }

    private fun infraction(
        description: String = "Teste",
        legalBasis: List<String> = emptyList(),
    ): TrafficInfraction =
        TrafficInfraction(
            id = "000-00",
            code = "000-00",
            article = "Art. 000",
            shortDescription = "Teste",
            description = description,
            contentStatus = InfractionContentStatus.VERIFIED,
            classification = InfractionClassification.SERIOUS,
            points = 5,
            offenderResponsibility = null,
            authority = null,
            verificationMethod = null,
            penalty = "Multa",
            administrativeMeasure = null,
            canConfigureTrafficCrime = false,
            legalBasis = legalBasis,
            whenToCite = emptyList(),
            whenNotToCite = emptyList(),
            definitionsAndProcedures = emptyList(),
            aitOfficialObservations = emptyList(),
            relatedCodes = emptyList(),
            relatedInfractions = emptyList(),
            sources = emptyList(),
        )
}
