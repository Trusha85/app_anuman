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
import com.appanuman.model.RiskBand
import com.appanuman.ui.theme.LocalRiskColors

@Composable
fun RiskBandChip(
    band: RiskBand,
    modifier: Modifier = Modifier
) {
    val riskColors = LocalRiskColors.current
    val (bgColor, textColor, textRes) = when (band) {
        RiskBand.LOW -> Triple(riskColors.low.copy(alpha = 0.15f), riskColors.low, R.string.band_low)
        RiskBand.MEDIUM -> Triple(riskColors.medium.copy(alpha = 0.15f), riskColors.medium, R.string.band_medium)
        RiskBand.HIGH -> Triple(riskColors.high.copy(alpha = 0.15f), riskColors.high, R.string.band_high)
        RiskBand.CRITICAL -> Triple(riskColors.critical.copy(alpha = 0.15f), riskColors.critical, R.string.band_critical)
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(id = textRes),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = textColor
        )
    }
}
