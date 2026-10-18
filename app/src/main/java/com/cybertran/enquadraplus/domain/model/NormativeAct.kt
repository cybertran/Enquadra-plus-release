package com.cybertran.enquadraplus.domain.model

data class NormativeAct(
    val id: String,
    val agency: String,
    val actType: String,
    val number: Int,
    val numberLabel: String,
    val year: Int,
    val actDate: String?,
    val publicationDate: String?,
    val subject: String,
    val status: String?,
    val officialUrl: String,
    val sourcePageUrl: String,
    val lastCheckedAt: String,
    val fullText: String? = null,
) {
    val displayTitle: String
        get() =
            if (numberLabel.startsWith(actType, ignoreCase = true)) {
                "$numberLabel/$year"
            } else {
                "$actType $numberLabel/$year"
            }
}


data class InfractionNormativeLink(
    val act: NormativeAct,
    val relationKind: String,
    val evidence: String,
)
