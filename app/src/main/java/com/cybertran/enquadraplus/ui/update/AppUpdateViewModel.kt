package com.cybertran.enquadraplus.ui.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cybertran.enquadraplus.EnquadraApplication
import com.cybertran.enquadraplus.data.update.AppReleaseManifest
import com.cybertran.enquadraplus.data.update.AppUpdateCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppUpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    NOT_CONFIRMED,
    UPDATE_AVAILABLE,
    ERROR,
}

data class AppUpdateUiState(
    val status: AppUpdateStatus = AppUpdateStatus.IDLE,
    val release: AppReleaseManifest? = null,
)

class AppUpdateViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val checker =
        (application as EnquadraApplication).container.appUpdateChecker

    private val _state = MutableStateFlow(AppUpdateUiState())
    val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    fun checkNow(installedVersionCode: Long) {
        if (_state.value.status == AppUpdateStatus.CHECKING) return

        _state.value = AppUpdateUiState(status = AppUpdateStatus.CHECKING)
        viewModelScope.launch {
            runCatching {
                checker.check(installedVersionCode)
            }.onSuccess { result ->
                _state.value = when (result) {
                    is AppUpdateCheckResult.UpdateAvailable ->
                        AppUpdateUiState(
                            status = AppUpdateStatus.UPDATE_AVAILABLE,
                            release = result.release,
                        )
                    is AppUpdateCheckResult.UpToDate ->
                        AppUpdateUiState(
                            status = AppUpdateStatus.UP_TO_DATE,
                            release = result.release,
                        )
                    is AppUpdateCheckResult.NotConfirmed ->
                        AppUpdateUiState(
                            status = AppUpdateStatus.NOT_CONFIRMED,
                            release = result.release,
                        )
                }
            }.onFailure {
                _state.value = AppUpdateUiState(status = AppUpdateStatus.ERROR)
            }
        }
    }
}
