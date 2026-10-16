package com.appanuman.data.engine

import com.appanuman.data.model.ThreatNarrative

object ThreatNarrativeEngine {

    val OTP_STEALER = ThreatNarrative(
        chainId = "OTP_STEALER",
        title_en = "OTP Theft Risk",
        title_hi = "OTP चोरी का खतरा",
        body_en = "This app can read your incoming SMS including one-time passwords, send data over the internet, and restart itself after a reboot. This matches the capability pattern used by SMS-stealing malware. (Ref: MITRE T1412 — Capture SMS Messages)",
        body_hi = "यह ऐप आपके SMS पढ़ सकता है जिसमें OTP शामिल हैं, इंटरनेट पर डेटा भेज सकता है और रीबूट के बाद खुद चालू हो सकता है। यह SMS चोरी करने वाले मैलवेयर का तरीका है।",
        analogy_en = "Like giving a stranger a copy of your house key that works even after you change the lock.",
        analogy_hi = "जैसे किसी अजनबी को घर की चाभी देना जो ताला बदलने पर भी काम करे।",
        mitre_ref = "T1412",
        severity = "CRITICAL"
    )

    val OVERLAY_TRAP = ThreatNarrative(
        chainId = "OVERLAY_TRAP",
        title_en = "Fake Screen Overlay Risk",
        title_hi = "नकली स्क्रीन ओवरले का खतरा",
        body_en = "This app can draw a fake screen over your banking or UPI app and read everything you type including passwords and PINs. This matches the capability pattern used by overlay-based banking trojans. (Ref: MITRE T1417 — Input Capture)",
        body_hi = "यह ऐप आपके बैंकिंग या UPI ऐप पर नकली स्क्रीन दिखा सकता है और आपका पासवर्ड, PIN पढ़ सकता है। यह ओवरले बैंकिंग ट्रोजन का तरीका है।",
        analogy_en = "Like a thief placing a fake ATM panel over the real one to steal your PIN.",
        analogy_hi = "जैसे ATM पर नकली पैनल लगाकर PIN चुराना।",
        mitre_ref = "T1417",
        severity = "CRITICAL"
    )

    val SILENT_SPY = ThreatNarrative(
        chainId = "SILENT_SPY",
        title_en = "Silent Background Spy Risk",
        title_hi = "चुपचाप जासूसी का खतरा",
        body_en = "This app reads all your notifications including OTP alerts, can control other apps on your behalf, and sends data over the internet — with no visible activity on screen. (Ref: MITRE T1418)",
        body_hi = "यह ऐप आपकी सभी सूचनाएं पढ़ सकता है, दूसरे ऐप्स को नियंत्रित कर सकता है और डेटा भेज सकता है — बिना स्क्रीन पर कुछ दिखे।",
        analogy_en = "Like someone who can read all your post, open your doors, and report back — invisibly.",
        analogy_hi = "जैसे कोई आपकी सारी चिट्ठियां पढ़े, दरवाजे खोले और रिपोर्ट करे — बिना दिखे।",
        mitre_ref = "T1418",
        severity = "HIGH"
    )

    val PERSIST_ADMIN = ThreatNarrative(
        chainId = "PERSIST_ADMIN",
        title_en = "Hard to Remove App Risk",
        title_hi = "हटाने में मुश्किल ऐप का खतरा",
        body_en = "This app has device administrator rights and internet access, and was not installed from the Play Store. Device admins can block uninstallation and remotely control your device. (Ref: MITRE T1401)",
        body_hi = "यह ऐप डिवाइस एडमिन अधिकार और इंटरनेट रखता है और Play Store से नहीं आया। ऐसे ऐप्स को हटाना मुश्किल होता है।",
        analogy_en = "Like a guest who refuses to leave and has changed the locks from inside.",
        analogy_hi = "जैसे कोई मेहमान जो जाने से मना करे और अंदर से ताला बदल ले।",
        mitre_ref = "T1401",
        severity = "HIGH"
    )

    val DORMANT_THREAT = ThreatNarrative(
        chainId = "DORMANT_THREAT",
        title_en = "Sleeping Risk — Still Has Access",
        title_hi = "सोया हुआ खतरा — अभी भी एक्सेस है",
        body_en = "You have not used this app in over 30 days, but it still holds sensitive permissions. Unused apps with active access are a silent risk — they can still be triggered remotely or by an update.",
        body_hi = "आपने यह ऐप 30+ दिनों से उपयोग नहीं किया, लेकिन इसके पास अभी भी संवेदनशील अनुमतियां हैं। बेकार ऐप्स भी खतरनाक हो सकते हैं।",
        analogy_en = "Like leaving a spare key under the doormat for a service you no longer use.",
        analogy_hi = "जैसे उस सेवा की चाभी बाहर छोड़ना जो अब आप उपयोग नहीं करते।",
        mitre_ref = "T1444",
        severity = "MEDIUM"
    )

    fun getNarrative(chainId: String): ThreatNarrative? {
        return when (chainId) {
            "OTP_STEALER" -> OTP_STEALER
            "OVERLAY_TRAP" -> OVERLAY_TRAP
            "SILENT_SPY" -> SILENT_SPY
            "PERSIST_ADMIN" -> PERSIST_ADMIN
            "DORMANT_THREAT" -> DORMANT_THREAT
            else -> null
        }
    }
}
