package com.appanuman.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.appanuman.data.model.RiskBand
import com.appanuman.data.model.UserMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    app: AppRiskProfile?,
    userMode: UserMode,
    language: Language,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    if (app == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "App not found", color = Color.White, fontSize = 20.sp)
        }
        return
    }

    val bandColor = when (app.riskBand) {
        RiskBand.CRITICAL -> Color(0xFFC62828)
        RiskBand.HIGH -> Color(0xFFE65100)
        RiskBand.MEDIUM -> Color(0xFFF57F17)
        RiskBand.LOW -> Color(0xFF2E7D32)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = app.label,
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242424))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF333333)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.label.take(1).uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = app.label,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = app.packageName,
                        fontSize = 16.sp,
                        color = Color(0xFF9E9E9E)
                    )

                    if (userMode != UserMode.GRANDPARENT) {
                        Text(
                            text = "${app.score}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = bandColor
                        )

                        Surface(
                            color = bandColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = app.riskBand.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = bandColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // "Safe to Keep?" verdict card (Simple + Grandparent mode)
            if (userMode != UserMode.DETAILED) {
                val verdictTitle = when (app.riskBand) {
                    RiskBand.LOW -> stringResource(id = R.string.safe_verdict)
                    RiskBand.MEDIUM -> stringResource(id = R.string.check_verdict)
                    RiskBand.HIGH -> stringResource(id = R.string.attention_verdict)
                    RiskBand.CRITICAL -> stringResource(id = R.string.danger_verdict)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = bandColor.copy(alpha = 0.25f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(bandColor))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.safe_to_keep),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE0E0E0)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = verdictTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = bandColor
                        )
                    }
                }
            }

            // Threat Narratives
            if (app.threatNarratives.isNotEmpty()) {
                for (narrative in app.threatNarratives) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2222)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFC62828)))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (language == Language.HI) narrative.title_hi else narrative.title_en,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF5350)
                            )
                            Text(
                                text = if (language == Language.HI) narrative.body_hi else narrative.body_en,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = "💡 " + if (language == Language.HI) narrative.analogy_hi else narrative.analogy_en,
                                fontSize = 18.sp,
                                color = Color(0xFFFFB74D),
                                fontWeight = FontWeight.Medium
                            )
                            if (userMode == UserMode.DETAILED && narrative.mitre_ref.isNotEmpty()) {
                                Text(
                                    text = "MITRE ATT&CK: ${narrative.mitre_ref}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF90CAF9)
                                )
                            }
                        }
                    }
                }
            }

            // Why Flagged (3 plain bullets)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242424))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.why_flagged),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    val topFindings = app.waterfall.take(3)
                    if (topFindings.isNotEmpty()) {
                        for (finding in topFindings) {
                            Text(
                                text = "• " + (if (language == Language.HI) finding.label_hi else finding.label_en),
                                fontSize = 18.sp,
                                color = Color(0xFFE0E0E0)
                            )
                        }
                    } else {
                        Text(
                            text = if (language == Language.HI) "• कोई खतरनाक गतिविधि नहीं पाई गई" else "• No dangerous activity patterns found",
                            fontSize = 18.sp,
                            color = Color(0xFF81C784)
                        )
                    }
                }
            }

            // Grandparent Help Button
            if (userMode == UserMode.GRANDPARENT) {
                Button(
                    onClick = {
                        val helpText = if (language == Language.HI) {
                            "मेरे फोन पर इस ऐप को जांचने में मदद चाहिए: ${app.label}। एक सुरक्षा ऐप ने इसे चिह्नित किया है। क्या आप देख सकते हैं?"
                        } else {
                            "I need help checking this app on my phone: ${app.label}. A security app flagged it. Can you look at it for me?"
                        }
                        ShareCompat.IntentBuilder(context)
                            .setType("text/plain")
                            .setText(helpText)
                            .startChooser()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.get_help),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Action Buttons (Open settings & Uninstall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.open_settings), fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.uninstall), fontSize = 16.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
