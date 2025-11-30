package com.countdownhour.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.countdownhour.data.TimerStatus

@Composable
fun ControlButtons(
    status: TimerStatus,
    hasTargetSet: Boolean = true,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (status) {
            TimerStatus.IDLE -> {
                // Show Start button only if target is set
                if (hasTargetSet) {
                    FilledIconButton(
                        onClick = onStart,
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            TimerStatus.RUNNING -> {
                // Show Pause and Stop buttons
                FilledIconButton(
                    onClick = onPause,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            TimerStatus.PAUSED -> {
                // Show Resume and Stop buttons
                FilledIconButton(
                    onClick = onResume,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            TimerStatus.FINISHED -> {
                // Show Reset button
                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "New Timer",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
