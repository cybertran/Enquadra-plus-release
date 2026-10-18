package com.cybertran.enquadraplus.domain.model

object AitObservationComposer {
    fun compose(
        fact: String,
        location: String,
        circumstance: String,
        complement: String,
    ): String =
        listOf(fact, location, circumstance, complement)
            .map(::normalizeFact)
            .filter(String::isNotBlank)
            .joinToString(". ")
            .let { text ->
                if (text.isBlank()) "" else "$text."
            }

    internal fun normalizeFact(value: String): String =
        value
            .trim()
            .replace(Regex("\\s+"), " ")
            .trimEnd('.', ';', ',')
}
