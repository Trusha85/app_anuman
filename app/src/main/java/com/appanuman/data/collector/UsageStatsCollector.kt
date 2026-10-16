package com.appanuman.data.collector

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

class UsageStatsCollector(private val context: Context) {

    fun isUsageStatsPermissionGranted(): Boolean {
        return try {
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns a map of packageName -> lastTimeUsed (timestamp in ms).
     */
    fun queryLastTimeUsedMap(): Map<String, Long> {
        if (!isUsageStatsPermissionGranted()) {
            return emptyMap()
        }

        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyMap()

            val now = System.currentTimeMillis()
            // Look back over 90 days to capture apps unused for >30 days
            val startTime = now - (90L * 24 * 60 * 60 * 1000)
            val statsList = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_MONTHLY,
                startTime,
                now
            ) ?: emptyList()

            val resultMap = mutableMapOf<String, Long>()
            for (stats in statsList) {
                val existing = resultMap[stats.packageName] ?: 0L
                if (stats.lastTimeUsed > existing) {
                    resultMap[stats.packageName] = stats.lastTimeUsed
                }
            }
            resultMap
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Calculates days unused for an app.
     * Returns null if usage stats permission is not granted.
     */
    fun calculateDaysUnused(
        packageName: String,
        usageMap: Map<String, Long>,
        firstInstallTime: Long
    ): Int? {
        if (!isUsageStatsPermissionGranted()) {
            return null
        }

        val now = System.currentTimeMillis()
        val lastUsed = usageMap[packageName]

        val baselineTime = if (lastUsed != null && lastUsed > 0L) {
            lastUsed
        } else if (firstInstallTime > 0L) {
            firstInstallTime
        } else {
            return null
        }

        val diffMs = now - baselineTime
        if (diffMs < 0) return 0
        return (diffMs / (86400000L)).toInt()
    }
}
