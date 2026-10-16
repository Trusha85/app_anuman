package com.appanuman.data.engine

import com.appanuman.data.model.InstallerTrust
import com.appanuman.data.model.RiskBand
import com.appanuman.data.model.SpecialAccessInfo
import com.appanuman.data.model.SpecialAccessState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskScoringEngineTest {

    @Test
    fun testDeterministicScoringRepeatedRuns() {
        val score1 = RiskScoringEngine.scoreApp(
            label = "SampleApp",
            packageName = "com.sample.app",
            targetSdkVersion = 34,
            installerTrust = InstallerTrust.VERIFIED,
            declaredPermissions = listOf("android.permission.INTERNET", "android.permission.RECEIVE_SMS"),
            grantedPermissions = listOf("android.permission.INTERNET", "android.permission.RECEIVE_SMS"),
            specialAccess = SpecialAccessInfo(),
            daysUnused = 5,
            permissionCreepFlag = false,
            addedPermissions = emptyList()
        )

        val score2 = RiskScoringEngine.scoreApp(
            label = "SampleApp",
            packageName = "com.sample.app",
            targetSdkVersion = 34,
            installerTrust = InstallerTrust.VERIFIED,
            declaredPermissions = listOf("android.permission.INTERNET", "android.permission.RECEIVE_SMS"),
            grantedPermissions = listOf("android.permission.INTERNET", "android.permission.RECEIVE_SMS"),
            specialAccess = SpecialAccessInfo(),
            daysUnused = 5,
            permissionCreepFlag = false,
            addedPermissions = emptyList()
        )

        assertEquals(score1.score, score2.score)
        assertEquals(score1.riskBand, score2.riskBand)
        assertEquals(score1.waterfall.size, score2.waterfall.size)
    }

    @Test
    fun testOtpStealerAttackChain() {
        val app = RiskScoringEngine.scoreApp(
            label = "BadSMSApp",
            packageName = "com.bad.sms",
            targetSdkVersion = 34,
            installerTrust = InstallerTrust.VERIFIED,
            declaredPermissions = listOf(
                "android.permission.RECEIVE_SMS",
                "android.permission.INTERNET",
                "android.permission.RECEIVE_BOOT_COMPLETED"
            ),
            grantedPermissions = listOf(
                "android.permission.RECEIVE_SMS",
                "android.permission.INTERNET",
                "android.permission.RECEIVE_BOOT_COMPLETED"
            ),
            specialAccess = SpecialAccessInfo(),
            daysUnused = 0,
            permissionCreepFlag = false,
            addedPermissions = emptyList()
        )

        // Base weights: SMS (10) + Boot (5) + Internet (2) = 17
        // Chain bonus: OTP_STEALER (+15)
        // Total = 32
        assertEquals(32, app.score)
        assertEquals(RiskBand.MEDIUM, app.riskBand)
        assertTrue(app.threatNarratives.any { it.chainIdStr == "OTP_STEALER" })
    }

    @Test
    fun testContextMismatchDetection() {
        val app = RiskScoringEngine.scoreApp(
            label = "Super Flashlight",
            packageName = "com.flashlight.app",
            targetSdkVersion = 34,
            installerTrust = InstallerTrust.VERIFIED,
            declaredPermissions = listOf("android.permission.READ_SMS"),
            grantedPermissions = listOf("android.permission.READ_SMS"),
            specialAccess = SpecialAccessInfo(),
            daysUnused = 0,
            permissionCreepFlag = false,
            addedPermissions = emptyList()
        )

        assertTrue(app.contextMismatch)
        assertEquals("HIGH", app.mismatchSeverity)
    }

    @Test
    fun testDeviceRiskGrade() {
        val (score, grade) = RiskScoringEngine.calculateDeviceRisk(emptyList(), panicTriggers = 0)
        assertEquals(0, score)
        assertEquals("A", grade)
    }
}
