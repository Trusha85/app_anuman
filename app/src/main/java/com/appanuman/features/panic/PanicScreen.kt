package com.appanuman.features.panic

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import com.appanuman.R
import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.Language
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanicScreen(
    apps: List<AppRiskProfile>,
    language: Language,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val checkResult = remember(apps) { PanicCheckEngine.runCheck(apps) }

    // Animated reveal state (1 to 5)
    var visibleCheckIndex by remember { mutableIntStateOf(0) }
    var animationComplete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (i in 1..5) {
            delay(500)
            visibleCheckIndex = i
        }
        delay(300)
        animationComplete = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_verdict")
    val pulseColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFC62828),
        targetValue = Color(0xFFB71C1C),
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "danger_pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.panic_title),
                        fontSize = 28.sp,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.panic_subtitle),
                    fontSize = 18.sp,
                    color = Color(0xFFB0B0B0)
                )
            }

            // Animated Checklist Items
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF242424))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ChecklistItem(
                        title = stringResource(id = R.string.panic_check1),
                        isVisible = visibleCheckIndex >= 1,
                        hasWarning = checkResult.remoteAccessApps.isNotEmpty(),
                        isDanger = checkResult.remoteAccessApps.isNotEmpty()
                    )

                    ChecklistItem(
                        title = stringResource(id = R.string.panic_check2),
                        isVisible = visibleCheckIndex >= 2,
                        hasWarning = checkResult.recentSpecialAccessApps.isNotEmpty(),
                        isDanger = false
                    )

                    ChecklistItem(
                        title = stringResource(id = R.string.panic_check3),
                        isVisible = visibleCheckIndex >= 3,
                        hasWarning = checkResult.recentSideloads.isNotEmpty(),
                        isDanger = checkResult.recentSideloads.isNotEmpty() && checkResult.recentSpecialAccessApps.isNotEmpty()
                    )

                    ChecklistItem(
                        title = stringResource(id = R.string.panic_check4),
                        isVisible = visibleCheckIndex >= 4,
                        hasWarning = checkResult.activeOverlayRisk,
                        isDanger = checkResult.activeOverlayRisk
                    )

                    ChecklistItem(
                        title = stringResource(id = R.string.panic_check5),
                        isVisible = visibleCheckIndex >= 5,
                        hasWarning = checkResult.upiRiskApps.isNotEmpty(),
                        isDanger = false
                    )
                }
            }

            // Final Verdict Card
            if (animationComplete) {
                item {
                    val verdictCardColor = when (checkResult.verdict) {
                        PanicVerdict.SAFE -> Color(0xFF2E7D32)
                        PanicVerdict.WARNING -> Color(0xFFE65100)
                        PanicVerdict.DANGER -> pulseColor
                    }

                    val verdictText = when (checkResult.verdict) {
                        PanicVerdict.SAFE -> stringResource(id = R.string.panic_verdict_safe)
                        PanicVerdict.WARNING -> stringResource(id = R.string.panic_verdict_warning)
                        PanicVerdict.DANGER -> stringResource(id = R.string.panic_verdict_danger)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = verdictCardColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = verdictText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Triggered Details Cards
                if (checkResult.remoteAccessApps.isNotEmpty()) {
                    item {
                        TriggeredExplanationCard(
                            title = if (language == Language.HI) "रिमोट कंट्रोल ऐप्स पाए गए" else "Remote Control Apps Found",
                            findings = checkResult.remoteAccessApps,
                            explanation = if (language == Language.HI)
                                "ये ऐप्स किसी को भी आपके फोन की स्क्रीन और नियंत्रण दूर से दे सकते हैं।"
                            else
                                "These apps allow full remote control and screen viewing of your phone by third parties.",
                            actionLabel = stringResource(id = R.string.open_settings),
                            onAction = {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                if (checkResult.recentSpecialAccessApps.isNotEmpty()) {
                    item {
                        TriggeredExplanationCard(
                            title = if (language == Language.HI) "हाल के ऐप्स में विशेष अनुमतियां" else "Recent Special Access Apps",
                            findings = checkResult.recentSpecialAccessApps,
                            explanation = if (language == Language.HI)
                                "हाल ही में इंस्टॉल किए गए ऐप के पास स्क्रीन नियंत्रण या एक्सेसिबिलिटी अनुमति है।"
                            else
                                "Apps installed within 48 hours have been granted deep accessibility or screen drawing access.",
                            actionLabel = stringResource(id = R.string.open_settings),
                            onAction = {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                if (checkResult.recentSideloads.isNotEmpty()) {
                    item {
                        TriggeredExplanationCard(
                            title = if (language == Language.HI) "हाल ही में साइडलोड किए गए ऐप्स" else "Recently Sideloaded Apps",
                            findings = checkResult.recentSideloads,
                            explanation = if (language == Language.HI)
                                "ये ऐप्स Play Store से बाहर से पिछले 24 घंटों में इंस्टॉल किए गए हैं।"
                            else
                                "These apps were installed from unknown sources outside the official Play Store in the last 24 hours.",
                            actionLabel = stringResource(id = R.string.open_settings),
                            onAction = {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                if (checkResult.activeOverlayRisk) {
                    item {
                        TriggeredExplanationCard(
                            title = if (language == Language.HI) "नकली स्क्रीन ओवरले का खतरा" else "Screen Overlay / Trojan Risk",
                            findings = listOf(if (language == Language.HI) "बैंकिंग/OTP ओवरले पैटर्न सक्रिय" else "Banking/OTP overlay pattern detected"),
                            explanation = if (language == Language.HI)
                                "एक या अधिक ऐप्स आपकी बैंकिंग ऐप्स के ऊपर फर्जी स्क्रीन दिखाकर पासवर्ड चुरा सकते हैं।"
                            else
                                "One or more apps possess capabilities to superimpose fake interfaces over banking apps to capture inputs.",
                            actionLabel = stringResource(id = R.string.open_settings),
                            onAction = {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                if (checkResult.upiRiskApps.isNotEmpty()) {
                    item {
                        TriggeredExplanationCard(
                            title = if (language == Language.HI) "UPI/भुगतान जोखिम ऐप" else "UPI Fraud Pattern Detected",
                            findings = checkResult.upiRiskApps,
                            explanation = if (language == Language.HI)
                                "यह वित्तीय ऐप आपके SMS और सूचनाएं दोनों पढ़ सकता है।"
                            else
                                "Financial apps holding both SMS reading and notification listener access can intercept payment OTPs.",
                            actionLabel = stringResource(id = R.string.open_settings),
                            onAction = {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                // Share Warning Button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val allFound = mutableListOf<String>()
                            allFound.addAll(checkResult.remoteAccessApps)
                            allFound.addAll(checkResult.recentSpecialAccessApps)
                            allFound.addAll(checkResult.recentSideloads)
                            allFound.addAll(checkResult.upiRiskApps)

                            val listSummary = if (allFound.isEmpty()) "None" else allFound.distinct().joinToString(", ")
                            val verdictStr = when (checkResult.verdict) {
                                PanicVerdict.SAFE -> "SAFE"
                                PanicVerdict.WARNING -> "WARNING"
                                PanicVerdict.DANGER -> "DANGER"
                            }

                            val shareText = if (language == Language.HI) {
                                "App-Anuman स्कैम अलर्ट: $verdictStr। मिले ऐप्स: $listSummary। अपने फोन पर इन ऐप्स की जांच करें। केवल डिवाइस पर स्कैन किया गया।"
                            } else {
                                "App-Anuman Scam Alert: $verdictStr. Found: $listSummary. Check your phone for these apps. Scanned on-device only."
                            }

                            ShareCompat.IntentBuilder(context)
                                .setType("text/plain")
                                .setText(shareText)
                                .setSubject("App-Anuman Scam Alert")
                                .startChooser()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.panic_share_warning),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ChecklistItem(
    title: String,
    isVisible: Boolean,
    hasWarning: Boolean,
    isDanger: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        if (isVisible) {
            when {
                isDanger -> {
                    Text(text = "🚨", fontSize = 20.sp)
                }
                hasWarning -> {
                    Text(text = "⚠️", fontSize = 20.sp)
                }
                else -> {
                    Text(text = "✅", fontSize = 20.sp)
                }
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF1E88E5),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun TriggeredExplanationCard(
    title: String,
    findings: List<String>,
    explanation: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B2B)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE65100)))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (findings.isNotEmpty()) {
                Text(
                    text = "• " + findings.joinToString("\n• "),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFFB74D)
                )
            }

            Text(
                text = explanation,
                fontSize = 18.sp,
                color = Color(0xFFE0E0E0)
            )

            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}
