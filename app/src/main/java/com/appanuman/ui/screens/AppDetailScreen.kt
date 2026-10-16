package com.appanuman.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.model.*
import com.appanuman.ui.components.*
import com.appanuman.ui.theme.LocalRiskColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppDetailScreen(
    app: AppRiskProfile?,
    userMode: UserMode,
    language: Language,
    onBackClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryForGlossary by remember { mutableStateOf<PermissionCategory?>(null) }
    val riskColors = LocalRiskColors.current

    if (selectedCategoryForGlossary != null && app != null) {
        val cat = selectedCategoryForGlossary!!
        val status = app.categoryStatuses[cat] ?: CategoryStatus.NOT_REQUESTED
        val permissions = app.categoryPermissions[cat] ?: emptyList()

        PermissionGlossarySheet(
            category = cat,
            status = status,
            permissionsList = permissions,
            userMode = userMode,
            onDismiss = { selectedCategoryForGlossary = null }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.detail_title),
                showBackButton = true,
                showModeSelector = false,
                currentLanguage = language,
                userMode = userMode,
                onBackClick = onBackClick,
                onLanguageToggle = onToggleLanguage
            )
        },
        bottomBar = {
            if (app != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (userMode == UserMode.GRANDPARENT) {
                        // Grandparent Mode: Single big action button "Get Help With This App"
                        Button(
                            onClick = {
                                shareAppHelp(context, app.appName, language)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .minimumInteractiveComponentSize(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(id = R.string.action_get_help),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        // Simple & Detailed Mode: Actions row (56dp min height)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", app.packageName, null)
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .minimumInteractiveComponentSize(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.action_open_settings),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_DELETE,
                                        Uri.fromParts("package", app.packageName, null)
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .minimumInteractiveComponentSize(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = riskColors.high
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.action_uninstall),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        if (app == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "App details not found",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Icon 72dp
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Android,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Package name (hidden in Simple/Grandparent)
                                if (userMode == UserMode.DETAILED) {
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TrustBadge(trustLevel = app.trustLevel)
                                    RiskBandChip(band = app.riskBand)
                                }
                            }

                            // Risk score 48sp bold (hidden in Grandparent)
                            if (userMode != UserMode.GRANDPARENT) {
                                Column(horizontalAlignment = Alignment.End) {
                                    val scoreColor = when {
                                        app.riskScore >= 75 -> riskColors.critical
                                        app.riskScore >= 50 -> riskColors.high
                                        app.riskScore >= 25 -> riskColors.medium
                                        else -> riskColors.low
                                    }
                                    Text(
                                        text = "${app.riskScore}",
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = scoreColor
                                        )
                                    )
                                    Text(
                                        text = "/100",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Detailed Mode: Dates and targetSdkVersion warning if < 28
                        if (userMode == UserMode.DETAILED) {
                            HorizontalDivider()
                            val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(id = R.string.detail_installed_on, dateFormatter.format(Date(app.firstInstallTime))),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(id = R.string.detail_updated_on, dateFormatter.format(Date(app.lastUpdateTime))),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (app.targetSdkVersion < 28) {
                                Text(
                                    text = stringResource(id = R.string.detail_target_sdk_warning, app.targetSdkVersion),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = riskColors.high
                                    )
                                )
                            }
                        }
                    }
                }

                // "Safe to Keep?" verdict card (Simple + Grandparent modes)
                if (userMode != UserMode.DETAILED) {
                    SafeToKeepCard(
                        score = app.riskScore,
                        userMode = userMode
                    )
                }

                // Permission Creep Alert card
                if (app.permissionCreepFlag) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = riskColors.medium.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(id = R.string.creep_alert_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = riskColors.medium)
                            )
                            val addedStr = app.addedPermissions.joinToString(", ")
                            Text(
                                text = stringResource(id = R.string.creep_alert_desc, 3, addedStr),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Dormant Alert card
                if (app.isDormant) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = riskColors.medium.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = stringResource(id = R.string.dormant_alert_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = riskColors.medium)
                            )
                            Text(
                                text = stringResource(id = R.string.dormant_alert_desc, app.daysUnused, "SMS, GPS Location"),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", app.packageName, null)
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(stringResource(id = R.string.dormant_review_action))
                            }
                        }
                    }
                }

                // Shadow Capability Grid (3x3)
                if (userMode != UserMode.GRANDPARENT) {
                    Text(
                        text = stringResource(id = R.string.cat_special_access) + " & Capabilities",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    CapabilityGrid(
                        statuses = app.categoryStatuses,
                        userMode = userMode,
                        onCategoryClick = { selectedCategoryForGlossary = it }
                    )
                }

                // Threat Narratives (if any triggered)
                if (app.threatNarratives.isNotEmpty() && userMode != UserMode.GRANDPARENT) {
                    Text(
                        text = stringResource(id = R.string.threat_patterns_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    app.threatNarratives.forEach { narrative ->
                        ThreatNarrativeCard(
                            narrative = narrative,
                            userMode = userMode,
                            language = language
                        )
                    }
                }

                // Why Flagged Section (Always shown in all modes)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (userMode == UserMode.GRANDPARENT) {
                                stringResource(id = R.string.what_app_can_do_title)
                            } else {
                                stringResource(id = R.string.why_flagged_title)
                            },
                            style = if (userMode == UserMode.GRANDPARENT) {
                                MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            } else {
                                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            },
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val bullets = if (language == Language.HI) app.whyFlagged_hi else app.whyFlagged
                        bullets.forEach { bullet ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = bullet,
                                    style = if (userMode == UserMode.GRANDPARENT) {
                                        MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp)
                                    } else {
                                        MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                                    },
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Score Waterfall (Detailed mode default, toggle in Simple mode, hidden in Grandparent)
                if (userMode != UserMode.GRANDPARENT && app.waterfall.isNotEmpty()) {
                    ScoreWaterfall(
                        waterfall = app.waterfall,
                        totalScore = app.riskScore,
                        userMode = userMode,
                        language = language
                    )
                }

                // Scan Coverage Footer (Detailed and Simple modes)
                if (userMode != UserMode.GRANDPARENT) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.coverage_title),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            CoverageCheckItem(text = stringResource(id = R.string.coverage_accessibility), isChecked = true)
                            CoverageCheckItem(text = stringResource(id = R.string.coverage_admin), isChecked = true)
                            CoverageCheckItem(text = stringResource(id = R.string.coverage_overlay), isChecked = false, isWarning = true)
                            CoverageCheckItem(text = stringResource(id = R.string.coverage_listeners), isChecked = true)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CoverageCheckItem(text: String, isChecked: Boolean, isWarning: Boolean = false) {
    val riskColors = LocalRiskColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val (icon, color) = when {
            isWarning -> Pair(Icons.Default.Warning, riskColors.medium)
            isChecked -> Pair(Icons.Default.CheckCircle, riskColors.low)
            else -> Pair(Icons.Default.Help, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun shareAppHelp(context: Context, appName: String, language: Language) {
    val message = if (language == Language.HI) {
        context.getString(R.string.help_share_message, appName)
    } else {
        context.getString(R.string.help_share_message, appName)
    }
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Get Help With This App")
    context.startActivity(shareIntent)
}
