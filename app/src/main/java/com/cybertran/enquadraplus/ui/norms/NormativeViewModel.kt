package com.cybertran.enquadraplus.ui.norms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cybertran.enquadraplus.EnquadraApplication
import com.cybertran.enquadraplus.domain.model.NormativeAct
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NormativeViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository =
        (application as EnquadraApplication).container.normativeRepository

    val query = MutableStateFlow("")

    val results: StateFlow<List<NormativeAct>> =
        query
            .flatMapLatest(repository::search)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val selected = MutableStateFlow<NormativeAct?>(null)

    fun updateQuery(value: String) {
        query.value = value
    }

    fun open(id: String) {
        viewModelScope.launch {
            selected.value = repository.findById(id)
        }
    }

    fun closeDetail() {
        selected.value = null
    }
}
