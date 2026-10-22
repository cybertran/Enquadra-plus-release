package com.cybertran.enquadraplus.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackFormBindingTest {
    @Test
    fun feedbackFormBindingMatchesPublishedForm() {
        assertEquals(
            "https://docs.google.com/forms/d/e/1FAIpQLSeenUB00lPbYq6ZNpTIyhNB7gyvQukRtnal9_J6egMlAlPDUg/viewform",
            FEEDBACK_URL,
        )
        assertEquals("entry.1546345396", FEEDBACK_CONTEXT_ENTRY)
    }
}
