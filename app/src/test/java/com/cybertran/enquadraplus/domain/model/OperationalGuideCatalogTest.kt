package com.cybertran.enquadraplus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationalGuideCatalogTest {

    @Test
    fun guideIdsAreUniqueAndEveryGuideHasTraceableContent() {
        val guides = OperationalGuideCatalog.entries

        assertEquals(guides.size, guides.map { it.id }.toSet().size)
        assertTrue(guides.size >= 8)
        assertTrue(
            guides.all { guide ->
                guide.steps.isNotEmpty() &&
                    guide.legalReferences.isNotEmpty() &&
                    guide.normativeSearchTerms.isNotEmpty()
            },
        )
    }
}
