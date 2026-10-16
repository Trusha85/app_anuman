package com.appanuman.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.model.UserMode
import com.appanuman.ui.theme.LocalRiskColors

@Composable
fun SafeToKeepCard(
    score: Int,
    userMode: UserMode,
    modifier: Modifier = Modifier
) {
    val riskColors = LocalRiskColors.current
    val (cardColor, titleRes, descRes) = when {
        score >= 75 -> Triple(riskColors.critical, R.string.verdict_act_now_title, R.string.verdict_act_now_desc)
        score >= 50 -> Triple(riskColors.high, R.string.verdict_attention_title, R.string.verdict_attention_desc)
        score >= 25 -> Triple(riskColors.medium, R.string.verdict_checking_title, R.string.verdict_checking_desc)
        else -> Triple(riskColors.low, R.string.verdict_safe_title, R.string.verdict_safe_desc)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(id = titleRes),
                style = if (userMode == UserMode.GRANDPARENT) {
                    MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                },
                color = cardColor
            )
            Text(
                text = stringResource(id = descRes),
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
