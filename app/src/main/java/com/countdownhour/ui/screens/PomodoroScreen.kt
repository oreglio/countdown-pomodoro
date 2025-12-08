package com.countdownhour.ui.screens

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = viewModel(),
    showTodoDialog: Boolean = false,
    onShowTodoDialogChange: (Boolean) -> Unit = {}
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

    // Show todo pool screen or main pomodoro screen
    if (showTodoDialog) {
        TodoPoolScreen(
            todos = state.todoPool,
            selectedIds = state.selectedTodoIds,
            onDismiss = {
                viewModel.refreshActiveTodos()
                onShowTodoDialogChange(false)
            },
            onAddTodo = { viewModel.addTodoToPool(it) },
            onRemoveTodo = { viewModel.removeTodoFromPool(it) },
            onUpdateTodo = { id, text -> viewModel.updateTodoText(id, text) },
            onToggleSelection = { viewModel.toggleTodoSelection(it) },
            onToggleCompletion = { viewModel.toggleTodoPoolCompletion(it) },
            onClearAll = { viewModel.clearAllTodos() },
            onClearCompleted = { viewModel.clearCompletedTodos() },
            onRestoreTodos = { todos, selected -> viewModel.restoreTodos(todos, selected) }
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
                onAddTime = { viewModel.addTime(it) },
                onShowSettings = { showSettings = true },
                onShowTodos = { onShowTodoDialogChange(true) },
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
                onAddTime = { viewModel.addTime(it) },
                onShowSettings = { showSettings = true },
                onShowTodos = { onShowTodoDialogChange(true) },
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
    onAddTime: (Int) -> Unit,
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

    // Preview selected tasks state
    var showSelectedTasksPreview by remember { mutableStateOf(false) }
    val selectedTasks = state.todoPool.filter { it.id in state.selectedTodoIds && !it.isCompleted }

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

    // Main container with tap to dismiss preview
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (showSelectedTasksPreview) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { showSelectedTasksPreview = false })
                    }
                } else Modifier
            )
    ) {
        // Preview of selected tasks (centered, replaces normal content)
        if (showSelectedTasksPreview && state.phase == PomodoroPhase.IDLE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                SelectedTasksPreview(tasks = selectedTasks)
            }
        } else {
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
                        // Todo button with 1-second long press for preview
                        var isPressed by remember { mutableStateOf(false) }

                        LaunchedEffect(isPressed) {
                            if (isPressed && selectedTasks.isNotEmpty()) {
                                delay(1000L)
                                if (isPressed) {
                                    showSelectedTasksPreview = true
                                    isPressed = false
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (state.todoPool.isNotEmpty())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .pointerInput(selectedTasks) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            tryAwaitRelease()
                                            isPressed = false
                                        },
                                        onTap = { onShowTodos() }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FormatListBulleted,
                                contentDescription = "Todos",
                                modifier = Modifier.size(20.dp),
                                tint = if (state.todoPool.isNotEmpty())
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
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
                    val dragThreshold = 40f
                    val currentOnAddTime by rememberUpdatedState(onAddTime)
                    val currentOnShowTodos by rememberUpdatedState(onShowTodos)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = { currentOnShowTodos() })
                            }
                            .pointerInput(state.isRunning) {
                                if (state.isRunning) {
                                    var totalDrag = 0f
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                            val change = event.changes.firstOrNull()
                                            if (change != null && change.pressed) {
                                                val dragAmount = change.position.y - change.previousPosition.y
                                                if (kotlin.math.abs(dragAmount) > 1f) {
                                                    totalDrag += dragAmount

                                                    while (totalDrag <= -dragThreshold) {
                                                        currentOnAddTime(1)
                                                        totalDrag += dragThreshold
                                                    }
                                                    while (totalDrag >= dragThreshold) {
                                                        currentOnAddTime(-1)
                                                        totalDrag -= dragThreshold
                                                    }
                                                }
                                            } else {
                                                totalDrag = 0f
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        // Timer centered, secondary info below
                        Box(contentAlignment = Alignment.Center) {
                            // Timer display (swipeable during running sessions, double-tap for todos)
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

                            // End time positioned below timer
                            if (state.isRunning && state.remainingMillis > 0) {
                                Text(
                                    text = "→ ${formatEndTime(state.remainingMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 72.dp)
                                )
                            }

                            // Elapsed time since session completed (only in IDLE after a session)
                            if (state.phase == PomodoroPhase.IDLE && state.sessionCompletedAt != null && elapsedSinceCompletion > 0) {
                                Text(
                                    text = "+${formatElapsedTime(elapsedSinceCompletion)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 72.dp)
                                )
                            }
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
                    onSkip = onSkip,
                    onAddTime = onAddTime
                )

                // Bottom flexible space to center the block when todos are shown
                if (hasTodosInFocus) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Preview of selected tasks for idle mode (read-only, no toggle)
@Composable
private fun SelectedTasksPreview(tasks: List<PomodoroTodo>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            tasks.forEach { todo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = todo.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
    onAddTime: (Int) -> Unit,
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
    val hasMultipleTodos = state.phase == PomodoroPhase.WORK && state.todos.size > 1

    // Compact layout when multiple todos in focus (tasks left, timer right, no circle)
    if (hasMultipleTodos) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left side - Tasks
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Top
            ) {
                FocusTodoList(
                    todos = state.todos,
                    onToggle = onToggleTodo,
                    compact = false
                )
            }

            // Right side - Timer and controls (no circle)
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Phase label
                Text(
                    text = state.phaseLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Timer display (swipeable during running sessions, double-tap for todos)
                SwipeableTimerText(
                    minutes = state.remainingMinutes,
                    seconds = state.remainingSeconds,
                    isRunning = state.isRunning,
                    onAddMinutes = onAddTime,
                    onDoubleTap = onShowTodos,
                    fontSize = 84.sp,
                    letterSpacing = (-2).sp
                )

                // End time
                if (state.remainingMillis > 0) {
                    Text(
                        text = "→ ${formatEndTime(state.remainingMillis)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pomodoro indicators
                PomodoroIndicators(
                    completedPomodoros = state.completedPomodoros,
                    currentInCycle = state.currentPomodoroInCycle,
                    totalInCycle = state.settings.pomodorosUntilLongBreak
                )

                Text(
                    text = "Total: ${state.completedPomodoros}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                    onSkip = onSkip,
                    onAddTime = onAddTime,
                    compact = true
                )
            }
        }
    } else {
        // Standard layout (idle, single todo, or no todos)
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

                // Todo list during focus (single todo case)
                if (state.phase == PomodoroPhase.WORK && state.todos.size == 1) {
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

                // LARGE timer display - the main focus (swipeable during running sessions, double-tap for todos)
                SwipeableTimerText(
                    minutes = state.remainingMinutes,
                    seconds = state.remainingSeconds,
                    isRunning = state.isRunning,
                    onAddMinutes = onAddTime,
                    onDoubleTap = onShowTodos,
                    fontSize = 120.sp,
                    letterSpacing = (-2).sp
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
                    onAddTime = onAddTime,
                    compact = true
                )
            }
        }
    }
}

@Composable
private fun SwipeableTimerText(
    minutes: Int,
    seconds: Int,
    isRunning: Boolean,
    onAddMinutes: (Int) -> Unit,
    onDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 84.sp,
    letterSpacing: androidx.compose.ui.unit.TextUnit = (-2).sp
) {
    val dragThreshold = 40f // pixels per 1 minute change
    val currentOnAddMinutes by rememberUpdatedState(onAddMinutes)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)

    Text(
        text = String.format("%02d:%02d", minutes, seconds),
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Light,
        letterSpacing = letterSpacing,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { currentOnDoubleTap() }
                )
            }
            .pointerInput(isRunning) {
                if (isRunning) {
                    var totalDrag = 0f
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            val change = event.changes.firstOrNull()
                            if (change != null && change.pressed) {
                                val dragAmount = change.position.y - change.previousPosition.y
                                // Only consume if actually dragging
                                if (kotlin.math.abs(dragAmount) > 1f) {
                                    totalDrag += dragAmount

                                    while (totalDrag <= -dragThreshold) {
                                        currentOnAddMinutes(1)
                                        totalDrag += dragThreshold
                                    }
                                    while (totalDrag >= dragThreshold) {
                                        currentOnAddMinutes(-1)
                                        totalDrag -= dragThreshold
                                    }
                                }
                            } else {
                                totalDrag = 0f
                            }
                        }
                    }
                }
            }
    )
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
    onAddTime: (Int) -> Unit = {},
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

                Spacer(modifier = Modifier.width(12.dp))

                // +5 button (tap = +5mn, long press = +1mn)
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(RoundedCornerShape(buttonSize / 2))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onAddTime(5) },
                                onLongPress = { onAddTime(1) }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+5",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

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
    onUpdateTodo: (String, String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onToggleCompletion: (String) -> Unit,
    onClearAll: () -> Unit,
    onClearCompleted: () -> Unit,
    onRestoreTodos: (List<PomodoroTodo>, Set<String>) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Custom snackbar state with countdown
    var snackbarVisible by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var snackbarBackupTodos by remember { mutableStateOf<List<PomodoroTodo>>(emptyList()) }
    var snackbarBackupSelected by remember { mutableStateOf<Set<String>>(emptySet()) }
    val snackbarProgress = remember { Animatable(1f) }
    val snackbarDurationMs = 10000L

    // Split todos into active and completed
    val activeTodos = todos.filter { !it.isCompleted }
    val completedTodos = todos.filter { it.isCompleted }

    // Animate countdown when snackbar is visible
    LaunchedEffect(snackbarVisible) {
        if (snackbarVisible) {
            snackbarProgress.snapTo(1f)
            snackbarProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = snackbarDurationMs.toInt(),
                    easing = LinearEasing
                )
            )
            // Auto-dismiss when animation completes
            snackbarVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    // Hide keyboard when tapping outside text field
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with trash and done icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 24.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = LocalContext.current
                Text(
                    text = "Task Pool",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                // Export tasks to markdown
                                if (todos.isNotEmpty()) {
                                    val markdown = buildString {
                                        appendLine("# Task Pool")
                                        appendLine()
                                        val active = todos.filter { !it.isCompleted }
                                        val completed = todos.filter { it.isCompleted }

                                        if (active.isNotEmpty()) {
                                            appendLine("## To Do")
                                            active.forEach { todo ->
                                                appendLine("- [ ] ${todo.text}")
                                            }
                                            appendLine()
                                        }

                                        if (completed.isNotEmpty()) {
                                            appendLine("## Done")
                                            completed.forEach { todo ->
                                                appendLine("- [x] ${todo.text}")
                                            }
                                        }
                                    }

                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, markdown)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export Tasks")
                                    context.startActivity(shareIntent)
                                }
                            }
                        )
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (todos.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            // Store backup before clearing
                                            snackbarBackupTodos = todos.toList()
                                            snackbarBackupSelected = selectedIds.toSet()
                                            snackbarMessage = "Cleared ${todos.size} tasks"
                                            onClearAll()
                                            snackbarVisible = true
                                        },
                                        onLongPress = { onClearCompleted() }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Double tap to clear all, long press to clear completed",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Done button
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(horizontal = 8.dp)
                    )
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Input row (only if less than 50 todos)
                if (todos.size < 50) {
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
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (inputText.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                modifier = Modifier
                                    .size(22.dp)
                                    .then(if (inputText.isBlank()) Modifier.alpha(0.40f) else Modifier),
                                tint = if (inputText.isNotBlank())
                                    Color.White
                                else
                                    LocalContentColor.current
                            )
                        }
                    }
                } else {
                    // Limit reached message
                    Text(
                        text = "Task limit reached (50 max)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
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
                    key(todo.id) {
                        TodoPoolItem(
                            todo = todo,
                            isSelected = todo.id in selectedIds,
                            canSelect = todo.id in selectedIds || selectedIds.size < 5,
                            onToggleSelection = { onToggleSelection(todo.id) },
                            onToggleCompletion = { onToggleCompletion(todo.id) },
                            onRemove = { onRemoveTodo(todo.id) },
                            onUpdate = { newText -> onUpdateTodo(todo.id, newText) }
                        )
                    }
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
                        key(todo.id) {
                            TodoPoolItem(
                                todo = todo,
                                isSelected = todo.id in selectedIds,
                                canSelect = todo.id in selectedIds || selectedIds.size < 5,
                                isCompleted = true,
                                onToggleSelection = { onToggleSelection(todo.id) },
                                onToggleCompletion = { onToggleCompletion(todo.id) },
                                onRemove = { onRemoveTodo(todo.id) },
                                onUpdate = { newText -> onUpdateTodo(todo.id, newText) }
                            )
                        }
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

                // Bottom margin for scroll
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Custom snackbar with progress bar countdown
        if (snackbarVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF323232))
            ) {
                // Progress bar inside snackbar, 1px from top with matching border radius
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 1.dp, start = 2.dp, end = 2.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .fillMaxWidth(snackbarProgress.value)
                            .height(3.dp)
                            .background(Color.White)
                    )
                }

                // Content row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = snackbarMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "UNDO",
                        color = MaterialTheme.colorScheme.inversePrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            snackbarVisible = false
                            onRestoreTodos(snackbarBackupTodos, snackbarBackupSelected)
                        }
                    )
                }
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
    onRemove: () -> Unit,
    onUpdate: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(todo.text) { mutableStateOf(todo.text) }
    val focusRequester = remember { FocusRequester() }

    val textColor = if (isCompleted)
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    else if (canSelect)
        MaterialTheme.colorScheme.onSurface
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    // Request focus when entering edit mode
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isEditing -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .pointerInput(isCompleted, canSelect, isEditing) {
                if (!isEditing) {
                    var totalDragX = 0f
                    detectTapGestures(
                        onTap = { if (!isCompleted && canSelect) onToggleSelection() },
                        onLongPress = { onToggleCompletion() }
                    )
                }
            }
            .pointerInput(isCompleted, isEditing) {
                if (!isEditing && !isCompleted) {
                    var totalDragX = 0f
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null && change.pressed) {
                                val dragX = change.position.x - change.previousPosition.x
                                totalDragX += dragX
                                // Swipe left threshold (-80px)
                                if (totalDragX < -80f) {
                                    editText = todo.text
                                    isEditing = true
                                    totalDragX = 0f
                                }
                            } else {
                                totalDragX = 0f
                            }
                        }
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection indicator (round) - show check icon for completed tasks
        Icon(
            imageVector = when {
                isCompleted -> Icons.Default.Check
                isSelected -> Icons.Default.CheckCircle
                else -> Icons.Default.RadioButtonUnchecked
            },
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

        // Text or edit field
        if (isEditing) {
            BasicTextField(
                value = editText,
                onValueChange = { if (it.length <= 72) editText = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (editText.isNotBlank()) {
                            onUpdate(editText)
                        }
                        isEditing = false
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            )
        } else {
            Text(
                text = todo.text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = if (isSelected && !isCompleted) FontWeight.Medium else FontWeight.Normal,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
        }

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
