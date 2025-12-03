package com.countdownhour.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.countdownhour.data.PomodoroTodo
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
    var showTodoDialog by remember { mutableStateOf(false) }

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

    // Show todo pool screen or main pomodoro screen
    if (showTodoDialog) {
        TodoPoolScreen(
            todos = state.todoPool,
            selectedIds = state.selectedTodoIds,
            onDismiss = { showTodoDialog = false },
            onAddTodo = { viewModel.addTodoToPool(it) },
            onRemoveTodo = { viewModel.removeTodoFromPool(it) },
            onToggleSelection = { viewModel.toggleTodoSelection(it) },
            onToggleCompletion = { viewModel.toggleTodoPoolCompletion(it) },
            onClearAll = { viewModel.clearAllTodos() }
        )
    } else {
        if (isLandscape) {
            PomodoroLandscapeLayout(
                state = state,
                onStartWork = { duration, skipTodos -> viewModel.startWork(duration, skipTodos) },
                onStartBreak = { duration -> viewModel.startBreak(duration) },
                onPause = { viewModel.pause() },
                onResume = { viewModel.resume() },
                onReset = { viewModel.reset() },
                onSkip = { viewModel.skipPhase() },
                onShowSettings = { showSettings = true },
                onShowTodos = { showTodoDialog = true },
                onToggleTodo = { viewModel.toggleTodo(it) }
            )
        } else {
            PomodoroPortraitLayout(
                state = state,
                onStartWork = { duration, skipTodos -> viewModel.startWork(duration, skipTodos) },
                onStartBreak = { duration -> viewModel.startBreak(duration) },
                onPause = { viewModel.pause() },
                onResume = { viewModel.resume() },
                onReset = { viewModel.reset() },
                onSkip = { viewModel.skipPhase() },
                onShowSettings = { showSettings = true },
                onShowTodos = { showTodoDialog = true },
                onToggleTodo = { viewModel.toggleTodo(it) }
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
}

@Composable
private fun formatEndTime(remainingMillis: Long): String {
    val endTimeMillis = System.currentTimeMillis() + remainingMillis
    val calendar = java.util.Calendar.getInstance().apply {
        timeInMillis = endTimeMillis
    }
    return String.format("%02d:%02d", calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE))
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
    onStartWork: (Int, Boolean) -> Unit,
    onStartBreak: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onShowSettings: () -> Unit,
    onShowTodos: () -> Unit,
    onToggleTodo: (String) -> Unit
) {
    // Track elapsed time since session completed
    var elapsedSinceCompletion by remember { mutableLongStateOf(0L) }

    // Quick access durations (reset when settings change or on reset)
    val isReset = state.completedPomodoros == 0 && state.phase == PomodoroPhase.IDLE
    var focusDuration by remember(state.settings.workDurationMinutes, isReset) {
        mutableStateOf(state.settings.workDurationMinutes)
    }
    val breakDuration = if (state.currentPomodoroInCycle >= state.settings.pomodorosUntilLongBreak) {
        state.settings.longBreakMinutes
    } else {
        state.settings.shortBreakMinutes
    }
    var customBreakDuration by remember(breakDuration, isReset) { mutableStateOf(breakDuration) }

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
    val hasTodosInFocus = state.phase == PomodoroPhase.WORK && state.todos.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = if (hasTodosInFocus) 12.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (hasTodosInFocus) Arrangement.Top else Arrangement.Center
    ) {
        // Todo and Settings buttons (only when idle)
        if (state.phase == PomodoroPhase.IDLE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Todo button
                FilledIconButton(
                    onClick = onShowTodos,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (state.todoPool.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.FormatListBulleted,
                        contentDescription = "Todos",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Settings button
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

        // Todo list during focus (at very top)
        if (state.phase == PomodoroPhase.WORK && state.todos.isNotEmpty()) {
            FocusTodoList(
                todos = state.todos,
                onToggle = onToggleTodo
            )
            // Flexible space to push content to center of remaining area
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

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

                // End time (only when running)
                if (state.isRunning && state.remainingMillis > 0) {
                    Text(
                        text = "→ ${formatEndTime(state.remainingMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

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
            focusDuration = focusDuration,
            breakDuration = customBreakDuration,
            isLongBreak = state.currentPomodoroInCycle >= state.settings.pomodorosUntilLongBreak,
            hasTodosSelected = state.selectedTodoIds.isNotEmpty(),
            onFocusDurationChange = { focusDuration = (focusDuration + it).coerceIn(5, 120) },
            onBreakDurationChange = { customBreakDuration = (customBreakDuration + it).coerceIn(1, 60) },
            onStartWork = { skipTodos -> onStartWork(focusDuration, skipTodos) },
            onStartBreak = { onStartBreak(customBreakDuration) },
            onPause = onPause,
            onResume = onResume,
            onReset = onReset,
            onSkip = onSkip
        )

        // Bottom flexible space to center the block when todos are shown
        if (hasTodosInFocus) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PomodoroLandscapeLayout(
    state: PomodoroState,
    onStartWork: (Int, Boolean) -> Unit,
    onStartBreak: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onShowSettings: () -> Unit,
    onShowTodos: () -> Unit,
    onToggleTodo: (String) -> Unit
) {
    // Track elapsed time since session completed
    var elapsedSinceCompletion by remember { mutableLongStateOf(0L) }

    // Quick access durations (reset when settings change or on reset)
    val isReset = state.completedPomodoros == 0 && state.phase == PomodoroPhase.IDLE
    var focusDuration by remember(state.settings.workDurationMinutes, isReset) {
        mutableStateOf(state.settings.workDurationMinutes)
    }
    val breakDuration = if (state.currentPomodoroInCycle >= state.settings.pomodorosUntilLongBreak) {
        state.settings.longBreakMinutes
    } else {
        state.settings.shortBreakMinutes
    }
    var customBreakDuration by remember(breakDuration, isReset) { mutableStateOf(breakDuration) }

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

    val hasTodosInFocus = state.phase == PomodoroPhase.WORK && state.todos.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (hasTodosInFocus) 12.dp else 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = if (hasTodosInFocus) Alignment.Top else Alignment.CenterVertically
    ) {
        // Left side - Large timer display
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (hasTodosInFocus) Arrangement.Top else Arrangement.Center
        ) {
            // Todo and Settings buttons (only when idle)
            if (state.phase == PomodoroPhase.IDLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Todo button
                    FilledIconButton(
                        onClick = onShowTodos,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (state.todoPool.isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.FormatListBulleted,
                            contentDescription = "Todos",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Settings button
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

            // Todo list during focus (at very top)
            if (state.phase == PomodoroPhase.WORK && state.todos.isNotEmpty()) {
                FocusTodoList(
                    todos = state.todos,
                    onToggle = onToggleTodo,
                    compact = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

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

            // End time (only when running)
            if (state.isRunning && state.remainingMillis > 0) {
                Text(
                    text = "→ ${formatEndTime(state.remainingMillis)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

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
            // Compact progress circle (hide in IDLE to save space)
            if (state.phase != PomodoroPhase.IDLE) {
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
            }

            Text(
                text = "Total: ${state.completedPomodoros}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(if (state.phase == PomodoroPhase.IDLE) 16.dp else 24.dp))

            // Compact control buttons for landscape
            PomodoroControlButtons(
                phase = state.phase,
                completedPomodoros = state.completedPomodoros,
                focusDuration = focusDuration,
                breakDuration = customBreakDuration,
                isLongBreak = state.currentPomodoroInCycle >= state.settings.pomodorosUntilLongBreak,
                hasTodosSelected = state.selectedTodoIds.isNotEmpty(),
                onFocusDurationChange = { focusDuration = (focusDuration + it).coerceIn(5, 120) },
                onBreakDurationChange = { customBreakDuration = (customBreakDuration + it).coerceIn(1, 60) },
                onStartWork = { skipTodos -> onStartWork(focusDuration, skipTodos) },
                onStartBreak = { onStartBreak(customBreakDuration) },
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
private fun DurationAdjuster(
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onFineTune: (Int) -> Unit,
    compact: Boolean = false
) {
    val dragThreshold = 30f // pixels per 1 minute change

    // Keep callback reference updated for pointerInput
    val currentOnFineTune by rememberUpdatedState(onFineTune)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
    ) {
        FilledIconButton(
            onClick = onDecrease,
            modifier = Modifier.size(if (compact) 28.dp else 32.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "−5",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Swipeable value - swipe up to increase, down to decrease
        Text(
            text = "${value}mn",
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .width(if (compact) 40.dp else 48.dp)
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null && change.pressed) {
                                val dragAmount = change.position.y - (change.previousPosition.y)
                                totalDrag += dragAmount

                                // Negative drag = swipe up = increase
                                // Positive drag = swipe down = decrease
                                while (totalDrag <= -dragThreshold) {
                                    currentOnFineTune(1)
                                    totalDrag += dragThreshold
                                }
                                while (totalDrag >= dragThreshold) {
                                    currentOnFineTune(-1)
                                    totalDrag -= dragThreshold
                                }
                                change.consume()
                            } else {
                                totalDrag = 0f
                            }
                        }
                    }
                },
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        FilledIconButton(
            onClick = onIncrease,
            modifier = Modifier.size(if (compact) 28.dp else 32.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "+5",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PomodoroControlButtons(
    phase: PomodoroPhase,
    completedPomodoros: Int,
    focusDuration: Int = 25,
    breakDuration: Int = 5,
    isLongBreak: Boolean = false,
    hasTodosSelected: Boolean = false,
    onFocusDurationChange: (Int) -> Unit = {},
    onBreakDurationChange: (Int) -> Unit = {},
    onStartWork: (Boolean) -> Unit,  // Boolean = skipTodos (true on long press)
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
            ) {
                // Focus section with long press support
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 12.dp)
                ) {
                    // Focus button with long press detection
                    var isLongPressing by remember { mutableStateOf(false) }

                    OutlinedButton(
                        onClick = { onStartWork(false) },  // Normal tap = with todos
                        modifier = Modifier
                            .width(if (compact) 100.dp else 190.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        isLongPressing = true
                                        onStartWork(true)  // Long press = skip todos
                                    }
                                )
                            }
                    ) {
                        Text(
                            if (compact) "${focusDuration}mn" else "Focus ${focusDuration}mn",
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (!compact) {
                            Text(
                                " → ${formatEndTime(focusDuration * 60 * 1000L)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    DurationAdjuster(
                        value = focusDuration,
                        onDecrease = { onFocusDurationChange(-5) },
                        onIncrease = { onFocusDurationChange(5) },
                        onFineTune = { onFocusDurationChange(it) },
                        compact = compact
                    )
                }

                // Break section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 12.dp)
                ) {
                    OutlinedButton(
                        onClick = onStartBreak,
                        modifier = Modifier.width(if (compact) 100.dp else 190.dp)
                    ) {
                        Text(
                            if (compact) "${breakDuration}mn" else if (isLongBreak) "Long ${breakDuration}mn" else "Break ${breakDuration}mn",
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (!compact) {
                            Text(
                                " → ${formatEndTime(breakDuration * 60 * 1000L)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    DurationAdjuster(
                        value = breakDuration,
                        onDecrease = { onBreakDurationChange(-5) },
                        onIncrease = { onBreakDurationChange(5) },
                        onFineTune = { onBreakDurationChange(it) },
                        compact = compact
                    )
                }

                // Reset button to clear completed count (only if > 0)
                if (completedPomodoros > 0) {
                    FilledIconButton(
                        onClick = onReset,
                        modifier = Modifier.size(if (compact) 36.dp else 40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(if (compact) 18.dp else 20.dp)
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

@Composable
private fun FocusTodoList(
    todos: List<PomodoroTodo>,
    onToggle: (String) -> Unit,
    compact: Boolean = false
) {
    // Subtle card container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            todos.forEach { todo ->
                // All todos black, light grey when completed
                val textColor = if (todo.isCompleted)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                else
                    MaterialTheme.colorScheme.onSurface

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (todo.isCompleted)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            else
                                Color.Transparent
                        )
                        .clickable { onToggle(todo.id) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Round checkbox (grey when completed)
                    Icon(
                        imageVector = if (todo.isCompleted)
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                        tint = textColor  // Same grey as text when completed
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = todo.text,
                        style = if (compact)
                            MaterialTheme.typography.bodySmall
                        else
                            MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        textDecoration = if (todo.isCompleted)
                            TextDecoration.LineThrough
                        else
                            TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoPoolScreen(
    todos: List<PomodoroTodo>,
    selectedIds: Set<String>,
    onDismiss: () -> Unit,
    onAddTodo: (String) -> Unit,
    onRemoveTodo: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onToggleCompletion: (String) -> Unit,
    onClearAll: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    // Split todos into active and completed
    val activeTodos = todos.filter { !it.isCompleted }
    val completedTodos = todos.filter { it.isCompleted }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)  // Space for fixed button
        ) {
            // Header with optional trash icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Task Pool",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                if (todos.isNotEmpty()) {
                    IconButton(
                        onClick = onClearAll,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Clear all tasks",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Input row (only if less than 15 todos)
                if (todos.size < 15) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Add a task...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            BasicTextField(
                                value = inputText,
                                onValueChange = { if (it.length <= 72) inputText = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (inputText.isNotBlank()) {
                                            onAddTodo(inputText)
                                            inputText = ""
                                        }
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onAddTodo(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Selection hint
                if (activeTodos.isNotEmpty()) {
                    Text(
                        text = "Select up to 5 tasks for your focus session (${selectedIds.size}/5)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Active todo list with selection
                activeTodos.forEach { todo ->
                    TodoPoolItem(
                        todo = todo,
                        isSelected = todo.id in selectedIds,
                        canSelect = todo.id in selectedIds || selectedIds.size < 5,
                        onToggleSelection = { onToggleSelection(todo.id) },
                        onToggleCompletion = { onToggleCompletion(todo.id) },
                        onRemove = { onRemoveTodo(todo.id) }
                    )
                }

                // Completed section
                if (completedTodos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "DONE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    completedTodos.forEach { todo ->
                        TodoPoolItem(
                            todo = todo,
                            isSelected = todo.id in selectedIds,
                            canSelect = todo.id in selectedIds || selectedIds.size < 5,
                            isCompleted = true,
                            onToggleSelection = { onToggleSelection(todo.id) },
                            onToggleCompletion = { onToggleCompletion(todo.id) },
                            onRemove = { onRemoveTodo(todo.id) }
                        )
                    }
                }

                // Empty state
                if (todos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Add tasks to focus on during your sessions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Fixed Done button at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun TodoPoolItem(
    todo: PomodoroTodo,
    isSelected: Boolean,
    canSelect: Boolean,
    isCompleted: Boolean = false,
    onToggleSelection: () -> Unit,
    onToggleCompletion: () -> Unit,
    onRemove: () -> Unit
) {
    val textColor = if (isCompleted)
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    else if (canSelect)
        MaterialTheme.colorScheme.onSurface
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (canSelect) onToggleSelection() },
                    onLongPress = { onToggleCompletion() }
                )
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection indicator (round)
        Icon(
            imageVector = if (isSelected)
                Icons.Default.CheckCircle
            else
                Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = when {
                isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                isSelected -> MaterialTheme.colorScheme.primary
                canSelect -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = todo.text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = if (isSelected && !isCompleted) FontWeight.Medium else FontWeight.Normal,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )

        // Delete button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(20.dp),
                tint = if (isCompleted)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
