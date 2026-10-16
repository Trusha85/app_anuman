package com.appanuman.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.appanuman.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    apps: List<AppRiskProfile>,
    userMode: UserMode,
    language: Language,
    filterType: String? = null,
    onAppClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(filterType ?: "ALL") }

    val filteredApps = remember(apps, searchQuery, selectedFilter) {
        apps.filter { app ->
            val matchesSearch = app.label.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "OTP" -> app.grantedPermissions.any { it.contains("SMS") } &&
                        app.grantedPermissions.contains("android.permission.INTERNET")
                "WATCH" -> (app.grantedPermissions.any { it.contains("CAMERA") } ||
                        app.grantedPermissions.any { it.contains("RECORD_AUDIO") }) &&
                        (app.grantedPermissions.contains("android.permission.ACCESS_BACKGROUND_LOCATION") ||
                                app.specialAccess.accessibilityEnabled == SpecialAccessState.ENABLED)
                "CONTROL" -> app.specialAccess.accessibilityEnabled == SpecialAccessState.ENABLED ||
                        app.specialAccess.overlayState == SpecialAccessState.GRANTED
                "CRITICAL" -> app.riskBand == RiskBand.CRITICAL
                "HIGH" -> app.riskBand == RiskBand.HIGH
                "DORMANT" -> app.isDormant
                "SIDELOADED" -> app.installerTrust == InstallerTrust.SIDELOADED
                "MISMATCH" -> app.contextMismatch
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.all_apps),
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(text = stringResource(id = R.string.search_apps), fontSize = 18.sp, color = Color(0xFF888888))
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.White)
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF1E88E5),
                    unfocusedBorderColor = Color(0xFF444444)
                ),
                singleLine = true
            )

            // App rows
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppRowItem(
                        app = app,
                        userMode = userMode,
                        language = language,
                        onClick = { onAppClick(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppRowItem(
    app: AppRiskProfile,
    userMode: UserMode,
    language: Language,
    onClick: () -> Unit
) {
    val bandColor = when (app.riskBand) {
        RiskBand.CRITICAL -> Color(0xFFC62828)
        RiskBand.HIGH -> Color(0xFFE65100)
        RiskBand.MEDIUM -> Color(0xFFF57F17)
        RiskBand.LOW -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (userMode == UserMode.GRANDPARENT) 64.dp else 56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (userMode == UserMode.GRANDPARENT) 64.dp else 48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1).uppercase(),
                    fontSize = if (userMode == UserMode.GRANDPARENT) 26.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = app.label,
                    fontSize = if (userMode == UserMode.GRANDPARENT) 24.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (userMode != UserMode.GRANDPARENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = bandColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = app.riskBand.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = bandColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (app.contextMismatch) {
                            Surface(
                                color = Color(0xFFC62828).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Mismatch",
                                    fontSize = 14.sp,
                                    color = Color(0xFFEF5350),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (app.isDormant) {
                            Surface(
                                color = Color(0xFFE65100).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Dormant",
                                    fontSize = 14.sp,
                                    color = Color(0xFFFFB74D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Score indicator
            if (userMode == UserMode.GRANDPARENT) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(bandColor)
                )
            } else {
                Text(
                    text = "${app.score}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = bandColor
                )
            }
        }
    }
}
