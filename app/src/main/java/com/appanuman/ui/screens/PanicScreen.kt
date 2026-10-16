package com.appanuman.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.model.*
import com.appanuman.ui.components.AppTopBar
import com.appanuman.ui.theme.LocalRiskColors
import com.appanuman.ui.theme.PanicRed

@Composable
fun PanicScreen(
    panicResult: PanicScanResult,
    userMode: UserMode,
    language: Language,
    onRunScan: () -> Unit,
    onAppDetailClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val riskColors = LocalRiskColors.current

    LaunchedEffect(Unit) {
        onRunScan()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.panic_title),
                showBackButton = true,
                showModeSelector = false,
                currentLanguage = language,
                userMode = userMode,
                onBackClick = onBackClick,
                onLanguageToggle = onToggleLanguage
            )
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
            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PanicRed.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PanicRed,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.panic_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = PanicRed)
                        )
                        Text(
                            text = stringResource(id = R.string.panic_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Scanning progress indicator
            if (panicResult.isScanning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { panicResult.completedSteps / 5f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = PanicRed,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = stringResource(id = R.string.panic_running),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // Final Verdict Banner
                val (verdictColor, verdictTextRes) = when (panicResult.verdict) {
                    PanicVerdict.SAFE -> Pair(riskColors.low, R.string.panic_verdict_safe)
                    PanicVerdict.CONCERN -> Pair(riskColors.medium, R.string.panic_verdict_concern)
                    PanicVerdict.WARNING -> Pair(PanicRed, R.string.panic_verdict_warning)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = verdictColor.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = verdictTextRes),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = verdictColor
                            ),
                            textAlign = TextAlign.Center
                        )

                        // Share warning button
                        Button(
                            onClick = {
                                sharePanicReport(context, panicResult, language)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .minimumInteractiveComponentSize(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = verdictColor)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.panic_share_action),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Checklist of all 5 security vectors
            Text(
                text = "Live Verification Results",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            panicResult.checks.forEach { check ->
                val isVisible = !panicResult.isScanning || check.id <= panicResult.completedSteps

                AnimatedVisibility(visible = isVisible) {
                    PanicCheckCard(
                        check = check,
                        language = language,
                        onAppClick = { pkg -> onAppDetailClick(pkg) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PanicCheckCard(
    check: PanicCheckItem,
    language: Language,
    onAppClick: (String) -> Unit
) {
    val riskColors = LocalRiskColors.current
    val title = if (language == Language.HI) check.title_hi else check.title_en
    val desc = if (language == Language.HI) check.description_hi else check.description_en

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (check.isTriggered) riskColors.high.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (check.isTriggered) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (check.isTriggered) riskColors.high else riskColors.low,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (check.isTriggered) riskColors.high else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (check.isTriggered && check.flaggedPackageName != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Flagged: ${check.flaggedAppName ?: check.flaggedPackageName}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = riskColors.high)
                    )

                    TextButton(
                        onClick = { onAppClick(check.flaggedPackageName) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("View App →")
                    }
                }
            }
        }
    }
}

private fun sharePanicReport(context: Context, panicResult: PanicScanResult, language: Language) {
    val message = if (panicResult.verdict == PanicVerdict.SAFE) {
        context.getString(R.string.panic_share_safe_msg)
    } else {
        val triggered = panicResult.checks.filter { it.isTriggered }
            .joinToString("\n• ") { it.flaggedAppName ?: it.title_en }
        context.getString(R.string.panic_share_warning_msg, "\n• $triggered")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Scam Alert")
    context.startActivity(shareIntent)
}
