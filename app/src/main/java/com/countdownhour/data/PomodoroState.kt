package com.countdownhour.data

enum class PomodoroPhase {
    IDLE,           // Not started
    WORK,           // 25 min work session
    SHORT_BREAK,    // 5 min break
    LONG_BREAK,     // 15-30 min break after 4 pomodoros
    PAUSED          // Timer paused
}

data class PomodoroSettings(
    val workDurationMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val pomodorosUntilLongBreak: Int = 4
)

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.IDLE,
    val remainingMillis: Long = 0L,
    val totalMillis: Long = 0L,
    val completedPomodoros: Int = 0,
    val currentPomodoroInCycle: Int = 0,
    val settings: PomodoroSettings = PomodoroSettings(),
    val sessionCompletedAt: Long? = null  // Timestamp when session finished (for idle timer)
) {
    val remainingMinutes: Int
        get() = (remainingMillis / 60000).toInt()

    val remainingSeconds: Int
        get() = ((remainingMillis % 60000) / 1000).toInt()

    val progress: Float
        get() = if (totalMillis > 0) remainingMillis.toFloat() / totalMillis else 0f

    val isRunning: Boolean
        get() = phase == PomodoroPhase.WORK || phase == PomodoroPhase.SHORT_BREAK || phase == PomodoroPhase.LONG_BREAK

    val phaseLabel: String
        get() = when (phase) {
            PomodoroPhase.IDLE -> "Ready"
            PomodoroPhase.WORK -> "Focus"
            PomodoroPhase.SHORT_BREAK -> "Short Break"
            PomodoroPhase.LONG_BREAK -> "Long Break"
            PomodoroPhase.PAUSED -> "Paused"
        }
}
