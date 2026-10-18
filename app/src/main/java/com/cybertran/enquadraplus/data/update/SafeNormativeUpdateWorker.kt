package com.cybertran.enquadraplus.data.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cybertran.enquadraplus.EnquadraApplication
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class SafeNormativeUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val application = applicationContext as? EnquadraApplication
            ?: return Result.failure()

        return try {
            val repository = application.container.safeUpdateRepository
            repository.checkForUpdates()
            UpdateNotificationManager.notifyValidatedCandidates(
                context = applicationContext,
                states = repository.states.value,
            )
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.retry()
        }
    }
}

object SafeUpdateScheduler {

    private const val UNIQUE_WORK_NAME = "safe-normative-update-check"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request =
            PeriodicWorkRequestBuilder<SafeNormativeUpdateWorker>(
                24,
                TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .addTag(UNIQUE_WORK_NAME)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
