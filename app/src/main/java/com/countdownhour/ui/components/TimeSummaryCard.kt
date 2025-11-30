package com.countdownhour.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun TimeSummaryCard(
    targetHour: Int,
    targetMinute: Int,
    modifier: Modifier = Modifier
) {
    // Track current time for dynamic updates
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Update current time every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    // Calculate current hour and minute
    val now = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)

    // Calculate difference
    val target = Calendar.getInstance().apply {
        timeInMillis = currentTimeMillis
        set(Calendar.HOUR_OF_DAY, targetHour)
        set(Calendar.MINUTE, targetMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(now)) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val diffMillis = target.timeInMillis - now.timeInMillis
    val diffHours = (diffMillis / 3600000).toInt()
    val diffMinutes = ((diffMillis % 3600000) / 60000).toInt()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // NOW
            TimeBlock(
                label = "NOW",
                time = String.format("%02d:%02d", currentHour, currentMinute),
                isHighlighted = false
            )

            // Arrow with duration
            DurationIndicator(
                hours = diffHours,
                minutes = diffMinutes
            )

            // TARGET
            TimeBlock(
                label = "TARGET",
                time = String.format("%02d:%02d", targetHour, targetMinute),
                isHighlighted = true
            )
        }
    }
}

@Composable
private fun TimeBlock(
    label: String,
    time: String,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (isHighlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isHighlighted) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
                .border(
                    width = if (isHighlighted) 2.dp else 1.dp,
                    color = if (isHighlighted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = time,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun DurationIndicator(
    hours: Int,
    minutes: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Arrow dots
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Left dots
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.3f + (index * 0.2f)
                            )
                        )
                )
                Spacer(modifier = Modifier.width(3.dp))
            }

            // Arrow head
            Text(
                text = "▸",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Duration text
        Text(
            text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
