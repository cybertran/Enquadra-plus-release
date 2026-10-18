package com.cybertran.enquadraplus.data.repository

import com.cybertran.enquadraplus.domain.model.TrafficInfraction
import java.text.Normalizer
import java.util.Locale

internal fun List<TrafficInfraction>.rankForInfractionSearch(
    query: String,
): List<TrafficInfraction> {
    val normalizedQuery = query.normalizeForSearchRanking()

    if (normalizedQuery.isBlank()) {
        return this
    }

    return sortedBy { infraction ->
        val title = infraction.shortDescription.normalizeForSearchRanking()
        when {
            title.startsWith(normalizedQuery) -> 0
            title.contains(normalizedQuery) -> 1
            else -> 2
        }
    }
}

private fun String.normalizeForSearchRanking(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
