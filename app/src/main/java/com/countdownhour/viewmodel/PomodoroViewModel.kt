package com.countdownhour.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.countdownhour.data.PomodoroDataStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PomodoroViewModel : ViewModel() {

    private val _pomodoroState = MutableStateFlow(PomodoroState())
    val pomodoroState: StateFlow<PomodoroState> = _pomodoroState.asStateFlow()

    private var timerJob: Job? = null
    private var endTimeMillis: Long = 0L
    private var currentPhaseForResume: PomodoroPhase = PomodoroPhase.IDLE
    private var appContext: Context? = null
    private var dataStore: PomodoroDataStore? = null
    private var isInitialized = false

    fun setContext(context: Context) {
        appContext = context.applicationContext
        if (!isInitialized) {
            dataStore = PomodoroDataStore(context.applicationContext)
            loadPersistedData()
            isInitialized = true
        }
    }

    private fun loadPersistedData() {
        viewModelScope.launch {
            dataStore?.let { store ->
                val settings = store.settingsFlow.first()
                val todoPool = store.todoPoolFlow.first()
                val selectedIds = store.selectedTodoIdsFlow.first()
                val completedPomodoros = store.completedPomodorosFlow.first()
                val currentCycle = store.currentCycleFlow.first()

                _pomodoroState.value = _pomodoroState.value.copy(
                    settings = settings,
                    todoPool = todoPool,
                    selectedTodoIds = selectedIds,
                    completedPomodoros = completedPomodoros,
                    currentPomodoroInCycle = currentCycle
                )
            }
        }
    }

    private fun persistData() {
        viewModelScope.launch {
            dataStore?.let { store ->
                val state = _pomodoroState.value
                store.saveSettings(state.settings)
                store.saveTodoPool(state.todoPool)
                store.saveSelectedTodoIds(state.selectedTodoIds)
                store.saveCompletedPomodoros(state.completedPomodoros)
                store.saveCurrentCycle(state.currentPomodoroInCycle)
            }
        }
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
                persistData()  // Save completed pomodoros
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
                persistData()  // Save cycle reset
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

    fun addTime(minutes: Int) {
        val state = _pomodoroState.value
        // Prevent going below 1 minute
        val minRemaining = 60 * 1000L
        val addedMillis = minutes * 60 * 1000L
        val newRemaining = (state.remainingMillis + addedMillis).coerceAtLeast(minRemaining)
        val actualAdded = newRemaining - state.remainingMillis

        if (actualAdded != 0L) {
            endTimeMillis += actualAdded
            _pomodoroState.value = state.copy(
                remainingMillis = newRemaining,
                totalMillis = state.totalMillis + actualAdded
            )
            appContext?.let { TimerService.addTime(it, actualAdded) }
        }
    }

    fun reset() {
        timerJob?.cancel()
        val state = _pomodoroState.value
        // Preserve todoPool and selectedTodoIds when resetting
        _pomodoroState.value = PomodoroState(
            todoPool = state.todoPool,
            selectedTodoIds = state.selectedTodoIds,
            settings = state.settings
        )
        appContext?.let { TimerService.stop(it) }
        persistData()
    }

    fun clearAllTodos() {
        _pomodoroState.value = _pomodoroState.value.copy(
            todoPool = emptyList(),
            selectedTodoIds = emptySet(),
            todos = emptyList()
        )
        persistData()
    }

    fun clearCompletedTodos() {
        val state = _pomodoroState.value
        val completedIds = state.todoPool.filter { it.isCompleted }.map { it.id }.toSet()
        _pomodoroState.value = state.copy(
            todoPool = state.todoPool.filter { !it.isCompleted },
            selectedTodoIds = state.selectedTodoIds - completedIds,
            todos = state.todos.filter { !it.isCompleted }
        )
        persistData()
    }

    fun restoreTodos(todos: List<PomodoroTodo>, selectedIds: Set<String>) {
        _pomodoroState.value = _pomodoroState.value.copy(
            todoPool = todos,
            selectedTodoIds = selectedIds
        )
        persistData()
    }

    fun skipPhase() {
        timerJob?.cancel()
        onPhaseComplete()
    }

    fun updateSettings(settings: PomodoroSettings) {
        _pomodoroState.value = _pomodoroState.value.copy(settings = settings)
        persistData()
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
        if (state.todoPool.size < 50 && text.isNotBlank()) {
            val newTodo = PomodoroTodo(text = text.trim())
            _pomodoroState.value = state.copy(
                todoPool = state.todoPool + newTodo
            )
            persistData()
        }
    }

    fun removeTodoFromPool(id: String) {
        val state = _pomodoroState.value
        _pomodoroState.value = state.copy(
            todoPool = state.todoPool.filter { it.id != id },
            selectedTodoIds = state.selectedTodoIds - id
        )
        persistData()
    }

    fun updateTodoText(id: String, newText: String) {
        if (newText.isBlank()) return
        val state = _pomodoroState.value
        _pomodoroState.value = state.copy(
            todoPool = state.todoPool.map { todo ->
                if (todo.id == id) todo.copy(text = newText.trim())
                else todo
            },
            todos = state.todos.map { todo ->
                if (todo.id == id) todo.copy(text = newText.trim())
                else todo
            }
        )
        persistData()
    }

    fun toggleTodoSelection(id: String) {
        val state = _pomodoroState.value
        val todo = state.todoPool.find { it.id == id } ?: return

        // Don't allow selecting completed tasks
        if (todo.isCompleted && id !in state.selectedTodoIds) {
            return
        }

        val newSelection = if (id in state.selectedTodoIds) {
            state.selectedTodoIds - id
        } else if (state.selectedTodoIds.size < 5) {
            state.selectedTodoIds + id
        } else {
            state.selectedTodoIds  // Max 5 selected
        }
        _pomodoroState.value = state.copy(selectedTodoIds = newSelection)
        persistData()
    }

    fun toggleTodo(id: String) {
        val state = _pomodoroState.value
        val todo = state.todos.find { it.id == id } ?: return
        val isNowCompleted = !todo.isCompleted
        val completedAt = if (isNowCompleted) System.currentTimeMillis() else null

        _pomodoroState.value = state.copy(
            // Update active todos
            todos = state.todos.map { t ->
                if (t.id == id) t.copy(isCompleted = isNowCompleted, completedAt = completedAt)
                else t
            },
            // Sync completion status back to pool
            todoPool = state.todoPool.map { t ->
                if (t.id == id) t.copy(isCompleted = isNowCompleted, completedAt = completedAt)
                else t
            },
            // Unselect if completed
            selectedTodoIds = if (isNowCompleted) state.selectedTodoIds - id else state.selectedTodoIds
        )
        persistData()
    }

    fun toggleTodoPoolCompletion(id: String) {
        val state = _pomodoroState.value
        val todo = state.todoPool.find { it.id == id } ?: return
        val isNowCompleted = !todo.isCompleted
        val completedAt = if (isNowCompleted) System.currentTimeMillis() else null

        _pomodoroState.value = state.copy(
            todoPool = state.todoPool.map { t ->
                if (t.id == id) t.copy(isCompleted = isNowCompleted, completedAt = completedAt)
                else t
            },
            // Sync with active todos if present
            todos = state.todos.map { t ->
                if (t.id == id) t.copy(isCompleted = isNowCompleted, completedAt = completedAt)
                else t
            },
            // Unselect if completed
            selectedTodoIds = if (isNowCompleted) state.selectedTodoIds - id else state.selectedTodoIds
        )
        persistData()
    }

    private fun activateSelectedTodos() {
        val state = _pomodoroState.value
        val selectedTodos = state.todoPool
            .filter { it.id in state.selectedTodoIds }
            .map { it.copy(isCompleted = false) }  // Reset completion status
        _pomodoroState.value = state.copy(todos = selectedTodos)
    }

    fun refreshActiveTodos() {
        val state = _pomodoroState.value
        // Only refresh if in a running session
        if (state.phase == PomodoroPhase.WORK || state.phase == PomodoroPhase.PAUSED) {
            val selectedTodos = state.todoPool
                .filter { it.id in state.selectedTodoIds }
                .map { todo ->
                    // Preserve completion status from current active todos
                    val existingTodo = state.todos.find { it.id == todo.id }
                    existingTodo ?: todo.copy(isCompleted = false)
                }
            _pomodoroState.value = state.copy(todos = selectedTodos)
        }
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
