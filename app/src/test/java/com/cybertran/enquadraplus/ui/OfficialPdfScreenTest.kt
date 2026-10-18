package com.cybertran.enquadraplus.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OfficialPdfScreenTest {

    @Test
    fun suggestedFileNameRemovesUnsafeCharactersAndAddsExtension() {
        assertEquals(
            "Resolução CONTRAN 1031 2026.pdf",
            suggestedPdfFileName("Resolução CONTRAN 1031/2026"),
        )
    }

    @Test
    fun existingPdfExtensionIsPreserved() {
        assertEquals(
            "manual.pdf",
            suggestedPdfFileName("manual.pdf"),
        )
    }

    @Test
    fun blankTitleFallsBackToSafeName() {
        assertEquals(
            "documento-oficial.pdf",
            suggestedPdfFileName("///"),
        )
    }
}
