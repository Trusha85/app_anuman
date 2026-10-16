package com.appanuman.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.Language
import com.appanuman.data.model.UserMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    apps: List<AppRiskProfile>,
    isScanning: Boolean,
    scanDuration: Float,
    deviceGrade: String,
    deviceScore: Int,
    userMode: UserMode,
    language: Language,
    onScanClick: () -> Unit,
    onQuestionClick: (String) -> Unit,
    onPanicClick: () -> Unit,
    onDormantClick: () -> Unit,
    onShareClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onAllAppsClick: () -> Unit,
    onLanguageToggle: () -> Unit,
    onModeChange: (UserMode) -> Unit
) {
    var showModeDialog by remember { mutableStateOf(false) }

    val gradeColor = when (deviceGrade) {
        "A" -> Color(0xFF2E7D32)
        "B" -> Color(0xFF558B2F)
        "C" -> Color(0xFFF57F17)
        "D" -> Color(0xFFE65100)
        else -> Color(0xFFC62828)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                },
                actions = {
                    // Language Toggle
                    TextButton(onClick = onLanguageToggle) {
                        Text(
                            text = if (language == Language.EN) "हिं" else "EN",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9)
                        )
                    }

                    // Mode Selector Icon
                    IconButton(onClick = { showModeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "User Mode",
                            tint = Color.White
                        )
                    }

                    // Privacy Shield Icon
                    IconButton(onClick = onPrivacyClick) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacy & Security",
                            tint = Color(0xFF81C784)
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
        if (showModeDialog) {
            AlertDialog(
                onDismissRequest = { showModeDialog = false },
                title = { Text(text = "Select Mode") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ListItem(
                            headlineContent = { Text(stringResource(id = R.string.mode_simple)) },
                            leadingContent = { RadioButton(selected = userMode == UserMode.SIMPLE, onClick = null) },
                            modifier = Modifier.clickable {
                                onModeChange(UserMode.SIMPLE)
                                showModeDialog = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(id = R.string.mode_detailed)) },
                            leadingContent = { RadioButton(selected = userMode == UserMode.DETAILED, onClick = null) },
                            modifier = Modifier.clickable {
                                onModeChange(UserMode.DETAILED)
                                showModeDialog = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(id = R.string.mode_grandparent)) },
                            leadingContent = { RadioButton(selected = userMode == UserMode.GRANDPARENT, onClick = null) },
                            modifier = Modifier.clickable {
                                onModeChange(UserMode.GRANDPARENT)
                                showModeDialog = false
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showModeDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // GRANDPARENT MODE
            if (userMode == UserMode.GRANDPARENT) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = gradeColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (deviceScore <= 20)
                                stringResource(id = R.string.grandparent_safe)
                            else if (deviceScore <= 40)
                                stringResource(id = R.string.grandparent_check)
                            else if (deviceScore <= 60)
                                stringResource(id = R.string.grandparent_attention)
                            else
                                stringResource(id = R.string.grandparent_danger),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = if (deviceScore <= 20) "✅" else "⚠️",
                            fontSize = 64.sp
                        )
                    }
                }
            } else {
                // NORMAL QUESTION-FIRST DESIGN (3 Question Cards)
                QuestionCard(
                    question = stringResource(id = R.string.question_otp),
                    icon = "✉️",
                    onClick = { onQuestionClick("OTP") }
                )

                QuestionCard(
                    question = stringResource(id = R.string.question_watch),
                    icon = "👁️",
                    onClick = { onQuestionClick("WATCH") }
                )

                QuestionCard(
                    question = stringResource(id = R.string.question_control),
                    icon = "📱",
                    onClick = { onQuestionClick("CONTROL") }
                )

                // Device Risk Gauge Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF242424))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(id = R.string.device_grade),
                                fontSize = 18.sp,
                                color = Color(0xFFB0B0B0)
                            )
                            Text(
                                text = "Grade $deviceGrade • $deviceScore/100",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(gradeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = deviceGrade,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // "Scan Now" Button (64dp height minimum)
            Button(
                onClick = onScanClick,
                enabled = !isScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
            ) {
                if (isScanning) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Scanning…", fontSize = 20.sp, color = Color.White)
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.scan_now),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Scan Summary text
            if (apps.isNotEmpty() && scanDuration > 0f) {
                Text(
                    text = stringResource(id = R.string.scanned_summary, apps.size, scanDuration),
                    fontSize = 16.sp,
                    color = Color(0xFF81C784),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Quick Nav Links (All Apps, Dormant, Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onAllAppsClick,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = stringResource(id = R.string.all_apps), fontSize = 16.sp)
                }

                OutlinedButton(
                    onClick = onDormantClick,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D))
                ) {
                    Text(text = "Dormant", fontSize = 16.sp)
                }

                OutlinedButton(
                    onClick = onShareClick,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF90CAF9))
                ) {
                    Text(text = "Share", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PANIC BUTTON — Always visible at bottom, large red
            Button(
                onClick = onPanicClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Text(text = "🚨", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(id = R.string.panic_button),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuestionCard(
    question: String,
    icon: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = icon, fontSize = 26.sp)
            Text(
                text = question,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF888888)
            )
        }
    }
}
