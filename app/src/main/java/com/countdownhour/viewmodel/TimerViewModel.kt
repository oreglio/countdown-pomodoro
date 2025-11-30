package com.countdownhour.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.countdownhour.data.TimerState
import com.countdownhour.data.TimerStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class TimerViewModel : ViewModel() {

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    fun setTargetTime(hour: Int, minute: Int) {
        _timerState.value = _timerState.value.copy(
            targetHour = hour,
            targetMinute = minute
        )
    }

    fun start() {
        val state = _timerState.value
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, state.targetHour)
            set(Calendar.MINUTE, state.targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If target time is before now, set it for tomorrow
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val totalMillis = target.timeInMillis - now.timeInMillis

        _timerState.value = state.copy(
            totalMillis = totalMillis,
            remainingMillis = totalMillis,
            status = TimerStatus.RUNNING
        )

        startCountdown()
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingMillis > 0 &&
                   _timerState.value.status == TimerStatus.RUNNING) {
                delay(1000L)
                _timerState.value = _timerState.value.copy(
                    remainingMillis = (_timerState.value.remainingMillis - 1000).coerceAtLeast(0)
                )
            }
            if (_timerState.value.remainingMillis <= 0) {
                _timerState.value = _timerState.value.copy(status = TimerStatus.FINISHED)
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(status = TimerStatus.PAUSED)
    }

    fun resume() {
        _timerState.value = _timerState.value.copy(status = TimerStatus.RUNNING)
        startCountdown()
    }

    fun stop() {
        timerJob?.cancel()
        _timerState.value = TimerState()
    }

    fun reset() {
        // Reset recalculates time from now to target and restarts
        timerJob?.cancel()
        val state = _timerState.value
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, state.targetHour)
            set(Calendar.MINUTE, state.targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val totalMillis = target.timeInMillis - now.timeInMillis
        _timerState.value = state.copy(
            totalMillis = totalMillis,
            remainingMillis = totalMillis,
            status = TimerStatus.RUNNING
        )
        startCountdown()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
