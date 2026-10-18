package com.cybertran.enquadraplus

import kotlinx.coroutines.CancellationException

internal enum class StartupPhase {
    BUNDLED_DATA_IMPORT,
    UPDATE_SCHEDULING,
}

internal suspend fun runStartupBootstrap(
    importBundledData: suspend () -> Unit,
    scheduleUpdates: () -> Unit,
    onError: (StartupPhase, Exception) -> Unit,
) {
    try {
        importBundledData()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        onError(StartupPhase.BUNDLED_DATA_IMPORT, error)
    }

    try {
        scheduleUpdates()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        onError(StartupPhase.UPDATE_SCHEDULING, error)
    }
}
