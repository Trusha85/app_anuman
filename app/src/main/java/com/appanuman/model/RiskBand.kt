package com.appanuman.model

enum class RiskBand(val minScore: Int, val maxScore: Int) {
    LOW(0, 24),
    MEDIUM(25, 49),
    HIGH(50, 74),
    CRITICAL(75, 100);

    companion object {
        fun fromScore(score: Int): RiskBand {
            return when {
                score >= 75 -> CRITICAL
                score >= 50 -> HIGH
                score >= 25 -> MEDIUM
                else -> LOW
            }
        }
    }
}
