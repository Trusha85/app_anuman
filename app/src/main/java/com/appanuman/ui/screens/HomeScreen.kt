package com.appanuman.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.model.*
import com.appanuman.ui.components.*
import com.appanuman.ui.theme.LocalRiskColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    userMode: UserMode,
    language: Language,
    deviceSummary: DeviceRiskSummary,
    scanState: ScanState,
    onScanNow: () -> Unit,
    onNavigateToQuestionList: (String) -> Unit,
    onNavigateToPanic: () -> Unit,
    onSelectMode: (UserMode) -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showModeSheet by remember { mutableStateOf(false) }
    val riskColors = LocalRiskColors.current

    if (showModeSheet) {
        ModeSelectorBottomSheet(
            currentMode = userMode,
            onModeSelected = onSelectMode,
            onDismiss = { showModeSheet = false }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = null,
                showBackButton = false,
                showModeSelector = true,
                currentLanguage = language,
                userMode = userMode,
                onModeClick = { showModeSheet = true },
                onLanguageToggle = onToggleLanguage
            )
        },
        bottomBar = {
            // Panic button is always visible at the bottom of the screen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.Transparent
            ) {
                PanicButton(
                    onClick = onNavigateToPanic,
                    userMode = userMode
                )
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userMode == UserMode.GRANDPARENT) {
                // GRANDPARENT MODE VARIANT
                GrandparentHomeContent(
                    deviceSummary = deviceSummary,
                    scanState = scanState,
                    onScanNow = onScanNow
                )
            } else {
                // SIMPLE & DETAILED MODE: Question-First Design
                Text(
                    text = stringResource(id = R.string.home_device_grade_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                // Question Cards
                QuestionCard(
                    title = stringResource(id = R.string.home_question_otp_title),
                    subtitle = stringResource(id = R.string.home_question_otp_subtitle, deviceSummary.otpStealerCount),
                    icon = Icons.Default.Sms,
                    userMode = userMode,
                    onClick = { onNavigateToQuestionList("question_otp") }
                )

                QuestionCard(
                    title = stringResource(id = R.string.home_question_background_title),
                    subtitle = stringResource(id = R.string.home_question_background_subtitle, deviceSummary.backgroundSpyCount),
                    icon = Icons.Default.Visibility,
                    userMode = userMode,
                    onClick = { onNavigateToQuestionList("question_background") }
                )

                QuestionCard(
                    title = stringResource(id = R.string.home_question_screen_title),
                    subtitle = stringResource(id = R.string.home_question_screen_subtitle, deviceSummary.screenControlCount),
                    icon = Icons.Default.TouchApp,
                    userMode = userMode,
                    onClick = { onNavigateToQuestionList("question_screen") }
                )

                // Device Risk Gauge Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RiskGauge(
                            score = deviceSummary.deviceScore,
                            grade = deviceSummary.grade
                        )

                        // Breakdown chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RiskMetricItem(label = stringResource(id = R.string.band_critical), count = deviceSummary.criticalCount, color = riskColors.critical)
                            RiskMetricItem(label = stringResource(id = R.string.band_high), count = deviceSummary.highCount, color = riskColors.high)
                            RiskMetricItem(label = stringResource(id = R.string.band_medium), count = deviceSummary.mediumCount, color = riskColors.medium)
                            RiskMetricItem(label = stringResource(id = R.string.band_low), count = deviceSummary.lowCount, color = riskColors.low)
                        }
                    }
                }

                // Scan Now Button
                Button(
                    onClick = onScanNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = scanState != ScanState.SCANNING
                ) {
                    if (scanState == ScanState.SCANNING) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = stringResource(id = R.string.home_scanning),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Text(
                                text = stringResource(id = R.string.home_scan_now),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Scan Stats & Timestamp
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.home_scanned_stats,
                            deviceSummary.scannedAppsCount,
                            deviceSummary.scanDurationSeconds
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    val timeFormatter = remember { SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()) }
                    val formattedTime = timeFormatter.format(Date(deviceSummary.lastScanTimeMillis))
                    Text(
                        text = stringResource(id = R.string.home_last_scan, formattedTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GrandparentHomeContent(
    deviceSummary: DeviceRiskSummary,
    scanState: ScanState,
    onScanNow: () -> Unit
) {
    val riskColors = LocalRiskColors.current
    val (statusColor, statusIcon, statusTextRes) = when {
        deviceSummary.deviceScore >= 75 -> Triple(riskColors.high, Icons.Default.Warning, R.string.gp_status_critical)
        deviceSummary.deviceScore >= 50 -> Triple(riskColors.high, Icons.Default.Error, R.string.gp_status_high)
        deviceSummary.deviceScore >= 25 -> Triple(riskColors.medium, Icons.Default.Info, R.string.gp_status_medium)
        else -> Triple(riskColors.low, Icons.Default.CheckCircle, R.string.gp_status_safe)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.gp_home_question),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        // One Big Colored Result Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = statusColor.copy(alpha = 0.15f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusColor,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(id = statusTextRes),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Scan button for grandparent mode
        Button(
            onClick = onScanNow,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .minimumInteractiveComponentSize(),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = scanState != ScanState.SCANNING
        ) {
            Text(
                text = stringResource(id = R.string.home_scan_now),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun RiskMetricItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
