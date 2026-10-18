package com.cybertran.enquadraplus

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupBootstrapTest {

    @Test
    fun importFailureDoesNotEscapeAndSchedulingStillRuns() = runBlocking {
        val errors = mutableListOf<StartupPhase>()
        var scheduled = false

        runStartupBootstrap(
            importBundledData = {
                throw IllegalStateException("invalid bundled dataset")
            },
            scheduleUpdates = {
                scheduled = true
            },
            onError = { phase, _ ->
                errors += phase
            },
        )

        assertTrue(scheduled)
        assertEquals(listOf(StartupPhase.BUNDLED_DATA_IMPORT), errors)
    }

    @Test
    fun schedulingFailureDoesNotEscape() = runBlocking {
        val errors = mutableListOf<StartupPhase>()

        runStartupBootstrap(
            importBundledData = {},
            scheduleUpdates = {
                throw IllegalStateException("work manager unavailable")
            },
            onError = { phase, _ ->
                errors += phase
            },
        )

        assertEquals(listOf(StartupPhase.UPDATE_SCHEDULING), errors)
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsNotSwallowed() = runBlocking {
        runStartupBootstrap(
            importBundledData = {
                throw CancellationException("cancelled")
            },
            scheduleUpdates = {
                error("must not run")
            },
            onError = { _, _ ->
                error("cancellation must not be reported as startup failure")
            },
        )
    }
}
