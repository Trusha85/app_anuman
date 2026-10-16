package com.appanuman.features.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import com.appanuman.R
import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.Language
import com.appanuman.data.model.RiskBand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareFamilyScreen(
    apps: List<AppRiskProfile>,
    deviceGrade: String,
    deviceScore: Int,
    initialLanguage: Language,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var previewLang by remember { mutableStateOf(initialLanguage) }

    val reportText = remember(apps, deviceGrade, deviceScore, previewLang) {
        generateReport(apps, deviceGrade, deviceScore, previewLang)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.share_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                )
            )
        },
        containerColor = Color(0xFF1A1A1A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language selector toggle (EN | HI)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    SegmentedButton(
                        selected = previewLang == Language.EN,
                        onClick = { previewLang = Language.EN },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(
                            text = stringResource(id = R.string.share_lang_en),
                            fontSize = 18.sp,
                            fontWeight = if (previewLang == Language.EN) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    SegmentedButton(
                        selected = previewLang == Language.HI,
                        onClick = { previewLang = Language.HI },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(
                            text = stringResource(id = R.string.share_lang_hi),
                            fontSize = 18.sp,
                            fontWeight = if (previewLang == Language.HI) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.preview_label),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFB0B0B0)
            )

            // Scrollable Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242424))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = reportText,
                        fontSize = 18.sp,
                        color = Color(0xFFEAEAEA),
                        lineHeight = 24.sp
                    )
                }
            }

            // Share Button (minimum 56dp height)
            Button(
                onClick = {
                    ShareCompat.IntentBuilder(context)
                        .setType("text/plain")
                        .setText(reportText)
                        .setSubject(
                            if (previewLang == Language.HI) "App-Anuman सुरक्षा रिपोर्ट"
                            else "App-Anuman Security Report"
                        )
                        .startChooser()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.share_btn_text),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Privacy Notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.share_privacy_notice),
                    fontSize = 14.sp,
                    color = Color(0xFFB0B0B0)
                )
            }
        }
    }
}

private fun generateReport(
    apps: List<AppRiskProfile>,
    grade: String,
    score: Int,
    language: Language
): String {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val currentDate = dateFormat.format(Date())

    val attentionApps = apps.filter { it.riskBand == RiskBand.HIGH || it.riskBand == RiskBand.CRITICAL }
        .take(5)

    val topRiskyApp = attentionApps.maxByOrNull { it.score }

    return if (language == Language.HI) {
        buildString {
            appendLine("App-Anuman सुरक्षा रिपोर्ट — $currentDate")
            appendLine("आपके फोन का सुरक्षा स्तर: $grade — $score/100")
            appendLine()
            if (attentionApps.isNotEmpty()) {
                appendLine("${attentionApps.size} ऐप्स पर ध्यान देने की आवश्यकता है:")
                for (app in attentionApps) {
                    val finding = app.waterfall.firstOrNull()?.label_hi
                        ?: "संवेदनशील अनुमतियां और पृष्ठभूमि पहुंच"
                    appendLine("• ${app.label}: $finding")
                }
                appendLine()
            } else {
                appendLine("कोई उच्च जोखिम वाला ऐप नहीं मिला। आपका फोन सुरक्षित लगता है।")
                appendLine()
            }

            if (topRiskyApp != null) {
                val topNarrative = topRiskyApp.threatNarratives.firstOrNull()
                val explanation = topNarrative?.body_hi
                    ?: "इस ऐप के पास अत्यधिक संवेदनशील अनुमतियां हैं जो निजी डेटा को खतरे में डाल सकती हैं।"
                appendLine("सबसे महत्वपूर्ण चिंता: ${topRiskyApp.label}")
                appendLine(explanation)
                appendLine("सुझाई गई कार्रवाई: उस ऐप की सेटिंग्स खोलें और उसकी अनुमतियों की समीक्षा करें, या यदि आप इसका उपयोग नहीं करते हैं तो इसे अनइंस्टॉल करें।")
                appendLine()
            }

            appendLine("सभी जांच आपके फोन पर ही की गईं।")
            appendLine("ऑनलाइन कुछ भी नहीं भेजा गया। कोई ऐप डेटा साझा नहीं किया गया।")
            appendLine("—")
            appendLine("App-Anuman द्वारा जांचा गया (मुफ्त, ऑन-डिवाइस सुरक्षा)")
        }
    } else {
        buildString {
            appendLine("App-Anuman Security Report — $currentDate")
            appendLine("Your phone's safety level: $grade — $score/100")
            appendLine()
            if (attentionApps.isNotEmpty()) {
                appendLine("${attentionApps.size} apps need attention:")
                for (app in attentionApps) {
                    val finding = app.waterfall.firstOrNull()?.label_en
                        ?: "Sensitive privileges and background access"
                    appendLine("• ${app.label}: $finding")
                }
                appendLine()
            } else {
                appendLine("No high risk apps found. Your phone looks safe.")
                appendLine()
            }

            if (topRiskyApp != null) {
                val topNarrative = topRiskyApp.threatNarratives.firstOrNull()
                val explanation = topNarrative?.body_en
                    ?: "This app holds excessive sensitive privileges that could expose your private data."
                appendLine("Most important: ${topRiskyApp.label}")
                appendLine(explanation)
                appendLine("Suggested action: Open that app's settings and review its permissions, or uninstall it if you don't use it.")
                appendLine()
            }

            appendLine("All checks were done on your phone.")
            appendLine("Nothing was sent online. No app data was shared.")
            appendLine("—")
            appendLine("Checked by App-Anuman (free, on-device security)")
        }
    }
}
