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

    @Test
    fun renderSizeRejectsPathologicalPageDimensions() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            calculatePdfRenderSize(
                pageWidth = 1,
                pageHeight = 100_000,
            )
        }
    }

    @Test
    fun renderSizePreservesNormalPageWithinPixelBudget() {
        assertEquals(
            1190 to 1684,
            calculatePdfRenderSize(
                pageWidth = 595,
                pageHeight = 842,
            ),
        )
    }
}
