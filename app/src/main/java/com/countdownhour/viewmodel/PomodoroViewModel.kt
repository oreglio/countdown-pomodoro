package com.countdownhour.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.countdownhour.data.PomodoroPhase
import com.countdownhour.data.PomodoroSettings
import com.countdownhour.data.PomodoroState
import com.countdownhour.data.PomodoroTodo
import com.countdownhour.service.TimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PomodoroViewModel : ViewModel() {

    private val _pomodoroState = MutableStateFlow(PomodoroState())
    val pomodoroState: StateFlow<PomodoroState> = _pomodoroState.asStateFlow()

    private var timerJob: Job? = null
    private var endTimeMillis: Long = 0L
    private var currentPhaseForResume: PomodoroPhase = PomodoroPhase.IDLE
    private var appContext: Context? = null

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    fun startWork(customDurationMinutes: Int? = null, skipTodoSelection: Boolean = false) {
        // Activate selected todos before starting (unless skipped)
        if (!skipTodoSelection) {
            activateSelectedTodos()
        }

        val state = _pomodoroState.value
        val settings = state.settings
        val durationMinutes = customDurationMinutes ?: settings.workDurationMinutes
        val totalMillis = durationMinutes * 60 * 1000L
        endTimeMillis = System.currentTimeMillis() + totalMillis
        currentPhaseForResume = PomodoroPhase.WORK

        // Reset cycle if all pomodoros were completed (starting fresh cycle)
        val newCycleCount = if (state.currentPomodoroInCycle >= settings.pomodorosUntilLongBreak) {
            0
        } else {
            state.currentPomodoroInCycle
        }

        _pomodoroState.value = _pomodoroState.value.copy(
            phase = PomodoroPhase.WORK,
            totalMillis = totalMillis,
            remainingMillis = totalMillis,
            currentPomodoroInCycle = newCycleCount,
            sessionCompletedAt = null  // Reset idle timer
        )

        appContext?.let { TimerService.startPomodoro(it, totalMillis, PomodoroPhase.WORK) }
        startCountdown()
    }

    fun startBreak(customDurationMinutes: Int? = null) {
        val state = _pomodoroState.value
        val settings = state.settings
        // Long break is available when all pomodoros in cycle are completed
        val isLongBreak = state.currentPomodoroInCycle >= settings.pomodorosUntilLongBreak

        val defaultBreakDuration = if (isLongBreak) {
            settings.longBreakMinutes
        } else {
            settings.shortBreakMinutes
        }
        val breakDuration = customDurationMinutes ?: defaultBreakDuration

        val totalMillis = breakDuration * 60 * 1000L
        val newPhase = if (isLongBreak) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
        endTimeMillis = System.currentTimeMillis() + totalMillis
        currentPhaseForResume = newPhase

        _pomodoroState.value = state.copy(
            phase = newPhase,
            totalMillis = totalMillis,
            remainingMillis = totalMillis,
            sessionCompletedAt = null  // Reset idle timer
        )

        appContext?.let { TimerService.startPomodoro(it, totalMillis, newPhase) }
        startCountdown()
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_pomodoroState.value.remainingMillis > 0 &&
                   _pomodoroState.value.isRunning) {
                delay(1000L)
                // Recalculate from end time for accuracy
                val remaining = endTimeMillis - System.currentTimeMillis()
                _pomodoroState.value = _pomodoroState.value.copy(
                    remainingMillis = remaining.coerceAtLeast(0)
                )
            }

            if (_pomodoroState.value.remainingMillis <= 0) {
                onPhaseComplete()
            }
        }
    }

    private fun onPhaseComplete() {
        val state = _pomodoroState.value
        appContext?.let { TimerService.stop(it) }

        when (state.phase) {
            PomodoroPhase.WORK -> {
                // Increment pomodoro count - don't reset here, let indicators fill up
                _pomodoroState.value = state.copy(
                    phase = PomodoroPhase.IDLE,
                    completedPomodoros = state.completedPomodoros + 1,
                    currentPomodoroInCycle = state.currentPomodoroInCycle + 1,
                    remainingMillis = 0L,
                    sessionCompletedAt = System.currentTimeMillis()
                )
            }
            PomodoroPhase.SHORT_BREAK -> {
                _pomodoroState.value = state.copy(
                    phase = PomodoroPhase.IDLE,
                    remainingMillis = 0L,
                    sessionCompletedAt = System.currentTimeMillis()
                )
            }
            PomodoroPhase.LONG_BREAK -> {
                // Reset cycle after long break is completed
                _pomodoroState.value = state.copy(
                    phase = PomodoroPhase.IDLE,
                    currentPomodoroInCycle = 0,
                    remainingMillis = 0L,
                    sessionCompletedAt = System.currentTimeMillis()
                )
            }
            else -> {}
        }
    }

    fun pause() {
        timerJob?.cancel()
        currentPhaseForResume = _pomodoroState.value.phase
        _pomodoroState.value = _pomodoroState.value.copy(phase = PomodoroPhase.PAUSED)
        appContext?.let { TimerService.pause(it) }
    }

    fun resume() {
        // Recalculate end time based on remaining millis
        endTimeMillis = System.currentTimeMillis() + _pomodoroState.value.remainingMillis
        _pomodoroState.value = _pomodoroState.value.copy(phase = currentPhaseForResume)
        appContext?.let { TimerService.resume(it) }
        startCountdown()
    }

    fun reset() {
        timerJob?.cancel()
        _pomodoroState.value = PomodoroState()
        appContext?.let { TimerService.stop(it) }
    }

    fun skipPhase() {
        timerJob?.cancel()
        onPhaseComplete()
    }

    fun updateSettings(settings: PomodoroSettings) {
        _pomodoroState.value = _pomodoroState.value.copy(settings = settings)
    }

    fun syncFromBackground() {
        // Called when app comes back to foreground - recalculate remaining time
        if (_pomodoroState.value.isRunning && endTimeMillis > 0) {
            val remaining = endTimeMillis - System.currentTimeMillis()
            if (remaining <= 0) {
                onPhaseComplete()
            } else {
                _pomodoroState.value = _pomodoroState.value.copy(
                    remainingMillis = remaining
                )
            }
        }
    }

    // Todo pool management
    fun addTodoToPool(text: String) {
        val state = _pomodoroState.value
        if (state.todoPool.size < 15 && text.isNotBlank()) {
            val newTodo = PomodoroTodo(text = text.trim())
            _pomodoroState.value = state.copy(
                todoPool = state.todoPool + newTodo
            )
        }
    }

    fun removeTodoFromPool(id: String) {
        val state = _pomodoroState.value
        _pomodoroState.value = state.copy(
            todoPool = state.todoPool.filter { it.id != id },
            selectedTodoIds = state.selectedTodoIds - id
        )
    }

    fun toggleTodoSelection(id: String) {
        val state = _pomodoroState.value
        val newSelection = if (id in state.selectedTodoIds) {
            state.selectedTodoIds - id
        } else if (state.selectedTodoIds.size < 3) {
            state.selectedTodoIds + id
        } else {
            state.selectedTodoIds  // Max 3 selected
        }
        _pomodoroState.value = state.copy(selectedTodoIds = newSelection)
    }

    fun toggleTodo(id: String) {
        val state = _pomodoroState.value
        val newCompletedState = state.todos.find { it.id == id }?.isCompleted?.not() ?: return

        _pomodoroState.value = state.copy(
            // Update active todos
            todos = state.todos.map { todo ->
                if (todo.id == id) todo.copy(isCompleted = newCompletedState)
                else todo
            },
            // Sync completion status back to pool
            todoPool = state.todoPool.map { todo ->
                if (todo.id == id) todo.copy(isCompleted = newCompletedState)
                else todo
            }
        )
    }

    fun toggleTodoPoolCompletion(id: String) {
        val state = _pomodoroState.value
        _pomodoroState.value = state.copy(
            todoPool = state.todoPool.map { todo ->
                if (todo.id == id) todo.copy(isCompleted = !todo.isCompleted)
                else todo
            }
        )
    }

    private fun activateSelectedTodos() {
        val state = _pomodoroState.value
        val selectedTodos = state.todoPool
            .filter { it.id in state.selectedTodoIds }
            .map { it.copy(isCompleted = false) }  // Reset completion status
        _pomodoroState.value = state.copy(todos = selectedTodos)
    }

    fun clearTodos() {
        _pomodoroState.value = _pomodoroState.value.copy(
            todos = emptyList(),
            selectedTodoIds = emptySet()
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
