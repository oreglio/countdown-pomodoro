package com.countdownhour.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.countdownhour.data.PomodoroPhase
import com.countdownhour.data.PomodoroSettings
import com.countdownhour.data.PomodoroState
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

    fun startWork() {
        val settings = _pomodoroState.value.settings
        val totalMillis = settings.workDurationMinutes * 60 * 1000L

        _pomodoroState.value = _pomodoroState.value.copy(
            phase = PomodoroPhase.WORK,
            totalMillis = totalMillis,
            remainingMillis = totalMillis
        )

        startCountdown()
    }

    fun startBreak() {
        val state = _pomodoroState.value
        val settings = state.settings
        val isLongBreak = (state.currentPomodoroInCycle + 1) >= settings.pomodorosUntilLongBreak

        val breakDuration = if (isLongBreak) {
            settings.longBreakMinutes
        } else {
            settings.shortBreakMinutes
        }

        val totalMillis = breakDuration * 60 * 1000L
        val newPhase = if (isLongBreak) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK

        _pomodoroState.value = state.copy(
            phase = newPhase,
            totalMillis = totalMillis,
            remainingMillis = totalMillis
        )

        startCountdown()
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_pomodoroState.value.remainingMillis > 0 &&
                   _pomodoroState.value.isRunning) {
                delay(1000L)
                _pomodoroState.value = _pomodoroState.value.copy(
                    remainingMillis = (_pomodoroState.value.remainingMillis - 1000).coerceAtLeast(0)
                )
            }

            if (_pomodoroState.value.remainingMillis <= 0) {
                onPhaseComplete()
            }
        }
    }

    private fun onPhaseComplete() {
        val state = _pomodoroState.value
        when (state.phase) {
            PomodoroPhase.WORK -> {
                val newPomodoroCount = state.currentPomodoroInCycle + 1
                val isLongBreakNext = newPomodoroCount >= state.settings.pomodorosUntilLongBreak

                _pomodoroState.value = state.copy(
                    phase = PomodoroPhase.IDLE,
                    completedPomodoros = state.completedPomodoros + 1,
                    currentPomodoroInCycle = if (isLongBreakNext) 0 else newPomodoroCount,
                    remainingMillis = 0L
                )
            }
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> {
                _pomodoroState.value = state.copy(
                    phase = PomodoroPhase.IDLE,
                    remainingMillis = 0L
                )
            }
            else -> {}
        }
    }

    fun pause() {
        timerJob?.cancel()
        _pomodoroState.value = _pomodoroState.value.copy(phase = PomodoroPhase.PAUSED)
    }

    fun resume() {
        val previousPhase = when {
            _pomodoroState.value.totalMillis == _pomodoroState.value.settings.workDurationMinutes * 60 * 1000L -> PomodoroPhase.WORK
            _pomodoroState.value.totalMillis == _pomodoroState.value.settings.shortBreakMinutes * 60 * 1000L -> PomodoroPhase.SHORT_BREAK
            else -> PomodoroPhase.LONG_BREAK
        }
        _pomodoroState.value = _pomodoroState.value.copy(phase = previousPhase)
        startCountdown()
    }

    fun reset() {
        timerJob?.cancel()
        _pomodoroState.value = PomodoroState()
    }

    fun skipPhase() {
        timerJob?.cancel()
        onPhaseComplete()
    }

    fun updateSettings(settings: PomodoroSettings) {
        _pomodoroState.value = _pomodoroState.value.copy(settings = settings)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
