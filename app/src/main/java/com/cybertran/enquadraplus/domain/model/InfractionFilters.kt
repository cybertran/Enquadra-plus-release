package com.cybertran.enquadraplus.domain.model

import java.text.Normalizer

enum class InfractionListMode {
    ALL,
    FAVORITES,
    RECENT,
}

data class InfractionFilters(
    val classifications: Set<InfractionClassification> = emptySet(),
    val points: Set<Int> = emptySet(),
    val crimeOnly: Boolean = false,
    val authorityQuery: String = "",
    val verificationQuery: String = "",
) {
    val activeCount: Int
        get() =
            classifications.size +
                points.size +
                if (crimeOnly) 1 else 0 +
                if (authorityQuery.isNotBlank()) 1 else 0 +
                if (verificationQuery.isNotBlank()) 1 else 0

    fun matches(infraction: TrafficInfraction): Boolean {
        if (classifications.isNotEmpty() && infraction.classification !in classifications) {
            return false
        }
        if (points.isNotEmpty() && infraction.points !in points) {
            return false
        }
        if (crimeOnly && !infraction.canConfigureTrafficCrime) {
            return false
        }
        if (
            authorityQuery.isNotBlank() &&
            !infraction.authority.orEmpty().containsNormalized(authorityQuery)
        ) {
            return false
        }
        if (
            verificationQuery.isNotBlank() &&
            !infraction.verificationMethod.orEmpty().containsNormalized(verificationQuery)
        ) {
            return false
        }
        return true
    }
}

data class InfractionHistoryEntry(
    val code: String,
    val viewedAt: Long,
)

private fun String.containsNormalized(query: String): Boolean =
    normalizeFilterText().contains(query.normalizeFilterText())

private fun String.normalizeFilterText(): String =
    Normalizer
        .normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .trim()
