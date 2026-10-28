package com.cybertran.enquadraplus.data.repository

import com.cybertran.enquadraplus.domain.model.TrafficInfraction
import java.text.Normalizer
import java.util.Locale

/**
 * Numeric searches prioritize official identifiers; textual searches keep the
 * established typification-first ranking.
 */
internal fun List<TrafficInfraction>.rankForInfractionSearch(
    query: String,
): List<TrafficInfraction> {
    val normalizedQuery = query.normalizeForSearchRanking()

    if (normalizedQuery.isBlank()) {
        return this
    }

    val numericQuery = query.trim().takeIf { NUMERIC_REFERENCE_QUERY.matches(it) }
    val requestedArticle = numericQuery
        ?.let { NUMERIC_ARTICLE_QUERY.matchEntire(it) }
        ?.let { match ->
            val suffix = match.groupValues[2].lowercase(Locale.ROOT)
            match.groupValues[1] + if (suffix.isEmpty()) "" else "-$suffix"
        }
    val requestedCodeDigits = numericQuery
        ?.filter(Char::isDigit)
        .orEmpty()

    return sortedBy { infraction ->
        val title = infraction.shortDescription.normalizeForSearchRanking()

        if (numericQuery == null) {
            when {
                title.startsWith(normalizedQuery) -> 0
                title.contains(normalizedQuery) -> 1
                else -> 2
            }
        } else {
            val codeDigits = infraction.code.filter(Char::isDigit)
            val article = infraction.article.articleIdentifier()
            when {
                requestedCodeDigits.length == 5 && codeDigits == requestedCodeDigits -> 0
                requestedArticle != null && article == requestedArticle -> 1
                requestedCodeDigits.isNotEmpty() && codeDigits.startsWith(requestedCodeDigits) -> 2
                requestedArticle != null && article?.startsWith(requestedArticle) == true -> 3
                title.startsWith(normalizedQuery) -> 4
                title.contains(normalizedQuery) -> 5
                else -> 6
            }
        }
    }
}

private val NUMERIC_REFERENCE_QUERY =
    Regex("""^(?:(?:art|artigo)\.?\s*)?\d+(?:\s*-\s*(?:\d{1,2}|[a-z]))?$""", RegexOption.IGNORE_CASE)

private val NUMERIC_ARTICLE_QUERY =
    Regex("""^(?:(?:art|artigo)\.?\s*)?(\d+)(?:\s*-\s*([a-z]))?$""", RegexOption.IGNORE_CASE)

private val ARTICLE_LABEL =
    Regex("""^\s*art(?:igo)?\.?\s*(\d+)(?:-([a-z]))?(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE)

private fun String.articleIdentifier(): String? =
    ARTICLE_LABEL.find(this)?.let { match ->
        val suffix = match.groupValues[2].lowercase(Locale.ROOT)
        match.groupValues[1] + if (suffix.isEmpty()) "" else "-$suffix"
    }

private fun String.normalizeForSearchRanking(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
