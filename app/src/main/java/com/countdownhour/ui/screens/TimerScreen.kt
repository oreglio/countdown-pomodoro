package com.countdownhour.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.countdownhour.data.TimerState
import com.countdownhour.data.TimerStatus
import com.countdownhour.ui.components.CircularProgress
import com.countdownhour.ui.components.ControlButtons
import com.countdownhour.ui.components.TimePickerDialog
import com.countdownhour.ui.components.TimeSummaryCard
import com.countdownhour.viewmodel.TimerViewModel

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = viewModel()
) {
    val timerState by viewModel.timerState.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        TimerLandscapeLayout(
            timerState = timerState,
            onShowTimePicker = { showTimePicker = true },
            onStart = { viewModel.start() },
            onPause = { viewModel.pause() },
            onResume = { viewModel.resume() },
            onStop = { viewModel.stop() },
            onReset = { viewModel.reset() }
        )
    } else {
        TimerPortraitLayout(
            timerState = timerState,
            onShowTimePicker = { showTimePicker = true },
            onStart = { viewModel.start() },
            onPause = { viewModel.pause() },
            onResume = { viewModel.resume() },
            onStop = { viewModel.stop() },
            onReset = { viewModel.reset() }
        )
    }

    // Time picker dialog
    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.setTargetTime(hour, minute)
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun TimerPortraitLayout(
    timerState: TimerState,
    onShowTimePicker: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section - Time Summary Card (when target is set)
            AnimatedVisibility(
                visible = timerState.hasTargetSet,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    TimeSummaryCard(
                        targetHour = timerState.targetHour,
                        targetMinute = timerState.targetMinute
                    )
                }
            }

            // Spacer for centering when no target set
            if (!timerState.hasTargetSet) {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Center section - Main Timer Display
            Column(
                modifier = if (timerState.hasTargetSet) {
                    Modifier.weight(1f, fill = false)
                } else {
                    Modifier
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Circular progress with countdown
                CircularProgress(
                    progress = timerState.progress,
                    size = 280.dp,
                    strokeWidth = 14.dp,
                    progressColor = when (timerState.status) {
                        TimerStatus.RUNNING -> MaterialTheme.colorScheme.primary
                        TimerStatus.PAUSED -> MaterialTheme.colorScheme.secondary
                        TimerStatus.FINISHED -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                ) {
                    TimerContent(timerState)
                }
            }

            // Spacer for centering when no target set
            if (!timerState.hasTargetSet) {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Bottom section - Controls
            BottomControls(
                timerState = timerState,
                onShowTimePicker = onShowTimePicker,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onStop = onStop,
                onReset = onReset
            )
        }
    }
}

@Composable
private fun TimerLandscapeLayout(
    timerState: TimerState,
    onShowTimePicker: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side - Large timer display
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (timerState.status) {
                TimerStatus.IDLE -> {
                    if (timerState.hasTargetSet) {
                        // Show target time when set
                        Text(
                            text = String.format("%02d:%02d", timerState.targetHour, timerState.targetMinute),
                            fontSize = 120.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-2).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Target Time",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Set Target Time",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                TimerStatus.FINISHED -> {
                    Text(
                        text = "00:00:00",
                        fontSize = 100.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-2).sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Time's Up!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                else -> {
                    // Running or Paused
                    Text(
                        text = if (timerState.remainingHours > 0) {
                            String.format(
                                "%02d:%02d:%02d",
                                timerState.remainingHours,
                                timerState.remainingMinutes,
                                timerState.remainingSeconds
                            )
                        } else {
                            String.format(
                                "%02d:%02d",
                                timerState.remainingMinutes,
                                timerState.remainingSeconds
                            )
                        },
                        fontSize = if (timerState.remainingHours > 0) 100.sp else 120.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-2).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (timerState.status == TimerStatus.PAUSED) "Paused" else "Remaining",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Right side - Progress circle and controls
        Column(
            modifier = Modifier.weight(0.6f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Compact progress circle (smaller to leave room for buttons)
            CircularProgress(
                progress = timerState.progress,
                size = 100.dp,
                strokeWidth = 8.dp,
                progressColor = when (timerState.status) {
                    TimerStatus.RUNNING -> MaterialTheme.colorScheme.primary
                    TimerStatus.PAUSED -> MaterialTheme.colorScheme.secondary
                    TimerStatus.FINISHED -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            ) {
                Text(
                    text = "${(timerState.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Controls - always visible
            LandscapeControls(
                timerState = timerState,
                onShowTimePicker = onShowTimePicker,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onStop = onStop,
                onReset = onReset
            )
        }
    }
}

@Composable
private fun TimerContent(timerState: TimerState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (timerState.status) {
            TimerStatus.IDLE -> {
                if (timerState.hasTargetSet) {
                    // Show ready state with target
                    Text(
                        text = String.format("%02d:%02d", timerState.targetHour, timerState.targetMinute),
                        fontSize = 48.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ready",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Idle state - invitation to set time
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Set Target",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            TimerStatus.FINISHED -> {
                Text(
                    text = "00",
                    fontSize = 64.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "hours",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "00:00",
                    fontSize = 36.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            else -> {
                // Running or Paused - show countdown
                if (timerState.remainingHours > 0) {
                    Text(
                        text = String.format("%02d", timerState.remainingHours),
                        fontSize = 64.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = "hours",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = String.format(
                        "%02d:%02d",
                        timerState.remainingMinutes,
                        timerState.remainingSeconds
                    ),
                    fontSize = if (timerState.remainingHours > 0) 36.sp else 56.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "min : sec",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BottomControls(
    timerState: TimerState,
    onShowTimePicker: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status message for finished
        AnimatedVisibility(
            visible = timerState.status == TimerStatus.FINISHED,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Time's Up!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You've reached your target time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Time picker button (when idle and no target set, or to change target)
        AnimatedVisibility(
            visible = timerState.status == TimerStatus.IDLE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Button(
                onClick = onShowTimePicker,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (timerState.hasTargetSet) "Change Target Time" else "Select Target Time",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control buttons
        ControlButtons(
            status = timerState.status,
            hasTargetSet = timerState.hasTargetSet,
            onStart = onStart,
            onPause = onPause,
            onResume = onResume,
            onStop = onStop,
            onReset = onReset
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun LandscapeControls(
    timerState: TimerState,
    onShowTimePicker: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Time picker button in landscape
        if (timerState.status == TimerStatus.IDLE) {
            Button(
                onClick = onShowTimePicker,
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (timerState.hasTargetSet) "Change" else "Set Time",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Control buttons
        ControlButtons(
            status = timerState.status,
            hasTargetSet = timerState.hasTargetSet,
            onStart = onStart,
            onPause = onPause,
            onResume = onResume,
            onStop = onStop,
            onReset = onReset
        )
    }
}
