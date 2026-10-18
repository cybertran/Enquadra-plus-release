package com.cybertran.enquadraplus.ui.infractions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cybertran.enquadraplus.EnquadraApplication
import com.cybertran.enquadraplus.domain.model.InfractionClassification
import com.cybertran.enquadraplus.domain.model.InfractionFilters
import com.cybertran.enquadraplus.domain.model.InfractionListMode
import com.cybertran.enquadraplus.domain.model.InfractionNormativeLink
import com.cybertran.enquadraplus.domain.model.explicitNormativeReferences
import com.cybertran.enquadraplus.domain.model.TrafficInfraction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class InfractionsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = application as EnquadraApplication
    private val repository = app.container.trafficRepository
    private val userRepository = app.container.userInfractionRepository
    private val normativeRepository = app.container.normativeRepository

    val query = MutableStateFlow("")
    val listMode = MutableStateFlow(InfractionListMode.ALL)
    val filters = MutableStateFlow(InfractionFilters())

    private val baseResults =
        query
            .debounce(SEARCH_DEBOUNCE_MS)
            .flatMapLatest(repository::search)

    val favoriteCodes =
        userRepository
            .observeFavoriteCodes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet(),
            )

    private val recentViews =
        userRepository
            .observeRecentViews(HISTORY_LIMIT)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private val listContext =
        combine(
            baseResults,
            favoriteCodes,
            recentViews,
        ) { base, favorites, history ->
            Triple(base, favorites, history)
        }

    val results: StateFlow<List<TrafficInfraction>> =
        combine(
            listContext,
            filters,
            listMode,
        ) { context, activeFilters, mode ->
            val (base, favorites, history) = context
            val filtered = base.filter(activeFilters::matches)

            when (mode) {
                InfractionListMode.ALL -> filtered
                InfractionListMode.FAVORITES ->
                    filtered.filter { it.code in favorites }
                InfractionListMode.RECENT -> {
                    val order = history
                        .mapIndexed { index, item -> item.code to index }
                        .toMap()
                    filtered
                        .filter { it.code in order }
                        .sortedBy { order[it.code] }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val selected = MutableStateFlow<TrafficInfraction?>(null)
    val annotationDraft = MutableStateFlow("")
    val annotationSaved = MutableStateFlow(true)
    val applicableNorms =
        MutableStateFlow<List<InfractionNormativeLink>>(emptyList())

    fun updateQuery(value: String) {
        query.value = value
    }

    fun setListMode(mode: InfractionListMode) {
        listMode.value = mode
    }

    fun toggleClassification(classification: InfractionClassification) {
        filters.value = filters.value.copy(
            classifications = filters.value.classifications
                .toMutableSet()
                .also { values ->
                    if (!values.add(classification)) values.remove(classification)
                },
        )
    }

    fun togglePoints(points: Int) {
        filters.value = filters.value.copy(
            points = filters.value.points
                .toMutableSet()
                .also { values ->
                    if (!values.add(points)) values.remove(points)
                },
        )
    }

    fun toggleCrimeOnly() {
        filters.value = filters.value.copy(
            crimeOnly = !filters.value.crimeOnly,
        )
    }

    fun updateAuthorityFilter(value: String) {
        filters.value = filters.value.copy(authorityQuery = value)
    }

    fun updateVerificationFilter(value: String) {
        filters.value = filters.value.copy(verificationQuery = value)
    }

    fun clearFilters() {
        filters.value = InfractionFilters()
    }

    fun open(code: String) {
        viewModelScope.launch {
            val infraction = repository.findByCode(code)
            selected.value = infraction
            if (infraction != null) {
                userRepository.recordViewed(code)
                annotationDraft.value = userRepository.getAnnotation(code).orEmpty()
                annotationSaved.value = true
                val auditedLinks =
                    normativeRepository.findForInfraction(infraction.code)
                applicableNorms.value =
                    if (auditedLinks.isNotEmpty()) {
                        auditedLinks
                    } else {
                        infraction
                            .explicitNormativeReferences()
                            .mapNotNull { reference ->
                                normativeRepository.findByReference(
                                    actType = reference.actType,
                                    number = reference.number,
                                    year = reference.year,
                                )?.let { act ->
                                    InfractionNormativeLink(
                                        act = act,
                                        relationKind = "EXPLICIT_MBFT_REFERENCE",
                                        evidence =
                                            "Referência normativa explícita localizada na ficha MBFT.",
                                    )
                                }
                            }
                            .distinctBy { it.act.id }
                    }
            } else {
                applicableNorms.value = emptyList()
            }
        }
    }

    fun closeDetail() {
        selected.value = null
        annotationDraft.value = ""
        annotationSaved.value = true
        applicableNorms.value = emptyList()
    }

    fun toggleFavorite(code: String) {
        viewModelScope.launch {
            userRepository.toggleFavorite(code)
        }
    }

    fun updateAnnotationDraft(value: String) {
        annotationDraft.value = value
        annotationSaved.value = false
    }

    fun saveAnnotation() {
        val code = selected.value?.code ?: return
        viewModelScope.launch {
            userRepository.saveAnnotation(code, annotationDraft.value)
            annotationDraft.value = annotationDraft.value.trim()
            annotationSaved.value = true
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
        const val HISTORY_LIMIT = 100
    }
}
