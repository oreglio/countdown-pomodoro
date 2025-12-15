package com.countdownhour.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.countdownhour.data.PomodoroSettings
import kotlin.math.roundToInt

@Composable
fun PomodoroSettingsDialog(
    currentSettings: PomodoroSettings,
    onDismiss: () -> Unit,
    onConfirm: (PomodoroSettings) -> Unit
) {
    var workDuration by remember { mutableFloatStateOf(currentSettings.workDurationMinutes.toFloat()) }
    var shortBreak by remember { mutableFloatStateOf(currentSettings.shortBreakMinutes.toFloat()) }
    var longBreak by remember { mutableFloatStateOf(currentSettings.longBreakMinutes.toFloat()) }
    var cycleCount by remember { mutableFloatStateOf(currentSettings.pomodorosUntilLongBreak.toFloat()) }
    var volumeScrollEnabled by remember { mutableStateOf(currentSettings.volumeButtonScrollEnabled) }
    var volumeScrollPercent by remember { mutableFloatStateOf(currentSettings.volumeScrollPercent.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val settings = PomodoroSettings(
                        workDurationMinutes = workDuration.roundToInt(),
                        shortBreakMinutes = shortBreak.roundToInt(),
                        longBreakMinutes = longBreak.roundToInt(),
                        pomodorosUntilLongBreak = cycleCount.roundToInt(),
                        volumeButtonScrollEnabled = volumeScrollEnabled,
                        volumeScrollPercent = volumeScrollPercent.roundToInt()
                    )
                    onConfirm(settings)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                text = "Pomodoro Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Work duration (5-120 min)
                SliderSetting(
                    label = "Focus Duration",
                    value = workDuration,
                    onValueChange = { workDuration = it },
                    valueRange = 5f..120f,
                    steps = 22, // (120-5)/5 - 1 = 22 steps for 5-min increments
                    suffix = "min"
                )

                // Short break (1-30 min)
                SliderSetting(
                    label = "Short Break",
                    value = shortBreak,
                    onValueChange = { shortBreak = it },
                    valueRange = 1f..30f,
                    steps = 28, // 1-min increments
                    suffix = "min"
                )

                // Long break (5-60 min)
                SliderSetting(
                    label = "Long Break",
                    value = longBreak,
                    onValueChange = { longBreak = it },
                    valueRange = 5f..60f,
                    steps = 10, // 5-min increments
                    suffix = "min"
                )

                // Cycles until long break (2-8)
                SliderSetting(
                    label = "Pomodoros until long break",
                    value = cycleCount,
                    onValueChange = { cycleCount = it },
                    valueRange = 2f..8f,
                    steps = 5, // 1 increment steps
                    suffix = ""
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Info text
                Text(
                    text = "Complete ${cycleCount.roundToInt()} focus sessions to earn a long break",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // E-Ink Section Header
                Text(
                    text = "E-Ink Optimization",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Volume Button Scroll Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Volume button scroll",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = volumeScrollEnabled,
                        onCheckedChange = { volumeScrollEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                // Scroll Amount Slider (only visible when enabled)
                if (volumeScrollEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SliderSetting(
                        label = "Scroll amount",
                        value = volumeScrollPercent,
                        onValueChange = { volumeScrollPercent = it },
                        valueRange = 10f..100f,
                        steps = 8,  // 10% increments
                        suffix = "%"
                    )
                }
            }
        }
    )
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    suffix: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (suffix.isNotEmpty()) "${value.roundToInt()} $suffix" else "${value.roundToInt()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
