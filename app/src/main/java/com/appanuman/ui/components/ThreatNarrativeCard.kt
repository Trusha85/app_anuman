package com.appanuman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.model.Language
import com.appanuman.model.ThreatNarrative
import com.appanuman.model.UserMode
import com.appanuman.ui.theme.LocalRiskColors

@Composable
fun ThreatNarrativeCard(
    narrative: ThreatNarrative,
    userMode: UserMode,
    language: Language,
    modifier: Modifier = Modifier
) {
    val riskColors = LocalRiskColors.current
    val title = if (language == Language.HI) narrative.title_hi else narrative.title_en
    val body = if (language == Language.HI) narrative.body_hi else narrative.body_en
    val analogy = if (language == Language.HI) narrative.analogy_hi else narrative.analogy_en

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = riskColors.high.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = if (userMode == UserMode.GRANDPARENT) {
                        MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    },
                    color = riskColors.high,
                    modifier = Modifier.weight(1f)
                )

                RiskBandChip(band = narrative.severity)
            }

            Text(
                text = body,
                style = if (userMode == UserMode.GRANDPARENT) {
                    MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp)
                } else {
                    MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                },
                color = MaterialTheme.colorScheme.onSurface
            )

            // Analogy in italics (Simple and Grandparent modes)
            if (userMode != UserMode.DETAILED && analogy.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 “$analogy”",
                        style = if (userMode == UserMode.GRANDPARENT) {
                            MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, fontStyle = FontStyle.Italic)
                        } else {
                            MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // MITRE reference chip (Detailed mode only)
            if (userMode == UserMode.DETAILED && narrative.mitre_ref.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.mitre_ref, narrative.mitre_ref),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
