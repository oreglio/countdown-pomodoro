package com.countdownhour.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true
    )

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
    val targetHour = timePickerState.hour
    val targetMinute = timePickerState.minute

    val target = Calendar.getInstance().apply {
        timeInMillis = currentTimeMillis
        set(Calendar.HOUR_OF_DAY, targetHour)
        set(Calendar.MINUTE, targetMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        // If target is before current time, it's for tomorrow
        if (before(now)) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val diffMillis = target.timeInMillis - now.timeInMillis
    val diffHours = (diffMillis / 3600000).toInt()
    val diffMinutes = ((diffMillis % 3600000) / 60000).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Select Target Time") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dynamic time computation display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current time
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NOW",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%02d:%02d", currentHour, currentMinute),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Arrow and difference
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${diffHours}h ${diffMinutes}m",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Target time
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TARGET",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%02d:%02d", targetHour, targetMinute),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TimePicker(state = timePickerState)
            }
        }
    )
}
