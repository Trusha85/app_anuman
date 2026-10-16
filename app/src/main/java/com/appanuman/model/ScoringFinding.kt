package com.appanuman.model

data class ScoringFinding(
    val ruleId: String,
    val label_en: String,
    val label_hi: String,
    val points: Int,
    val evidence: String
)
