package com.cybertran.enquadraplus.domain.model

data class TrafficInfraction(
    val id: String,
    val code: String,
    val article: String,
    val shortDescription: String,
    val description: String,
    val contentStatus: InfractionContentStatus,
    val classification: InfractionClassification,
    val points: Int?,
    val offenderResponsibility: String?,
    val authority: String?,
    val verificationMethod: String?,
    val penalty: String?,
    val administrativeMeasure: String?,
    val canConfigureTrafficCrime: Boolean,
    val legalBasis: List<String>,
    val whenToCite: List<String>,
    val whenNotToCite: List<String>,
    val definitionsAndProcedures: List<String>,
    val aitOfficialObservations: List<String>,
    val sourceConflicts: List<String> = emptyList(),
    val relatedCodes: List<String>,
    val relatedInfractions: List<RelatedInfractionReference>,
    val sources: List<OfficialSourceReference>,
)

enum class InfractionContentStatus {
    VERIFIED,
    PARTIAL_VALIDATION,
    MANUAL_REVIEW_REQUIRED,
    SOURCE_CONFLICT,
}

enum class InfractionClassification {
    LIGHT,
    MEDIUM,
    SERIOUS,
    VERY_SERIOUS,
    OTHER,
}

data class OfficialSourceReference(
    val agency: String,
    val document: String,
    val section: String?,
    val officialUrl: String?,
    val effectiveFrom: String?,
    val lastCheckedAt: String,
)

enum class InfractionRelationType {
    ALTERNATIVE,
    CONCOMITANT,
    PREREQUISITE,
    RELATED,
}

data class RelatedInfractionReference(
    val code: String,
    val type: InfractionRelationType,
    val reason: String?,
    val evidence: List<String>,
    val sourceField: String?,
    val origin: String,
)
