package com.appanuman.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.model.*
import com.appanuman.ui.components.AppTopBar
import com.appanuman.ui.components.RiskBandChip
import com.appanuman.ui.theme.LocalRiskColors

@Composable
fun AboutScreen(
    selfAuditProfile: AppRiskProfile,
    userMode: UserMode,
    language: Language,
    onBackClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val riskColors = LocalRiskColors.current

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.about_title),
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
            // Self-Audit Result Card at top (Must show LOW <= 10)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = riskColors.low.copy(alpha = 0.12f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = riskColors.low,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = selfAuditProfile.appName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(id = R.string.about_self_score, selfAuditProfile.riskScore),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = riskColors.low
                                )
                            }
                        }

                        RiskBandChip(band = RiskBand.LOW)
                    }

                    Text(
                        text = stringResource(id = R.string.about_self_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.about_quote),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Info Section Cards
            AboutInfoCard(
                icon = Icons.Default.Key,
                title = stringResource(id = R.string.about_perms_title),
                description = stringResource(id = R.string.about_perms_desc)
            )

            AboutInfoCard(
                icon = Icons.Default.Storage,
                title = stringResource(id = R.string.about_storage_title),
                description = stringResource(id = R.string.about_storage_desc)
            )

            AboutInfoCard(
                icon = Icons.Default.WifiOff,
                title = stringResource(id = R.string.about_online_title),
                description = stringResource(id = R.string.about_online_desc)
            )

            AboutInfoCard(
                icon = Icons.Default.Security,
                title = stringResource(id = R.string.about_security_title),
                description = stringResource(id = R.string.about_security_desc)
            )

            AboutInfoCard(
                icon = Icons.Default.Info,
                title = stringResource(id = R.string.about_version_title),
                description = stringResource(id = R.string.about_version_desc)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AboutInfoCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
