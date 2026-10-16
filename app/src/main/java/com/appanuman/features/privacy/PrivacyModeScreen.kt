package com.appanuman.features.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.BuildConfig
import com.appanuman.R
import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.Language

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyModeScreen(
    selfProfile: AppRiskProfile?,
    language: Language,
    onNavigateBack: () -> Unit
) {
    val selfScore = selfProfile?.score ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.privacy_mode_title),
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
            Spacer(modifier = Modifier.height(4.dp))

            // Self Audit Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3321)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2E7D32)))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.self_audit_title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.self_audit_desc),
                        fontSize = 18.sp,
                        color = Color(0xFFC8E6C9)
                    )

                    Surface(
                        color = Color(0xFF2E7D32),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.self_audit_proof, selfScore),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = if (language == Language.HI)
                            "\"इस ऐप को अपने स्वयं के ऑडिट पर कम (LOW) स्कोर करना चाहिए — और यह करता है।\""
                        else
                            "\"This app should score LOW on its own audit — and it does.\"",
                        fontSize = 16.sp,
                        color = Color(0xFFA5D6A7),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Security Guarantees
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242424))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (language == Language.HI) "सुरक्षा उपाय और वास्तुकला" else "Security Guarantees",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    SecurityCheckRow(
                        title = if (language == Language.HI) "शून्य नेटवर्क अनुमति (No INTERNET)" else "Zero Network Permission",
                        desc = if (language == Language.HI) "App-Anuman किसी भी सर्वर से कनेक्ट नहीं हो सकता।" else "App-Anuman cannot connect to any external server."
                    )

                    SecurityCheckRow(
                        title = "FLAG_SECURE Active",
                        desc = if (language == Language.HI) "स्क्रीनशॉट और स्क्रीन रिकॉर्डिंग अवरोधित हैं।" else "Screenshots and screen recording of results are prevented."
                    )

                    SecurityCheckRow(
                        title = "EncryptedSharedPreferences",
                        desc = if (language == Language.HI) "प्राथमिकताएं डिवाइस पर AES-256 से एन्क्रिप्टेड हैं।" else "Preferences & diff snapshots encrypted using AES-256."
                    )

                    SecurityCheckRow(
                        title = "debuggable=false & allowBackup=false",
                        desc = if (language == Language.HI) "कोई बैकअप लीक नहीं, कोई डिबग पोर्ट खुला नहीं।" else "No cloud backup leakage, hardened manifest."
                    )
                }
            }

            // App & Scan Engine Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242424))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (language == Language.HI) "संस्करण विवरण" else "Engine Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "App-Anuman v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        fontSize = 16.sp,
                        color = Color(0xFFB0B0B0)
                    )
                    Text(
                        text = "Risk Scoring Engine: Deterministic v1.0 (Zero network)",
                        fontSize = 16.sp,
                        color = Color(0xFFB0B0B0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SecurityCheckRow(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF81C784),
            modifier = Modifier.size(24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = desc,
                fontSize = 16.sp,
                color = Color(0xFFB0B0B0)
            )
        }
    }
}
