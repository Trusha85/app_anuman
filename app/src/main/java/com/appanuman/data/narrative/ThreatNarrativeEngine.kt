package com.appanuman.data.narrative

import com.appanuman.data.model.AttackChainId
import com.appanuman.data.model.RiskBand
import com.appanuman.data.model.ThreatNarrative

object ThreatNarrativeEngine {

    val OTP_STEALER = ThreatNarrative(
        chainId = AttackChainId.OTP_STEALER,
        title_en = "OTP Theft Risk",
        title_hi = "OTP चोरी का खतरा",
        body_en = "This app can read your SMS including OTPs, send data over the internet, and restart after reboot. This matches the capability pattern of SMS-stealing malware. (MITRE T1412)",
        body_hi = "यह ऐप OTP सहित SMS पढ़ सकता है, इंटरनेट पर डेटा भेज सकता है और रीबूट के बाद चालू हो सकता है।",
        analogy_en = "Like giving a stranger a copy of your house key that works even after you change the lock.",
        analogy_hi = "जैसे किसी को घर की चाभी देना जो ताला बदलने पर भी काम करे।",
        mitre_ref = "T1412",
        severity = RiskBand.HIGH
    )

    val OVERLAY_TRAP = ThreatNarrative(
        chainId = AttackChainId.OVERLAY_TRAP,
        title_en = "Fake Screen Overlay Risk",
        title_hi = "नकली स्क्रीन ओवरले का खतरा",
        body_en = "This app can draw a fake screen over your banking or UPI app and read passwords and PINs you type. This matches the capability pattern of overlay banking trojans. (MITRE T1417)",
        body_hi = "यह ऐप UPI ऐप पर नकली स्क्रीन दिखाकर पासवर्ड और PIN चुरा सकता है।",
        analogy_en = "Like a thief placing a fake ATM panel over the real one to steal your PIN.",
        analogy_hi = "जैसे ATM पर नकली पैनल लगाकर PIN चुराना।",
        mitre_ref = "T1417",
        severity = RiskBand.CRITICAL
    )

    val SILENT_SPY = ThreatNarrative(
        chainId = AttackChainId.SILENT_SPY,
        title_en = "Silent Background Spy Risk",
        title_hi = "चुपचाप जासूसी का खतरा",
        body_en = "This app reads all notifications including OTPs, can control other apps, and sends data over the internet with no visible activity. (MITRE T1418)",
        body_hi = "यह ऐप सभी सूचनाएं पढ़ सकता है, दूसरे ऐप्स नियंत्रित कर सकता है और डेटा भेज सकता है।",
        analogy_en = "Like someone who reads all your post, opens your doors, and reports back — invisibly.",
        analogy_hi = "जैसे कोई सारी चिट्ठियां पढ़े और दरवाजे खोले — बिना दिखे।",
        mitre_ref = "T1418",
        severity = RiskBand.CRITICAL
    )

    val PERSIST_ADMIN = ThreatNarrative(
        chainId = AttackChainId.PERSIST_ADMIN,
        title_en = "Hard to Remove App Risk",
        title_hi = "हटाने में मुश्किल ऐप का खतरा",
        body_en = "This app has device administrator rights and internet access, and was not installed from Play Store. Device admins can block uninstallation. (MITRE T1401)",
        body_hi = "यह ऐप डिवाइस एडमिन और इंटरनेट रखता है और Play Store से नहीं आया। इसे हटाना मुश्किल है।",
        analogy_en = "Like a guest who refuses to leave and has changed the locks from inside.",
        analogy_hi = "जैसे मेहमान जो जाने से मना करे और अंदर से ताला बदल ले।",
        mitre_ref = "T1401",
        severity = RiskBand.HIGH
    )

    val DORMANT_THREAT = ThreatNarrative(
        chainId = AttackChainId.DORMANT_THREAT,
        title_en = "Sleeping Risk — Still Has Access",
        title_hi = "सोया हुआ खतरा — अभी भी एक्सेस है",
        body_en = "You have not used this app in over 30 days but it still holds sensitive permissions. Unused apps with active access remain a silent risk. (MITRE T1444)",
        body_hi = "आपने 30+ दिनों से यह ऐप उपयोग नहीं किया लेकिन इसके पास संवेदनशील अनुमतियां हैं।",
        analogy_en = "Like leaving a spare key under the doormat for a service you no longer use.",
        analogy_hi = "जैसे बेकार सेवा की चाभी दरवाजे के नीचे छोड़ना।",
        mitre_ref = "T1444",
        severity = RiskBand.MEDIUM
    )

    private val NARRATIVES_MAP = mapOf(
        AttackChainId.OTP_STEALER to OTP_STEALER,
        AttackChainId.OVERLAY_TRAP to OVERLAY_TRAP,
        AttackChainId.SILENT_SPY to SILENT_SPY,
        AttackChainId.PERSIST_ADMIN to PERSIST_ADMIN,
        AttackChainId.DORMANT_THREAT to DORMANT_THREAT
    )

    fun getNarrative(chainId: AttackChainId): ThreatNarrative? {
        return NARRATIVES_MAP[chainId]
    }

    fun getNarrativesForChains(chainIds: List<AttackChainId>): List<ThreatNarrative> {
        return chainIds.mapNotNull { NARRATIVES_MAP[it] }
    }
}
