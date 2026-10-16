package com.appanuman

import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.AttackChainId
import com.appanuman.data.model.CategoryResult
import com.appanuman.data.model.InstallerTrust
import com.appanuman.data.model.PermissionCategory
import com.appanuman.data.model.PermissionInfo
import com.appanuman.data.model.PermissionState
import com.appanuman.data.model.RiskBand
import com.appanuman.data.model.SpecialAccessResult
import com.appanuman.data.scoring.RiskScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskScoringEngineTest {

    private fun createBaseProfile(
        label: String = "Test App",
        packageName: String = "com.test.app",
        targetSdkVersion: Int = 34,
        installerTrust: InstallerTrust = InstallerTrust.VERIFIED,
        declaredPerms: List<PermissionInfo> = emptyList(),
        grantedPerms: List<PermissionInfo> = emptyList(),
        specialAccess: SpecialAccessResult = createDefaultSpecialAccess(),
        daysUnused: Int? = null,
        permissionCreepFlag: Boolean = false,
        addedPermissions: List<String> = emptyList()
    ): AppRiskProfile {
        val allCategories = PermissionCategory.values().associateWith { cat ->
            val perms = (declaredPerms + grantedPerms).filter { it.category == cat }.distinctBy { it.permissionName }
            val state = when {
                perms.any { it.state == PermissionState.GRANTED } -> PermissionState.GRANTED
                perms.isNotEmpty() -> PermissionState.DECLARED_ONLY
                else -> PermissionState.NOT_REQUESTED
            }
            CategoryResult(category = cat, state = state, permissions = perms)
        }

        return AppRiskProfile(
            label = label,
            packageName = packageName,
            versionName = "1.0",
            firstInstallTime = 100000L,
            lastUpdateTime = 100000L,
            installerPackageName = "com.android.vending",
            targetSdkVersion = targetSdkVersion,
            installerTrust = installerTrust,
            declaredPermissions = declaredPerms,
            grantedPermissions = grantedPerms,
            categoryMap = allCategories,
            specialAccess = specialAccess,
            riskScore = 0,
            riskBand = RiskBand.LOW,
            scoringFindings = emptyList(),
            triggeredChains = emptyList(),
            threatNarratives = emptyList(),
            contextMismatch = false,
            permissionCreepFlag = permissionCreepFlag,
            addedPermissions = addedPermissions,
            daysUnused = daysUnused,
            isDormant = false,
            isRemoteAccessApp = false,
            whyFlagged = emptyList(),
            icon = null
        )
    }

    private fun createDefaultSpecialAccess(
        accessibility: PermissionState = PermissionState.NOT_REQUESTED,
        deviceAdmin: PermissionState = PermissionState.NOT_REQUESTED,
        notificationListener: PermissionState = PermissionState.NOT_REQUESTED,
        overlay: PermissionState = PermissionState.NOT_REQUESTED,
        installPackages: PermissionState = PermissionState.NOT_REQUESTED
    ) = SpecialAccessResult(
        accessibilityEnabled = accessibility,
        deviceAdminActive = deviceAdmin,
        notificationListenerEnabled = notificationListener,
        overlayState = overlay,
        installPackagesState = installPackages,
        enabledAccessibilityServices = emptyList(),
        activeDeviceAdmins = emptyList()
    )

    @Test
    fun testOtpStealerChainTriggersCorrectly() {
        val granted = listOf(
            PermissionInfo("android.permission.RECEIVE_SMS", PermissionCategory.SMS, PermissionState.GRANTED, 1),
            PermissionInfo("android.permission.INTERNET", PermissionCategory.NETWORK, PermissionState.GRANTED, 0),
            PermissionInfo("android.permission.RECEIVE_BOOT_COMPLETED", PermissionCategory.BACKGROUND, PermissionState.GRANTED, 0)
        )
        val profile = createBaseProfile(grantedPerms = granted)

        val (score, findings) = RiskScoringEngine.score(profile)
        val chains = RiskScoringEngine.detectAttackChains(profile)

        val otpChain = chains.find { it.chainId == AttackChainId.OTP_STEALER }
        assertTrue("OTP_STEALER chain must match", otpChain?.matched == true)

        val chainFinding = findings.find { it.ruleId == "CHAIN_OTP_STEALER" }
        assertTrue("Scoring findings must include CHAIN_OTP_STEALER", chainFinding != null)
        assertEquals(15, chainFinding?.points)

        // Base: SMS(10) + Boot(5) + Internet(2) = 17, Chain: 15 => Total: 32
        assertEquals(32, score)
    }

    @Test
    fun testOverlayTrapChainTriggersCorrectly() {
        val specialAccess = createDefaultSpecialAccess(
            accessibility = PermissionState.GRANTED,
            overlay = PermissionState.GRANTED
        )
        val profile = createBaseProfile(specialAccess = specialAccess)

        val (score, findings) = RiskScoringEngine.score(profile)
        val chains = RiskScoringEngine.detectAttackChains(profile)

        val overlayChain = chains.find { it.chainId == AttackChainId.OVERLAY_TRAP }
        assertTrue("OVERLAY_TRAP chain must match", overlayChain?.matched == true)

        val chainFinding = findings.find { it.ruleId == "CHAIN_OVERLAY_TRAP" }
        assertTrue("Scoring findings must include CHAIN_OVERLAY_TRAP", chainFinding != null)
        assertEquals(15, chainFinding?.points)

        // Base: Accessibility(25) + Overlay(15) = 40, Chain: 15 => Total: 55
        assertEquals(55, score)
    }

    @Test
    fun testContextMismatchFiresForTorchWithSmsGranted() {
        val smsPerm = listOf(
            PermissionInfo("android.permission.READ_SMS", PermissionCategory.SMS, PermissionState.GRANTED, 1)
        )

        val torchMismatch = RiskScoringEngine.checkContextMismatch("Super Torch Flashlight", smsPerm)
        assertTrue("Torch app requesting SMS must trigger context mismatch", torchMismatch)

        val calculatorMismatch = RiskScoringEngine.checkContextMismatch("Quick Calculator Pro", smsPerm)
        assertTrue("Calculator requesting SMS must trigger context mismatch", calculatorMismatch)

        val normalMessengerMismatch = RiskScoringEngine.checkContextMismatch("Secure Messenger", smsPerm)
        assertFalse("Messenger requesting SMS should not trigger context mismatch", normalMessengerMismatch)
    }

    @Test
    fun testScoreIsDeterministic() {
        val granted = listOf(
            PermissionInfo("android.permission.CAMERA", PermissionCategory.CAMERA_MIC, PermissionState.GRANTED, 1),
            PermissionInfo("android.permission.ACCESS_FINE_LOCATION", PermissionCategory.LOCATION, PermissionState.GRANTED, 1),
            PermissionInfo("android.permission.INTERNET", PermissionCategory.NETWORK, PermissionState.GRANTED, 0)
        )
        val profile = createBaseProfile(grantedPerms = granted)

        val (firstScore, firstFindings) = RiskScoringEngine.score(profile)

        for (i in 1..10) {
            val (subsequentScore, subsequentFindings) = RiskScoringEngine.score(profile)
            assertEquals("Score must be deterministic on repeated calls", firstScore, subsequentScore)
            assertEquals("Findings list must have identical size", firstFindings.size, subsequentFindings.size)
            assertEquals("Findings points must match exactly", firstFindings.map { it.points }, subsequentFindings.map { it.points })
        }
    }

    @Test
    fun testScoreDoesNotExceed100() {
        // App with excessive permissions triggering all base weights and attack chains
        val granted = listOf(
            PermissionInfo("android.permission.RECEIVE_SMS", PermissionCategory.SMS, PermissionState.GRANTED, 1),
            PermissionInfo("android.permission.ACCESS_BACKGROUND_LOCATION", PermissionCategory.LOCATION, PermissionState.GRANTED, 1),
            PermissionInfo("android.permission.MANAGE_EXTERNAL_STORAGE", PermissionCategory.STORAGE, PermissionState.GRANTED, 1),
            PermissionInfo("android.permission.READ_CONTACTS", PermissionCategory.CONTACTS_CALLS, PermissionState.GRANTED, 1),
            PermissionInfo("android.permission.CAMERA", PermissionCategory.CAMERA_MIC, PermissionState.GRANTED, 1),
            PermissionInfo("android.permission.RECEIVE_BOOT_COMPLETED", PermissionCategory.BACKGROUND, PermissionState.GRANTED, 0),
            PermissionInfo("android.permission.INTERNET", PermissionCategory.NETWORK, PermissionState.GRANTED, 0)
        )
        val specialAccess = createDefaultSpecialAccess(
            accessibility = PermissionState.GRANTED,
            deviceAdmin = PermissionState.GRANTED,
            notificationListener = PermissionState.GRANTED,
            overlay = PermissionState.GRANTED
        )
        val profile = createBaseProfile(
            targetSdkVersion = 26,
            installerTrust = InstallerTrust.SIDELOADED,
            grantedPerms = granted,
            specialAccess = specialAccess,
            daysUnused = 60,
            permissionCreepFlag = true
        )

        val (score, findings) = RiskScoringEngine.score(profile)
        assertTrue("Score must not exceed 100", score <= 100)
        assertEquals("Score should be capped at 100", 100, score)
        assertEquals("Waterfall findings sum must match score", 100, findings.sumOf { it.points })
    }

    @Test
    fun testUnableToCheckNeverScoresAsGranted() {
        val specialAccess = createDefaultSpecialAccess(
            accessibility = PermissionState.UNABLE_TO_CHECK,
            deviceAdmin = PermissionState.UNABLE_TO_CHECK,
            notificationListener = PermissionState.UNABLE_TO_CHECK,
            overlay = PermissionState.UNABLE_TO_CHECK,
            installPackages = PermissionState.UNABLE_TO_CHECK
        )
        val profile = createBaseProfile(
            specialAccess = specialAccess,
            declaredPerms = emptyList(),
            grantedPerms = emptyList()
        )

        val (score, findings) = RiskScoringEngine.score(profile)
        assertEquals("UNABLE_TO_CHECK must never score as GRANTED", 0, score)
        assertTrue("No findings should be recorded for UNABLE_TO_CHECK", findings.isEmpty())
    }

    @Test
    fun testDormantDetectionFiresAt31DaysNot29Days() {
        val smsPerm = listOf(
            PermissionInfo("android.permission.READ_SMS", PermissionCategory.SMS, PermissionState.GRANTED, 1)
        )

        // Test at 29 days (should not trigger dormant chain)
        val profile29 = createBaseProfile(
            grantedPerms = smsPerm,
            daysUnused = 29
        )
        val chains29 = RiskScoringEngine.detectAttackChains(profile29)
        val dormantChain29 = chains29.find { it.chainId == AttackChainId.DORMANT_THREAT }
        assertFalse("Dormant threat chain must NOT match at 29 days", dormantChain29?.matched == true)

        val (_, findings29) = RiskScoringEngine.score(profile29)
        assertFalse("Dormant threat finding must NOT be present at 29 days",
            findings29.any { it.ruleId == "CHAIN_DORMANT_THREAT" })

        // Test at 31 days (should trigger dormant chain)
        val profile31 = createBaseProfile(
            grantedPerms = smsPerm,
            daysUnused = 31
        )
        val chains31 = RiskScoringEngine.detectAttackChains(profile31)
        val dormantChain31 = chains31.find { it.chainId == AttackChainId.DORMANT_THREAT }
        assertTrue("Dormant threat chain MUST match at 31 days", dormantChain31?.matched == true)

        val (_, findings31) = RiskScoringEngine.score(profile31)
        val dormantFinding31 = findings31.find { it.ruleId == "CHAIN_DORMANT_THREAT" }
        assertTrue("Dormant threat finding MUST be present at 31 days", dormantFinding31 != null)
        assertEquals(10, dormantFinding31?.points)
    }
}
