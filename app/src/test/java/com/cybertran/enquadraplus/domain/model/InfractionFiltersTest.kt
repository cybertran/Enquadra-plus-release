package com.cybertran.enquadraplus.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InfractionFiltersTest {

    @Test
    fun filtersByNaturePointsCrimeAuthorityAndVerification() {
        val item = infraction(
            classification = InfractionClassification.VERY_SERIOUS,
            points = 7,
            authority = "Órgão ou Entidade de Trânsito Municipal e Rodoviário.",
            verification = "Possível sem abordagem.",
            crime = true,
        )
        val filters = InfractionFilters(
            classifications = setOf(InfractionClassification.VERY_SERIOUS),
            points = setOf(7),
            crimeOnly = true,
            authorityQuery = "municipal",
            verificationQuery = "sem abordagem",
        )
        assertTrue(filters.matches(item))
    }

    @Test
    fun textFiltersIgnoreDiacriticsAndCase() {
        val item = infraction(
            authority = "Órgão de Trânsito Municipal",
            verification = "Mediante abordagem.",
        )
        assertTrue(
            InfractionFilters(
                authorityQuery = "ORGAO",
                verificationQuery = "abordagem",
            ).matches(item),
        )
    }

    @Test
    fun wrongPointsRejectsInfraction() {
        assertFalse(
            InfractionFilters(points = setOf(7)).matches(infraction(points = 5)),
        )
    }

    private fun infraction(
        classification: InfractionClassification = InfractionClassification.SERIOUS,
        points: Int? = 5,
        authority: String? = null,
        verification: String? = null,
        crime: Boolean = false,
    ): TrafficInfraction =
        TrafficInfraction(
            id = "000-00",
            code = "000-00",
            article = "Art. 000",
            shortDescription = "Teste",
            description = "Teste",
            contentStatus = InfractionContentStatus.VERIFIED,
            classification = classification,
            points = points,
            offenderResponsibility = null,
            authority = authority,
            verificationMethod = verification,
            penalty = "Multa",
            administrativeMeasure = null,
            canConfigureTrafficCrime = crime,
            legalBasis = emptyList(),
            whenToCite = emptyList(),
            whenNotToCite = emptyList(),
            definitionsAndProcedures = emptyList(),
            aitOfficialObservations = emptyList(),
            relatedCodes = emptyList(),
            relatedInfractions = emptyList(),
            sources = emptyList(),
        )
}
