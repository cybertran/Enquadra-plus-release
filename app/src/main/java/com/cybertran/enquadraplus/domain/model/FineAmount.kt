package com.cybertran.enquadraplus.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

data class FineAmountDisplay(
    val value: String,
    val detail: String? = null,
)

fun TrafficInfraction.fineAmountDisplay(): FineAmountDisplay? {
    val penaltyText = penalty?.trim().orEmpty()
    if (!penaltyText.contains("multa", ignoreCase = true)) return null

    val baseAmount = classification.baseFineAmount() ?: return null

    if (penaltyText.contains("acrescida a cada duzentos", ignoreCase = true)) {
        return FineAmountDisplay(
            value = formatBrl(baseAmount) + " + acréscimo variável",
            detail = "O valor final depende do excesso apurado, conforme a própria ficha do MBFT.",
        )
    }

    val multiplier = penaltyText.primaryFineMultiplier()
    val total = baseAmount.multiply(BigDecimal(multiplier))

    val recurrenceMultiplier = penaltyText.recurrenceFineMultiplier()
    val recurrenceDetail = recurrenceMultiplier
        ?.takeIf { it != multiplier }
        ?.let { recurrence ->
            "Em caso de reincidência prevista na ficha: " +
                formatBrl(baseAmount.multiply(BigDecimal(recurrence))) +
                " (" + recurrence + "x)."
        }

    return FineAmountDisplay(
        value = buildString {
            append(formatBrl(total))
            if (multiplier > 1) append(" (" + multiplier + "x)")
        },
        detail = recurrenceDetail,
    )
}

private fun InfractionClassification.baseFineAmount(): BigDecimal? =
    when (this) {
        InfractionClassification.LIGHT -> BigDecimal("88.38")
        InfractionClassification.MEDIUM -> BigDecimal("130.16")
        InfractionClassification.SERIOUS -> BigDecimal("195.23")
        InfractionClassification.VERY_SERIOUS -> BigDecimal("293.47")
        InfractionClassification.OTHER -> null
    }

private fun String.primaryFineMultiplier(): Int {
    val numeric = FINE_MULTIPLIER_REGEX.find(this)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (numeric != null) return numeric

    return if (contains("dez vezes", ignoreCase = true)) 10 else 1
}

private fun String.recurrenceFineMultiplier(): Int? {
    val recurrenceIndex = indexOf("reincid", ignoreCase = true)
    if (recurrenceIndex < 0) return null

    val recurrenceText = substring(recurrenceIndex)
    return FINE_MULTIPLIER_REGEX
        .find(recurrenceText)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
}

private fun formatBrl(amount: BigDecimal): String {
    val normalized = amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    val integer = normalized.substringBefore('.')
    val cents = normalized.substringAfter('.', "00")
    val grouped = integer
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()

    return "R$ " + grouped + "," + cents
}

private val FINE_MULTIPLIER_REGEX =
    Regex("""(?i)multa\s*\(\s*(\d+)\s*[xX]?""")
