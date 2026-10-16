package com.appanuman.features.panic

import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.InstallerTrust
import com.appanuman.data.model.SpecialAccessInfo
import com.appanuman.data.model.SpecialAccessState
import com.appanuman.data.model.ThreatNarrative
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PanicCheckEngineTest {

    @Test
    fun testSafeVerdictWhenClean() {
        val cleanApps = listOf(
            AppRiskProfile(
                packageName = "com.example.calculator",
                label = "Clean Calculator",
                firstInstallTime = System.currentTimeMillis() - 100_000_000,
                installerTrust = InstallerTrust.VERIFIED
            )
        )

        val result = PanicCheckEngine.runCheck(cleanApps)
        assertEquals(PanicVerdict.SAFE, result.verdict)
        assertTrue(result.remoteAccessApps.isEmpty())
        assertTrue(result.recentSpecialAccessApps.isEmpty())
        assertTrue(result.recentSideloads.isEmpty())
    }

    @Test
    fun testRemoteAccessDetectionTriggersDanger() {
        val apps = listOf(
            AppRiskProfile(
                packageName = "com.anydesk.anydeskandroid",
                label = "AnyDesk Remote Control",
                firstInstallTime = System.currentTimeMillis() - 100_000_000,
                installerTrust = InstallerTrust.VERIFIED
            )
        )

        val result = PanicCheckEngine.runCheck(apps)
        assertEquals(PanicVerdict.DANGER, result.verdict)
        assertEquals(1, result.remoteAccessApps.size)
        assertEquals("AnyDesk Remote Control", result.remoteAccessApps[0])
    }

    @Test
    fun testUpiFraudPatternTriggersWarning() {
        val apps = listOf(
            AppRiskProfile(
                packageName = "com.fake.bankhelper",
                label = "Quick Bank Pay",
                firstInstallTime = System.currentTimeMillis() - 100_000_000,
                installerTrust = InstallerTrust.VERIFIED,
                grantedPermissions = listOf("android.permission.RECEIVE_SMS"),
                specialAccess = SpecialAccessInfo(
                    notificationListenerEnabled = SpecialAccessState.ENABLED
                )
            )
        )

        val result = PanicCheckEngine.runCheck(apps)
        assertEquals(PanicVerdict.WARNING, result.verdict)
        assertEquals(1, result.upiRiskApps.size)
        assertEquals("Quick Bank Pay", result.upiRiskApps[0])
    }

    @Test
    fun testActiveOverlayTrapTriggersDanger() {
        val apps = listOf(
            AppRiskProfile(
                packageName = "com.suspicious.overlay",
                label = "Flashlight Free",
                firstInstallTime = System.currentTimeMillis() - 100_000_000,
                threatNarratives = listOf(
                    ThreatNarrative(
                        chainId = "OVERLAY_TRAP",
                        title_en = "Overlay",
                        title_hi = "ओवरले",
                        body_en = "Trap",
                        body_hi = "ट्रैप",
                        analogy_en = "",
                        analogy_hi = "",
                        mitre_ref = "",
                        severity = "CRITICAL"
                    )
                )
            )
        )

        val result = PanicCheckEngine.runCheck(apps)
        assertEquals(PanicVerdict.DANGER, result.verdict)
        assertTrue(result.activeOverlayRisk)
    }

    @Test
    fun testRecentSideloadAndSpecialAccessTriggersDanger() {
        val apps = listOf(
            AppRiskProfile(
                packageName = "com.sideload.recent",
                label = "Modded Game",
                firstInstallTime = System.currentTimeMillis() - (10L * 60 * 60 * 1000), // 10h ago
                installerTrust = InstallerTrust.SIDELOADED,
                specialAccess = SpecialAccessInfo(
                    accessibilityEnabled = SpecialAccessState.ENABLED
                )
            )
        )

        val result = PanicCheckEngine.runCheck(apps)
        assertEquals(PanicVerdict.DANGER, result.verdict)
        assertTrue(result.recentSpecialAccessApps.isNotEmpty())
        assertTrue(result.recentSideloads.isNotEmpty())
    }
}
