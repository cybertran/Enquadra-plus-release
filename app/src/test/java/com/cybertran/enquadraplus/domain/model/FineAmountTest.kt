package com.cybertran.enquadraplus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FineAmountTest {

    @Test
    fun baseFineUsesCtbArticle258Value() {
        val display = infraction(InfractionClassification.SERIOUS, "Multa").fineAmountDisplay()
        assertEquals("R$ 195,23", display?.value)
    }

    @Test
    fun multiplierIsAppliedWhenMbftPenaltyStatesIt() {
        val display = infraction(InfractionClassification.VERY_SERIOUS, "Multa (5X)").fineAmountDisplay()
        assertEquals("R$ 1.467,35 (5x)", display?.value)
    }

    @Test
    fun auditedNumericMultiplierFormatsAreRecognized() {
        val cases = listOf(
            "Multa (3X)" to "R$ 880,41 (3x)",
            "Multa (3x)" to "R$ 880,41 (3x)",
            "Multa (10 vezes)" to "R$ 2.934,70 (10x)",
            "Multa (20X)" to "R$ 5.869,40 (20x)",
            "Multa (60X)" to "R$ 17.608,20 (60x)",
        )

        cases.forEach { (penalty, expected) ->
            val display = infraction(
                InfractionClassification.VERY_SERIOUS,
                penalty,
            ).fineAmountDisplay()
            assertEquals(expected, display?.value)
        }
    }

    @Test
    fun wordMultiplierIsRecognized() {
        val display = infraction(
            InfractionClassification.VERY_SERIOUS,
            "Multa (dez vezes), suspensão do direito de dirigir",
        ).fineAmountDisplay()
        assertEquals("R$ 2.934,70 (10x)", display?.value)
    }

    @Test
    fun recurrenceValueIsShownSeparately() {
        val display = infraction(
            InfractionClassification.VERY_SERIOUS,
            "Multa (5x); em caso de reincidência no período de 12 meses: multa (10x)",
        ).fineAmountDisplay()
        assertEquals("R$ 1.467,35 (5x)", display?.value)
        assertEquals(
            "Em caso de reincidência prevista na ficha: R$ 2.934,70 (10x).",
            display?.detail,
        )
    }

    @Test
    fun variableWeightFineDoesNotPretendToHaveFixedTotal() {
        val display = infraction(
            InfractionClassification.MEDIUM,
            "Multa acrescida a cada duzentos quilogramas ou fração de excesso de peso apurado.",
        ).fineAmountDisplay()
        assertEquals("R$ 130,16 + acréscimo variável", display?.value)
    }

    @Test
    fun nonFinePenaltyDoesNotInventMonetaryValue() {
        val display = infraction(
            InfractionClassification.SERIOUS,
            "Tipificação do Enquadramento: aplica-se o art. 209.",
        ).fineAmountDisplay()
        assertNull(display)
    }

    private fun infraction(
        classification: InfractionClassification,
        penalty: String,
    ): TrafficInfraction =
        TrafficInfraction(
            id = "000-00",
            code = "000-00",
            article = "Art. 000",
            shortDescription = "Teste",
            description = "Teste",
            contentStatus = InfractionContentStatus.VERIFIED,
            classification = classification,
            points = null,
            offenderResponsibility = null,
            authority = null,
            verificationMethod = null,
            penalty = penalty,
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
