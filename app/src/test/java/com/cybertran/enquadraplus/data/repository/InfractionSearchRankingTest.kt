package com.cybertran.enquadraplus.data.repository

import com.cybertran.enquadraplus.domain.model.InfractionClassification
import com.cybertran.enquadraplus.domain.model.InfractionContentStatus
import com.cybertran.enquadraplus.domain.model.TrafficInfraction
import org.junit.Assert.assertEquals
import org.junit.Test

class InfractionSearchRankingTest {

    @Test
    fun titleStartingWithQueryComesBeforeTitleContainingQuery() {
        val containing = infraction(
            code = "100-02",
            title = "Deixar de reduzir a velocidade ao transitar por local sinalizado",
        )
        val starting = infraction(
            code = "100-01",
            title = "Transitar em velocidade superior à máxima permitida em até 20%",
        )

        val ranked = listOf(containing, starting).rankForInfractionSearch("transitar")

        assertEquals(listOf("100-01", "100-02"), ranked.map { it.code })
    }

    @Test
    fun rankingIgnoresCaseAndDiacritics() {
        val containing = infraction(
            code = "200-02",
            title = "Conduta relacionada à máxima velocidade ao transitar",
        )
        val starting = infraction(
            code = "200-01",
            title = "Transitar em velocidade superior à máxima permitida",
        )

        val ranked = listOf(containing, starting)
            .rankForInfractionSearch("TRANSITAR EM VELOCIDADE SUPERIOR A MAXIMA")

        assertEquals("200-01", ranked.first().code)
    }

    @Test
    fun incompleteSingleLetterSecondWordKeepsPreviousStableTerms() {
        assertEquals(
            "transitar*",
            "transitar e".toInfractionFtsQuery(),
        )
    }

    @Test
    fun connectorSecondWordDoesNotRestrictFtsQuery() {
        assertEquals(
            "transitar*",
            "transitar em".toInfractionFtsQuery(),
        )
    }

    @Test
    fun incompleteSingleLetterThirdWordKeepsCompletedPreviousWords() {
        assertEquals(
            "transitar*",
            "transitar em v".toInfractionFtsQuery(),
        )
    }

    @Test
    fun meaningfulWordAfterConnectorRefinesFtsQuery() {
        assertEquals(
            "transitar* AND ve*",
            "transitar em ve".toInfractionFtsQuery(),
        )
    }

    @Test
    fun queryMadeOnlyOfConnectorStillSearchesInsteadOfListingEverything() {
        assertEquals(
            "em*",
            "em".toInfractionFtsQuery(),
        )
    }

    @Test
    fun numericSingleCharacterTokenIsNotDropped() {
        assertEquals(
            "art* AND 1*",
            "art 1".toInfractionFtsQuery(),
        )
    }

    @Test
    fun equalRelevancePreservesExistingOrder() {
        val firstInDaoOrder = infraction(
            code = "300-02",
            title = "Transitar sobre calçada",
        )
        val secondInDaoOrder = infraction(
            code = "300-01",
            title = "Transitar pela contramão",
        )

        val ranked = listOf(firstInDaoOrder, secondInDaoOrder)
            .rankForInfractionSearch("transitar")

        assertEquals(listOf("300-02", "300-01"), ranked.map { it.code })
    }

    private fun infraction(
        code: String,
        title: String,
    ): TrafficInfraction =
        TrafficInfraction(
            id = code,
            code = code,
            article = "Art. 000",
            shortDescription = title,
            description = title,
            contentStatus = InfractionContentStatus.VERIFIED,
            classification = InfractionClassification.OTHER,
            points = null,
            offenderResponsibility = null,
            authority = null,
            verificationMethod = null,
            penalty = null,
            administrativeMeasure = null,
            canConfigureTrafficCrime = false,
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
