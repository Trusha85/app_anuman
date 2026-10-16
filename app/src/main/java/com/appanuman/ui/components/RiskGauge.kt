package com.appanuman.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.model.RiskBand
import com.appanuman.ui.theme.LocalRiskColors

@Composable
fun RiskGauge(
    score: Int,
    grade: String,
    modifier: Modifier = Modifier
) {
    val riskColors = LocalRiskColors.current
    val bandColor: Color = when {
        score >= 75 -> riskColors.critical
        score >= 50 -> riskColors.high
        score >= 25 -> riskColors.medium
        else -> riskColors.low
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .size(190.dp)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()

            // Background arc (240 degrees sweep starting from 150)
            drawArc(
                color = trackColor,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Value arc
            val sweep = (score / 100f) * 240f
            if (sweep > 0f) {
                drawArc(
                    color = bandColor,
                    startAngle = 150f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center typography: grade letter 48sp bold, outer score 24sp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = grade,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = bandColor
                )
            )
            Text(
                text = "$score / 100",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
