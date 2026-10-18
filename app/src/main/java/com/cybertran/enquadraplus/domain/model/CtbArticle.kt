package com.cybertran.enquadraplus.domain.model

data class CtbArticle(
    val id: String,
    val label: String,
    val number: Int,
    val suffix: String?,
    val chapter: String?,
    val section: String?,
    val subsection: String?,
    val text: String,
    val sourceUrl: String,
    val lastCheckedAt: String,
)
