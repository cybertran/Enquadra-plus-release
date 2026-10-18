package com.cybertran.enquadraplus

import android.app.Application
import android.util.Log
import com.cybertran.enquadraplus.data.AppContainer
import com.cybertran.enquadraplus.data.update.SafeUpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EnquadraApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        container = AppContainer(this)

        applicationScope.launch {
            runStartupBootstrap(
                importBundledData = container::importBundledDatasetIfNeeded,
                scheduleUpdates = {
                    SafeUpdateScheduler.schedule(this@EnquadraApplication)
                },
                onError = { phase, error ->
                    Log.e(
                        TAG,
                        "Startup phase failed without terminating the app: $phase",
                        error,
                    )
                },
            )
        }
    }

    private companion object {
        const val TAG = "EnquadraStartup"
    }
}
