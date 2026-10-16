package com.appanuman.model

data class DeviceRiskSummary(
    val deviceScore: Int,
    val grade: String = calculateGrade(deviceScore),
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val dormantCount: Int = 0,
    val panicTriggers: Int = 0,
    val scannedAppsCount: Int = 0,
    val scanDurationSeconds: Double = 1.2,
    val lastScanTimeMillis: Long = System.currentTimeMillis(),
    val otpStealerCount: Int = 0,
    val backgroundSpyCount: Int = 0,
    val screenControlCount: Int = 0
) {
    companion object {
        fun calculateGrade(score: Int): String {
            return when {
                score <= 20 -> "A"
                score <= 40 -> "B"
                score <= 60 -> "C"
                score <= 80 -> "D"
                else -> "F"
            }
        }
    }
}
