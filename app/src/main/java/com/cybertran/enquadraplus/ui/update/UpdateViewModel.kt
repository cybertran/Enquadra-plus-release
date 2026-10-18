package com.cybertran.enquadraplus.ui.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cybertran.enquadraplus.EnquadraApplication
import com.cybertran.enquadraplus.data.update.DatasetUpdateState
import com.cybertran.enquadraplus.data.update.UpdateDataset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository =
        (application as EnquadraApplication).container.safeUpdateRepository

    val states: StateFlow<List<DatasetUpdateState>> = repository.states
    val actionError = MutableStateFlow<String?>(null)

    fun checkNow() {
        actionError.value = null
        viewModelScope.launch {
            runCatching {
                repository.checkForUpdates()
            }.onFailure { error ->
                actionError.value = error.message ?: "Falha ao verificar atualizações."
            }
        }
    }

    fun apply(dataset: UpdateDataset) {
        actionError.value = null
        viewModelScope.launch {
            runCatching {
                repository.applyCandidate(dataset)
            }.onFailure { error ->
                actionError.value = error.message ?: "Falha ao aplicar a atualização."
            }
        }
    }

    fun rollback(dataset: UpdateDataset) {
        actionError.value = null
        viewModelScope.launch {
            runCatching {
                repository.rollback(dataset)
            }.onFailure { error ->
                actionError.value = error.message ?: "Falha ao restaurar a versão anterior."
            }
        }
    }
}
