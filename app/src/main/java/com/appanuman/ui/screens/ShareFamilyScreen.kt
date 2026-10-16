package com.appanuman.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.model.*
import com.appanuman.ui.components.AppTopBar
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShareFamilyScreen(
    deviceSummary: DeviceRiskSummary,
    apps: List<AppRiskProfile>,
    userMode: UserMode,
    language: Language,
    onBackClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val formattedDate = remember { dateFormatter.format(Date()) }

    // Generate plain-text family report
    val reportText = remember(deviceSummary, apps, language) {
        generateFamilyReport(deviceSummary, apps, formattedDate, language)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.share_screen_title),
                showBackButton = true,
                showModeSelector = false,
                currentLanguage = language,
                userMode = userMode,
                onBackClick = onBackClick,
                onLanguageToggle = onToggleLanguage
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, reportText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Security Report")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.share_button_label),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Privacy notice card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.share_privacy_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.share_card_header),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Plain-text preview card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = reportText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun generateFamilyReport(
    summary: DeviceRiskSummary,
    apps: List<AppRiskProfile>,
    dateStr: String,
    language: Language
): String {
    val flaggedApps = apps.filter { it.riskBand == RiskBand.HIGH || it.riskBand == RiskBand.CRITICAL }
    val topConcern = flaggedApps.firstOrNull()

    return if (language == Language.HI) {
        val topConcernDesc = topConcern?.let { app ->
            val plainReason = app.whyFlagged_hi.firstOrNull() ?: "असामान्य संवेदनशील अनुमतियां पाई गईं"
            """
शीर्ष चिंता: ${app.appName}
$plainReason
सुझाव: ऐप सेटिंग्स खोलें या तुरंत अनइंस्टॉल करें।
            """.trimIndent()
        } ?: "कोई गंभीर चिंता नहीं पाई गई।"

        val flaggedBullets = if (flaggedApps.isNotEmpty()) {
            flaggedApps.joinToString("\n") { "• ${it.appName}: ${it.whyFlagged_hi.firstOrNull() ?: "जांच आवश्यक"}" }
        } else {
            "• सभी ऐप्स सुरक्षित प्रतीत होते हैं।"
        }

        """
🛡️ ऐप-अनुमान सुरक्षा रिपोर्ट — $dateStr
फ़ोन जोखिम स्तर: ग्रेड ${summary.grade} (${summary.deviceScore}/100)

${flaggedApps.size} ऐप्स पर ध्यान देने की आवश्यकता है:
$flaggedBullets

$topConcernDesc

✅ सभी जांच आपके फ़ोन पर ऑफलाइन की गईं।
कुछ भी ऑनलाइन साझा नहीं किया गया।
ऐप-अनुमान डाउनलोड करें: github.com/Trusha85/app_anuman
        """.trimIndent()
    } else {
        val topConcernDesc = topConcern?.let { app ->
            val plainReason = app.whyFlagged.firstOrNull() ?: "Holds sensitive unverified permissions"
            """
Top concern: ${app.appName}
$plainReason
Suggested action: Open app settings or uninstall immediately.
            """.trimIndent()
        } ?: "No high risk concerns found."

        val flaggedBullets = if (flaggedApps.isNotEmpty()) {
            flaggedApps.joinToString("\n") { "• ${it.appName}: ${it.whyFlagged.firstOrNull() ?: "Review recommended"}" }
        } else {
            "• All scanned apps look safe."
        }

        """
🛡️ App-Anuman Security Report — $dateStr
Your phone risk level: Grade ${summary.grade} (${summary.deviceScore}/100)

${flaggedApps.size} apps need attention:
$flaggedBullets

$topConcernDesc

✅ All checks done on your device. Nothing was shared online.
Get App-Anuman at: github.com/Trusha85/app_anuman
        """.trimIndent()
    }
}
