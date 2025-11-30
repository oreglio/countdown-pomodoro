package com.countdownhour.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.countdownhour.data.PomodoroSettings

@Composable
fun PomodoroSettingsDialog(
    currentSettings: PomodoroSettings,
    onDismiss: () -> Unit,
    onConfirm: (PomodoroSettings) -> Unit
) {
    var workDuration by remember { mutableStateOf(currentSettings.workDurationMinutes.toString()) }
    var shortBreak by remember { mutableStateOf(currentSettings.shortBreakMinutes.toString()) }
    var longBreak by remember { mutableStateOf(currentSettings.longBreakMinutes.toString()) }
    var cycleCount by remember { mutableStateOf(currentSettings.pomodorosUntilLongBreak.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val settings = PomodoroSettings(
                        workDurationMinutes = workDuration.toIntOrNull()?.coerceIn(1, 120) ?: 25,
                        shortBreakMinutes = shortBreak.toIntOrNull()?.coerceIn(1, 60) ?: 5,
                        longBreakMinutes = longBreak.toIntOrNull()?.coerceIn(1, 60) ?: 15,
                        pomodorosUntilLongBreak = cycleCount.toIntOrNull()?.coerceIn(1, 10) ?: 4
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Work duration
                SettingRow(
                    label = "Focus Duration",
                    value = workDuration,
                    onValueChange = { workDuration = it.filter { c -> c.isDigit() } },
                    suffix = "min"
                )

                // Short break
                SettingRow(
                    label = "Short Break",
                    value = shortBreak,
                    onValueChange = { shortBreak = it.filter { c -> c.isDigit() } },
                    suffix = "min"
                )

                // Long break
                SettingRow(
                    label = "Long Break",
                    value = longBreak,
                    onValueChange = { longBreak = it.filter { c -> c.isDigit() } },
                    suffix = "min"
                )

                // Cycles until long break
                SettingRow(
                    label = "Pomodoros until long break",
                    value = cycleCount,
                    onValueChange = { cycleCount = it.filter { c -> c.isDigit() } },
                    suffix = ""
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Info text
                Text(
                    text = "Complete ${cycleCount.toIntOrNull() ?: 4} focus sessions to earn a long break",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.width(72.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
            if (suffix.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
