package com.appanuman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.model.TrustLevel
import com.appanuman.ui.theme.LocalRiskColors

@Composable
fun TrustBadge(
    trustLevel: TrustLevel,
    modifier: Modifier = Modifier
) {
    val riskColors = LocalRiskColors.current
    val (bgColor, textColor, textRes) = when (trustLevel) {
        TrustLevel.VERIFIED -> Triple(riskColors.low.copy(alpha = 0.12f), riskColors.low, R.string.trust_verified)
        TrustLevel.SYSTEM_INSTALL -> Triple(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), MaterialTheme.colorScheme.primary, R.string.trust_system)
        TrustLevel.PREINSTALLED -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, R.string.trust_preinstalled)
        TrustLevel.SIDELOADED -> Triple(riskColors.high.copy(alpha = 0.15f), riskColors.high, R.string.trust_sideloaded)
        TrustLevel.UNKNOWN -> Triple(riskColors.medium.copy(alpha = 0.15f), riskColors.medium, R.string.trust_unknown)
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = stringResource(id = textRes),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
    }
}

@Composable
fun ContextMismatchBadge(modifier: Modifier = Modifier) {
    val riskColors = LocalRiskColors.current
    Box(
        modifier = modifier
            .background(color = riskColors.high.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = stringResource(id = R.string.context_mismatch_badge),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = riskColors.high
            )
        )
    }
}

@Composable
fun CreepBadge(modifier: Modifier = Modifier) {
    val riskColors = LocalRiskColors.current
    Box(
        modifier = modifier
            .background(color = riskColors.medium.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = stringResource(id = R.string.list_filter_creep),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = riskColors.medium
            )
        )
    }
}
