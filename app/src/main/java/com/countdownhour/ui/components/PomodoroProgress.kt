package com.countdownhour.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.countdownhour.data.PomodoroPhase

@Composable
fun PomodoroProgress(
    progress: Float,
    phase: PomodoroPhase,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    strokeWidth: Dp = 16.dp,
    content: @Composable () -> Unit = {}
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val arcSize = Size(
                width = size.toPx() - strokeWidthPx,
                height = size.toPx() - strokeWidthPx
            )
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

            // Background circle
            drawArc(
                color = backgroundColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Progress arc (no animation for e-ink)
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        content()
    }
}

@Composable
fun PomodoroIndicators(
    completedPomodoros: Int,
    currentInCycle: Int,
    totalInCycle: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalInCycle) { index ->
            val isCompleted = index < currentInCycle

            Box(
                modifier = Modifier.size(12.dp)
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    if (isCompleted) {
                        drawCircle(color = Color.Black)
                    } else {
                        drawCircle(
                            color = Color.LightGray,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            if (index < totalInCycle - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}
