package com.appanuman.model

data class ThreatNarrative(
    val chainId: String,
    val title_en: String,
    val title_hi: String,
    val body_en: String,
    val body_hi: String,
    val analogy_en: String,
    val analogy_hi: String,
    val mitre_ref: String,
    val severity: RiskBand
)
