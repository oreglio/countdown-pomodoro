package com.countdownhour.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.countdownhour.data.PomodoroPhase
import com.countdownhour.data.PomodoroSettings
import com.countdownhour.data.PomodoroState
import com.countdownhour.ui.components.PomodoroIndicators
import com.countdownhour.ui.components.PomodoroProgress
import com.countdownhour.ui.components.PomodoroSettingsDialog
import com.countdownhour.viewmodel.PomodoroViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.pomodoroState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var showSettings by remember { mutableStateOf(false) }

    // Set context for service integration
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }

    // Sync timer when app comes back to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncFromBackground()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (isLandscape) {
        PomodoroLandscapeLayout(
            state = state,
            onStartWork = { viewModel.startWork() },
            onStartBreak = { viewModel.startBreak() },
            onPause = { viewModel.pause() },
            onResume = { viewModel.resume() },
            onReset = { viewModel.reset() },
            onSkip = { viewModel.skipPhase() },
            onShowSettings = { showSettings = true }
        )
    } else {
        PomodoroPortraitLayout(
            state = state,
            onStartWork = { viewModel.startWork() },
            onStartBreak = { viewModel.startBreak() },
            onPause = { viewModel.pause() },
            onResume = { viewModel.resume() },
            onReset = { viewModel.reset() },
            onSkip = { viewModel.skipPhase() },
            onShowSettings = { showSettings = true }
        )
    }

    // Settings dialog
    if (showSettings) {
        PomodoroSettingsDialog(
            currentSettings = state.settings,
            onDismiss = { showSettings = false },
            onConfirm = { settings ->
                viewModel.updateSettings(settings)
                showSettings = false
            }
        )
    }
}

@Composable
private fun formatElapsedTime(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
private fun PomodoroPortraitLayout(
    state: PomodoroState,
    onStartWork: () -> Unit,
    onStartBreak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onShowSettings: () -> Unit
) {
    // Track elapsed time since session completed
    var elapsedSinceCompletion by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state.sessionCompletedAt) {
        if (state.sessionCompletedAt != null && state.phase == PomodoroPhase.IDLE) {
            while (true) {
                elapsedSinceCompletion = System.currentTimeMillis() - state.sessionCompletedAt
                delay(1000L)
            }
        } else {
            elapsedSinceCompletion = 0L
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Settings button (only when idle)
        if (state.phase == PomodoroPhase.IDLE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledIconButton(
                    onClick = onShowSettings,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phase label
        Text(
            text = state.phaseLabel,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pomodoro progress circle
        PomodoroProgress(
            progress = state.progress,
            phase = state.phase
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Timer display
                Text(
                    text = String.format(
                        "%02d:%02d",
                        state.remainingMinutes,
                        state.remainingSeconds
                    ),
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Elapsed time since session completed (only in IDLE after a session)
                if (state.phase == PomodoroPhase.IDLE && state.sessionCompletedAt != null && elapsedSinceCompletion > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+${formatElapsedTime(elapsedSinceCompletion)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pomodoro indicators
        PomodoroIndicators(
            completedPomodoros = state.completedPomodoros,
            currentInCycle = state.currentPomodoroInCycle,
            totalInCycle = state.settings.pomodorosUntilLongBreak
        )

        // Total completed
        Text(
            text = "Completed: ${state.completedPomodoros}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Control buttons
        PomodoroControlButtons(
            phase = state.phase,
            completedPomodoros = state.completedPomodoros,
            onStartWork = onStartWork,
            onStartBreak = onStartBreak,
            onPause = onPause,
            onResume = onResume,
            onReset = onReset,
            onSkip = onSkip
        )
    }
}

@Composable
private fun PomodoroLandscapeLayout(
    state: PomodoroState,
    onStartWork: () -> Unit,
    onStartBreak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onShowSettings: () -> Unit
) {
    // Track elapsed time since session completed
    var elapsedSinceCompletion by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state.sessionCompletedAt) {
        if (state.sessionCompletedAt != null && state.phase == PomodoroPhase.IDLE) {
            while (true) {
                elapsedSinceCompletion = System.currentTimeMillis() - state.sessionCompletedAt
                delay(1000L)
            }
        } else {
            elapsedSinceCompletion = 0L
        }
    }

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
            // Settings button (only when idle)
            if (state.phase == PomodoroPhase.IDLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledIconButton(
                        onClick = onShowSettings,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phase label
            Text(
                text = state.phaseLabel,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // LARGE timer display - the main focus
            Text(
                text = String.format(
                    "%02d:%02d",
                    state.remainingMinutes,
                    state.remainingSeconds
                ),
                fontSize = 120.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Elapsed time since session completed (only in IDLE after a session)
            if (state.phase == PomodoroPhase.IDLE && state.sessionCompletedAt != null && elapsedSinceCompletion > 0) {
                Text(
                    text = "+${formatElapsedTime(elapsedSinceCompletion)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pomodoro indicators
            PomodoroIndicators(
                completedPomodoros = state.completedPomodoros,
                currentInCycle = state.currentPomodoroInCycle,
                totalInCycle = state.settings.pomodorosUntilLongBreak
            )
        }

        // Right side - Progress and controls
        Column(
            modifier = Modifier.weight(0.6f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Compact progress circle
            PomodoroProgress(
                progress = state.progress,
                phase = state.phase,
                size = 160.dp,
                strokeWidth = 12.dp
            ) {
                Text(
                    text = "${(state.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Total: ${state.completedPomodoros}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Compact control buttons for landscape
            PomodoroControlButtons(
                phase = state.phase,
                completedPomodoros = state.completedPomodoros,
                onStartWork = onStartWork,
                onStartBreak = onStartBreak,
                onPause = onPause,
                onResume = onResume,
                onReset = onReset,
                onSkip = onSkip,
                compact = true
            )
        }
    }
}

@Composable
private fun PomodoroControlButtons(
    phase: PomodoroPhase,
    completedPomodoros: Int,
    onStartWork: () -> Unit,
    onStartBreak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    compact: Boolean = false
) {
    val buttonSize = if (compact) 48.dp else 56.dp
    val iconSize = if (compact) 24.dp else 28.dp

    when (phase) {
        PomodoroPhase.IDLE -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(
                    onClick = onStartWork,
                    modifier = if (compact) Modifier.width(140.dp) else Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Start Focus", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onStartBreak,
                    modifier = if (compact) Modifier.width(140.dp) else Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Take Break", style = MaterialTheme.typography.labelLarge)
                }

                // Reset button to clear completed count (only if > 0)
                if (completedPomodoros > 0) {
                    Spacer(modifier = Modifier.height(8.dp))

                    FilledIconButton(
                        onClick = onReset,
                        modifier = Modifier.size(if (compact) 40.dp else 48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(if (compact) 20.dp else 24.dp)
                        )
                    }
                }
            }
        }

        PomodoroPhase.WORK, PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onPause,
                    modifier = Modifier.size(buttonSize),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Pause, "Pause", modifier = Modifier.size(iconSize))
                }

                Spacer(modifier = Modifier.width(16.dp))

                FilledIconButton(
                    onClick = onSkip,
                    modifier = Modifier.size(buttonSize),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.SkipNext, "Skip", modifier = Modifier.size(iconSize))
                }
            }
        }

        PomodoroPhase.PAUSED -> {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onResume,
                    modifier = Modifier.size(buttonSize),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, "Resume", modifier = Modifier.size(iconSize))
                }

                Spacer(modifier = Modifier.width(16.dp))

                FilledIconButton(
                    onClick = onReset,
                    modifier = Modifier.size(buttonSize),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Refresh, "Reset", modifier = Modifier.size(iconSize))
                }
            }
        }
    }
}
