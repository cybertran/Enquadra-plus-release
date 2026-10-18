package com.cybertran.enquadraplus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AitObservationComposerTest {

    @Test
    fun composesOnlyUserSuppliedFacts() {
        val text = AitObservationComposer.compose(
            fact = "Veículo estacionado sobre a faixa.",
            location = "lado direito da via",
            circumstance = "",
            complement = "placa R-6a visível;",
        )

        assertEquals(
            "Veículo estacionado sobre a faixa. lado direito da via. placa R-6a visível.",
            text,
        )
    }

    @Test
    fun blankFieldsDoNotCreateText() {
        assertEquals(
            "",
            AitObservationComposer.compose("", "   ", "", ""),
        )
    }

    @Test
    fun whitespaceAndTrailingPunctuationAreNormalized() {
        assertEquals(
            "fato observado.",
            AitObservationComposer.compose(
                fact = "  fato   observado,,, ",
                location = "",
                circumstance = "",
                complement = "",
            ),
        )
    }
}
