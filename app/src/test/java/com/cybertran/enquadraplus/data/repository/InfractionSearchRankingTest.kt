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

    @Test
    fun numericArticleQueryRanksExactArticleAheadOfOtherMatches() {
        val first = infraction("501-00", "Dirigir sem habilitação", "Art. 162, I")
        val second = infraction("535-50", "Reparo na rodovia", "Art. 179, I")
        val expected = infraction("537-10", "Veículo imobilizado sem combustível", "Art. 180")

        val ranked = listOf(first, second, expected).rankForInfractionSearch("180")

        assertEquals(listOf("537-10", "501-00", "535-50"), ranked.map { it.code })
    }

    @Test
    fun numericCodeQueryRanksExactCodeAheadOfMatchingArticle() {
        val articleMatch = infraction("200-01", "Outra infração", "Art. 53710")
        val codeMatch = infraction("537-10", "Veículo imobilizado sem combustível", "Art. 180")

        val ranked = listOf(articleMatch, codeMatch).rankForInfractionSearch("53710")

        assertEquals(listOf("537-10", "200-01"), ranked.map { it.code })
    }

    @Test
    fun hyphenatedCodeQueryRanksExactCodeFirst() {
        val other = infraction("537-11", "Outra infração", "Art. 180")
        val exact = infraction("537-10", "Veículo imobilizado", "Art. 180")

        val ranked = listOf(other, exact).rankForInfractionSearch("537-10")

        assertEquals(listOf("537-10", "537-11"), ranked.map { it.code })
    }

    @Test
    fun exactArticleDoesNotTreatSuffixAsSameArticle() {
        val suffix = infraction("200-01", "Artigo com sufixo", "Art. 180-A")
        val exact = infraction("537-10", "Veículo imobilizado", "Art. 180")

        val ranked = listOf(suffix, exact).rankForInfractionSearch("180")

        assertEquals(listOf("537-10", "200-01"), ranked.map { it.code })
    }

    @Test
    fun exactArticleWithRomanNumeralStillMatchesBaseArticle() {
        val other = infraction("100-01", "Outro artigo", "Art. 180-A")
        val exact = infraction("100-02", "Infração do mesmo artigo", "Art. 180, I")

        val ranked = listOf(other, exact).rankForInfractionSearch("Art. 180")

        assertEquals(listOf("100-02", "100-01"), ranked.map { it.code })
    }

    @Test
    fun partialNumericCodePrioritizesCodePrefix() {
        val other = infraction("100-01", "Não corresponde ao código", "Art. 700")
        val codeMatch = infraction("537-10", "Código parcial", "Art. 180")

        val ranked = listOf(other, codeMatch).rankForInfractionSearch("537")

        assertEquals(listOf("537-10", "100-01"), ranked.map { it.code })
    }

    @Test
    fun compactCodeIsTokenizedLikeHyphenatedCode() {
        assertEquals("537* AND 10*", "53710".toInfractionFtsQuery())
        assertEquals("537* AND 1*", "5371".toInfractionFtsQuery())
        assertEquals("537* AND 10*", "537-10".toInfractionFtsQuery())
    }

    @Test
    fun textSearchWithNumberKeepsDescriptionFirstRanking() {
        val containing = infraction("100-01", "Deixar de transitar 180 metros", "Art. 180")
        val starting = infraction("100-02", "Transitar 180 metros em velocidade indevida", "Art. 100")

        val ranked = listOf(containing, starting).rankForInfractionSearch("transitar 180")

        assertEquals(listOf("100-02", "100-01"), ranked.map { it.code })
    }

    private fun infraction(
        code: String,
        title: String,
        article: String = "Art. 000",
    ): TrafficInfraction =
        TrafficInfraction(
            id = code,
            code = code,
            article = article,
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
