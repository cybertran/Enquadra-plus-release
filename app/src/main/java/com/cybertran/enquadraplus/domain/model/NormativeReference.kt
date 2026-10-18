package com.cybertran.enquadraplus.domain.model

data class NormativeReference(
    val actType: String,
    val number: Int,
    val year: Int,
)

fun TrafficInfraction.explicitNormativeReferences(): List<NormativeReference> {
    val references = linkedSetOf(
        NormativeReference(
            actType = "RESOLUÇÃO",
            number = 985,
            year = 2022,
        ),
    )

    val text = buildList {
        add(article)
        add(description)
        penalty?.let(::add)
        administrativeMeasure?.let(::add)
        addAll(legalBasis)
        addAll(whenToCite)
        addAll(whenNotToCite)
        addAll(definitionsAndProcedures)
        addAll(aitOfficialObservations)
        sources.forEach { source ->
            add(source.document)
            source.section?.let(::add)
        }
    }.joinToString("\n")

    NORMATIVE_REFERENCE_REGEX.findAll(text).forEach { match ->
        val type = when (match.groupValues[1].lowercase()) {
            "resolução", "resolucao" -> "RESOLUÇÃO"
            "deliberação", "deliberacao" -> "DELIBERAÇÃO"
            else -> "PORTARIA"
        }
        val number = match.groupValues[2].toIntOrNull() ?: return@forEach
        val year = match.groupValues[3].toIntOrNull() ?: return@forEach
        references += NormativeReference(type, number, year)
    }

    return references.toList()
}

private val NORMATIVE_REFERENCE_REGEX = Regex(
    pattern = """(?i)\b(resolu[cç][aã]o|delibera[cç][aã]o|portaria)(?:\s+(?:CONTRAN|SENATRAN|DENATRAN))?\s*(?:n[ºo.]?\s*)?(\d{1,4})\s*(?:/|,?\s+de\s+(?:\d{1,2}\s+de\s+)?[\p{L}çÇãÃõÕéÉíÍóÓúÚâÂêÊ]+\s+de\s+)(\d{4})"""
)
