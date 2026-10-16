package com.appanuman.features.dormant

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
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
import com.appanuman.R
import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.Language
import com.appanuman.data.model.PermissionCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DormantDangerScreen(
    apps: List<AppRiskProfile>,
    language: Language,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var hasUsagePermission by remember { mutableStateOf(checkUsageStatsPermission(context)) }

    // Recheck permission when screen resumes
    DisposableEffect(Unit) {
        hasUsagePermission = checkUsageStatsPermission(context)
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.dormant_title),
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
        if (!hasUsagePermission) {
            UsagePermissionPrompt(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                },
                onSkipClick = onNavigateBack
            )
        } else {
            val dormantApps = remember(apps) {
                apps.filter { it.isDormant }
                    .sortedByDescending { app ->
                        app.grantedPermissions.count { p ->
                            p.contains("SMS") || p.contains("CAMERA") ||
                                    p.contains("LOCATION") || p.contains("STORAGE")
                        }
                    }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (dormantApps.isNotEmpty()) {
                        // Determine highest-risk category across dormant apps
                        val categoryWord = if (dormantApps.any { it.grantedPermissions.any { p -> p.contains("SMS") } }) {
                            if (language == Language.HI) "संदेश और OTP" else "messages"
                        } else if (dormantApps.any { it.grantedPermissions.any { p -> p.contains("CAMERA") } }) {
                            if (language == Language.HI) "कैमरा" else "camera"
                        } else {
                            if (language == Language.HI) "स्थान (लोकेशन)" else "location"
                        }

                        val summaryText = if (language == Language.HI) {
                            "${dormantApps.size} ऐप्स के पास 30+ दिनों से बिना उपयोग के आपके $categoryWord का एक्सेस है।"
                        } else {
                            "${dormantApps.size} apps have had access to your $categoryWord for over 30 days without being used."
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF332020)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFC62828)))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = summaryText,
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2E1D))
                        ) {
                            Text(
                                text = stringResource(id = R.string.no_dormant_apps),
                                fontSize = 18.sp,
                                color = Color(0xFF81C784),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                items(dormantApps, key = { it.packageName }) { app ->
                    DormantAppItem(
                        app = app,
                        onCleanUp = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DormantAppItem(
    app: AppRiskProfile,
    onCleanUp: () -> Unit
) {
    val dangerousPerms = app.grantedPermissions.filter { p ->
        p.contains("SMS") || p.contains("CAMERA") ||
                p.contains("LOCATION") || p.contains("CONTACTS") ||
                p.contains("STORAGE")
    }
    val dangerWeight = dangerousPerms.size * 15
    val exposureScore = ((app.daysUnused * dangerWeight) / 100).coerceIn(0, 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = app.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                val daysColor = if (app.daysUnused > 60) Color(0xFFEF5350) else Color(0xFFFFB74D)
                Text(
                    text = stringResource(id = R.string.days_unused, app.daysUnused),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = daysColor
                )
            }

            // Sensitive category chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (app.grantedPermissions.any { it.contains("SMS") }) {
                    CategoryBadge("SMS")
                }
                if (app.grantedPermissions.any { it.contains("CAMERA") }) {
                    CategoryBadge("Camera")
                }
                if (app.grantedPermissions.any { it.contains("LOCATION") }) {
                    CategoryBadge("Location")
                }
                if (app.grantedPermissions.any { it.contains("CONTACTS") }) {
                    CategoryBadge("Contacts")
                }
                if (app.grantedPermissions.any { it.contains("STORAGE") }) {
                    CategoryBadge("Storage")
                }
            }

            // Exposure Score Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.exposure_score),
                        fontSize = 18.sp,
                        color = Color(0xFFB0B0B0)
                    )
                    Text(
                        text = "$exposureScore/100",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                LinearProgressIndicator(
                    progress = { exposureScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (exposureScore >= 50) Color(0xFFC62828) else Color(0xFFE65100),
                    trackColor = Color(0xFF333333)
                )
            }

            // Clean Up Button (56dp height minimum)
            Button(
                onClick = onCleanUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
            ) {
                Text(
                    text = stringResource(id = R.string.dormant_clean_up),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(name: String) {
    Surface(
        color = Color(0xFF3A3A3A),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = name,
            fontSize = 14.sp,
            color = Color(0xFFE0E0E0),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun UsagePermissionPrompt(
    modifier: Modifier = Modifier,
    onGrantClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFF1E88E5),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.dormant_permission_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.dormant_permission_desc),
            fontSize = 18.sp,
            color = Color(0xFFB0B0B0),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onGrantClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
        ) {
            Text(
                text = stringResource(id = R.string.dormant_grant_btn),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            onClick = onSkipClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(
                text = stringResource(id = R.string.dormant_skip_btn),
                fontSize = 18.sp,
                color = Color(0xFFB0B0B0)
            )
        }
    }
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    return try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
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
