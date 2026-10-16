package com.appanuman.model

enum class PanicVerdict {
    SAFE,
    CONCERN,
    WARNING
}

data class PanicCheckItem(
    val id: Int,
    val title_en: String,
    val title_hi: String,
    val description_en: String,
    val description_hi: String,
    val isTriggered: Boolean,
    val severity: RiskBand = RiskBand.LOW,
    val flaggedPackageName: String? = null,
    val flaggedAppName: String? = null
)

data class PanicScanResult(
    val verdict: PanicVerdict,
    val checks: List<PanicCheckItem> = emptyList(),
    val isScanning: Boolean = false,
    val completedSteps: Int = 5
)
