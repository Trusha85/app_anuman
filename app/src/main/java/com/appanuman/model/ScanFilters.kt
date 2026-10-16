package com.appanuman.model

enum class ScanState {
    IDLE,
    SCANNING,
    COMPLETED,
    ERROR
}

enum class AppFilter {
    ALL,
    CRITICAL,
    HIGH,
    DORMANT,
    SIDELOADED,
    MISMATCH,
    CREEP,
    QUESTION_OTP,
    QUESTION_BACKGROUND,
    QUESTION_SCREEN
}

enum class SortOrder {
    RISK_SCORE,
    NAME,
    INSTALL_DATE,
    DAYS_UNUSED
}
