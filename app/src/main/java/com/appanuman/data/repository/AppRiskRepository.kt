package com.appanuman.data.repository

import android.content.Context
import com.appanuman.data.collector.AppInventoryCollector
import com.appanuman.data.model.AppFilter
import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.AttackChainId
import com.appanuman.data.model.DeviceRiskSummary
import com.appanuman.data.model.InstallerTrust
import com.appanuman.data.model.PanicCheckResult
import com.appanuman.data.model.PanicVerdict
import com.appanuman.data.model.PermissionCategory
import com.appanuman.data.model.PermissionState
import com.appanuman.data.model.RiskBand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppRiskRepository(
    private val context: Context,
    private val inventoryCollector: AppInventoryCollector = AppInventoryCollector(context)
) {

    private val upiKeywords = listOf("bank", "pay", "upi", "money", "wallet", "finance")

    suspend fun scanApps(includeSystem: Boolean): Pair<List<AppRiskProfile>, DeviceRiskSummary> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val apps = inventoryCollector.collectAllApps(context, includeSystem)
            val duration = System.currentTimeMillis() - startTime

            val summary = computeDeviceSummary(apps, duration)
            Pair(apps, summary)
        }

    fun computeDeviceSummary(apps: List<AppRiskProfile>, durationMs: Long): DeviceRiskSummary {
        var criticalCount = 0
        var highCount = 0
        var mediumCount = 0
        var lowCount = 0
        var dormantCount = 0
        var sideloadedCount = 0

        for (app in apps) {
            when (app.riskBand) {
                RiskBand.CRITICAL -> criticalCount++
                RiskBand.HIGH -> highCount++
                RiskBand.MEDIUM -> mediumCount++
                RiskBand.LOW -> lowCount++
            }
            if (app.isDormant) dormantCount++
            if (app.installerTrust == InstallerTrust.SIDELOADED) sideloadedCount++
        }

        val panic = evaluatePanic(apps)
        val panicTriggerCount = if (panic.overallVerdict == PanicVerdict.DANGER) 2
        else if (panic.overallVerdict == PanicVerdict.WARNING) 1 else 0

        val rawScore = (criticalCount * 20) + (highCount * 10) + (mediumCount * 3) +
                (dormantCount * 5) + (panicTriggerCount * 25)
        val deviceScore = minOf(100, rawScore)

        val grade = when {
            deviceScore <= 20 -> "A"
            deviceScore <= 40 -> "B"
            deviceScore <= 60 -> "C"
            deviceScore <= 80 -> "D"
            else -> "F"
        }

        return DeviceRiskSummary(
            deviceScore = deviceScore,
            grade = grade,
            criticalCount = criticalCount,
            highCount = highCount,
            mediumCount = mediumCount,
            lowCount = lowCount,
            dormantCount = dormantCount,
            sideloadedCount = sideloadedCount,
            scanDurationMs = durationMs,
            scannedAppCount = apps.size,
            lastScanTime = System.currentTimeMillis()
        )
    }

    fun evaluatePanic(apps: List<AppRiskProfile>): PanicCheckResult {
        val now = System.currentTimeMillis()
        val remoteAccessApps = mutableListOf<String>()
        val recentSpecialAccessApps = mutableListOf<String>()
        val recentSideloads = mutableListOf<String>()
        var activeOverlayRisk = false
        val upiRiskApps = mutableListOf<String>()

        for (app in apps) {
            // 1. Remote access apps
            if (app.isRemoteAccessApp || AppInventoryCollector.REMOTE_ACCESS_PACKAGES.contains(app.packageName)) {
                remoteAccessApps.add(app.label)
            }

            // 2. Accessibility or overlay on app installed in last 48h
            val installedWithin48h = (now - app.firstInstallTime) < (48L * 60 * 60 * 1000)
            val hasAccessibility = app.specialAccess.accessibilityEnabled == PermissionState.GRANTED
            val hasOverlay = app.specialAccess.overlayState == PermissionState.GRANTED ||
                    app.specialAccess.overlayState == PermissionState.DECLARED_ONLY
            if (installedWithin48h && (hasAccessibility || hasOverlay)) {
                recentSpecialAccessApps.add(app.label)
            }

            // 3. Sideloaded in last 24h
            val installedWithin24h = (now - app.firstInstallTime) < (24L * 60 * 60 * 1000)
            if (app.installerTrust == InstallerTrust.SIDELOADED && installedWithin24h) {
                recentSideloads.add(app.label)
            }

            // 4. Active screen control risk
            val hasScreenTrap = app.triggeredChains.any {
                it.matched && (it.chainId == AttackChainId.OVERLAY_TRAP || it.chainId == AttackChainId.SILENT_SPY)
            }
            if (hasScreenTrap) {
                activeOverlayRisk = true
            }

            // 5. UPI fraud pattern: SMS + notification listener + finance/bank/pay/upi keyword
            val labelLower = app.label.lowercase()
            val hasUpiKeyword = upiKeywords.any { labelLower.contains(it) }
            val hasSms = app.categoryMap[PermissionCategory.SMS]?.state == PermissionState.GRANTED
            val hasNotif = app.specialAccess.notificationListenerEnabled == PermissionState.GRANTED
            if (hasUpiKeyword && hasSms && hasNotif) {
                upiRiskApps.add(app.label)
            }
        }

        val issuesCount = remoteAccessApps.size + recentSpecialAccessApps.size +
                recentSideloads.size + (if (activeOverlayRisk) 1 else 0) + upiRiskApps.size

        val verdict = when {
            issuesCount >= 2 || remoteAccessApps.isNotEmpty() || activeOverlayRisk -> PanicVerdict.DANGER
            issuesCount == 1 -> PanicVerdict.WARNING
            else -> PanicVerdict.SAFE
        }

        return PanicCheckResult(
            remoteAccessAppsFound = remoteAccessApps,
            recentSpecialAccessApps = recentSpecialAccessApps,
            recentSideloads = recentSideloads,
            activeOverlayRisk = activeOverlayRisk,
            upiRiskApps = upiRiskApps,
            overallVerdict = verdict
        )
    }

    fun filterApps(apps: List<AppRiskProfile>, filter: AppFilter): List<AppRiskProfile> {
        return when (filter) {
            AppFilter.ALL -> apps
            AppFilter.CRITICAL -> apps.filter { it.riskBand == RiskBand.CRITICAL }
            AppFilter.HIGH -> apps.filter { it.riskBand == RiskBand.HIGH }
            AppFilter.DORMANT -> apps.filter { it.isDormant }
            AppFilter.SIDELOADED -> apps.filter { it.installerTrust == InstallerTrust.SIDELOADED }
            AppFilter.MISMATCH -> apps.filter { it.contextMismatch }
        }
    }

    fun buildShareReport(
        summary: DeviceRiskSummary?,
        apps: List<AppRiskProfile>,
        isHindi: Boolean
    ): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date(summary?.lastScanTime ?: System.currentTimeMillis()))
        val grade = summary?.grade ?: "A"
        val score = summary?.deviceScore ?: 0

        val flaggedApps = apps.filter { it.riskBand == RiskBand.CRITICAL || it.riskBand == RiskBand.HIGH }
            .sortedByDescending { it.riskScore }
        val topApp = flaggedApps.firstOrNull()

        return if (isHindi) {
            val appBullets = flaggedApps.take(3).joinToString("\n") { app ->
                val reason = app.whyFlagged.firstOrNull() ?: "उच्च सुरक्षा जोखिम"
                "• ${app.label}: $reason"
            }
            """
            ऐप-अनुमान सुरक्षा रिपोर्ट — $dateStr
            फ़ोन सुरक्षा स्तर: $grade ($score/100)
            ${flaggedApps.size} ऐप्स पर ध्यान देने की आवश्यकता है:
            $appBullets
            
            मुख्य चिंता: ${topApp?.label ?: "कोई नहीं"}
            ${topApp?.whyFlagged?.take(2)?.joinToString(" ") ?: "सभी ऐप्स सुरक्षित सीमाओं में काम कर रहे हैं।"}
            सुझाया गया कदम: ऐप सेटिंग्स खोलें या संदिग्ध ऐप हटाएं।
            
            सभी जांच केवल आपके फ़ोन पर ऑफ़लाइन हुई हैं। कोई डेटा शेयर नहीं हुआ।
            App-Anuman: https://github.com/Trusha85/app_anuman
            """.trimIndent()
        } else {
            val appBullets = flaggedApps.take(3).joinToString("\n") { app ->
                val reason = app.whyFlagged.firstOrNull() ?: "Elevated security risk detected"
                "• ${app.label}: $reason"
            }
            """
            App-Anuman Security Report — $dateStr
            Your phone risk level: $grade ($score/100)
            ${flaggedApps.size} apps need attention:
            $appBullets
            
            Top concern: ${topApp?.label ?: "None"}
            ${topApp?.whyFlagged?.take(2)?.joinToString(" ") ?: "All scanned applications operate within safe device boundaries."}
            Suggested action: Open App Settings or uninstall unfamiliar apps.
            
            All checks done entirely on your device. Nothing was shared online.
            App-Anuman: https://github.com/Trusha85/app_anuman
            """.trimIndent()
        }
    }
}
