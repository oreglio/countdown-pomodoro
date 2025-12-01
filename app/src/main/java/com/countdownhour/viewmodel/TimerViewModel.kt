package com.countdownhour.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.countdownhour.data.TimerState
import com.countdownhour.data.TimerStatus
import com.countdownhour.service.TimerService
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
    private var targetTimeMillis: Long = 0L
    private var appContext: Context? = null

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

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

        targetTimeMillis = target.timeInMillis
        val totalMillis = targetTimeMillis - now.timeInMillis

        _timerState.value = state.copy(
            totalMillis = totalMillis,
            remainingMillis = totalMillis,
            status = TimerStatus.RUNNING,
            finishedAt = null  // Reset elapsed timer
        )

        // Start foreground service
        appContext?.let { TimerService.startCountdown(it, targetTimeMillis) }

        startCountdown()
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingMillis > 0 &&
                   _timerState.value.status == TimerStatus.RUNNING) {
                delay(1000L)
                // Recalculate from target time for accuracy
                val remaining = targetTimeMillis - System.currentTimeMillis()
                _timerState.value = _timerState.value.copy(
                    remainingMillis = remaining.coerceAtLeast(0)
                )
            }
            if (_timerState.value.remainingMillis <= 0) {
                _timerState.value = _timerState.value.copy(
                    status = TimerStatus.FINISHED,
                    finishedAt = System.currentTimeMillis()
                )
                appContext?.let { TimerService.stop(it) }
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(status = TimerStatus.PAUSED)
        appContext?.let { TimerService.pause(it) }
    }

    fun resume() {
        // Recalculate target time based on remaining millis
        targetTimeMillis = System.currentTimeMillis() + _timerState.value.remainingMillis
        _timerState.value = _timerState.value.copy(status = TimerStatus.RUNNING)
        appContext?.let { TimerService.resume(it) }
        startCountdown()
    }

    fun stop() {
        timerJob?.cancel()
        _timerState.value = TimerState()
        appContext?.let { TimerService.stop(it) }
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
        targetTimeMillis = target.timeInMillis
        val totalMillis = targetTimeMillis - now.timeInMillis
        _timerState.value = state.copy(
            totalMillis = totalMillis,
            remainingMillis = totalMillis,
            status = TimerStatus.RUNNING,
            finishedAt = null  // Reset elapsed timer
        )
        appContext?.let { TimerService.startCountdown(it, targetTimeMillis) }
        startCountdown()
    }

    fun syncFromBackground() {
        // Called when app comes back to foreground - recalculate remaining time
        if (_timerState.value.status == TimerStatus.RUNNING && targetTimeMillis > 0) {
            val remaining = targetTimeMillis - System.currentTimeMillis()
            if (remaining <= 0) {
                _timerState.value = _timerState.value.copy(
                    remainingMillis = 0,
                    status = TimerStatus.FINISHED,
                    finishedAt = targetTimeMillis  // Use target time as finish time
                )
            } else {
                _timerState.value = _timerState.value.copy(
                    remainingMillis = remaining
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
