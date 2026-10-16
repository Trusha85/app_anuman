package com.appanuman.features.monitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.appanuman.BuildConfig
import com.appanuman.MainActivity
import com.appanuman.R
import com.appanuman.data.collector.AppInventoryCollector
import com.appanuman.data.model.InstallerTrust
import com.appanuman.data.model.RiskBand

class AppInstallReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "app_anuman_alerts"
        const val NOTIFICATION_ID_BASE = 2000
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action != Intent.ACTION_PACKAGE_ADDED && action != Intent.ACTION_PACKAGE_REPLACED) {
            return
        }

        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return // Ignore self updates

        val collector = AppInventoryCollector(context)
        val profile = collector.scanPackageByName(packageName) ?: return

        if (BuildConfig.DEBUG) {
            android.util.Log.d("AppInstallReceiver", "Scanned package $packageName, score=${profile.score}, band=${profile.riskBand}")
        }

        val isHighOrCritical = profile.riskBand == RiskBand.HIGH || profile.riskBand == RiskBand.CRITICAL
        val isSideloadedMedium = profile.installerTrust == InstallerTrust.SIDELOADED && profile.score >= 25

        if (isHighOrCritical || isSideloadedMedium) {
            createNotificationChannel(context)
            showRiskNotification(context, profile, isSideloadedMedium)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notif_channel_name)
            val desc = context.getString(R.string.notif_channel_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = desc
                // Silent for demo as specified
                setSound(null, null)
                enableVibration(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showRiskNotification(
        context: Context,
        profile: com.appanuman.data.model.AppRiskProfile,
        isSideloadedNotice: Boolean
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_PACKAGE_NAME", profile.packageName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            profile.packageName.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notif_flagged_title, profile.label)
        val body = if (isSideloadedNotice) {
            val topCategory = profile.grantedPermissions.firstOrNull()?.let {
                AppInventoryCollector.mapPermissionToCategory(it.permissionName).name
            } ?: "special"
            context.getString(R.string.notif_sideloaded_body, topCategory)
        } else {
            context.getString(R.string.notif_flagged_body, profile.riskBand.name)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + (profile.packageName.hashCode() % 1000), notification)
    }
}
