package com.countdownhour.data

enum class TimerStatus {
    IDLE,      // Timer not started
    RUNNING,   // Timer counting down
    PAUSED,    // Timer paused
    FINISHED   // Timer completed
}

data class TimerState(
    val targetHour: Int = -1,
    val targetMinute: Int = -1,
    val remainingMillis: Long = 0L,
    val totalMillis: Long = 0L,
    val status: TimerStatus = TimerStatus.IDLE,
    val finishedAt: Long? = null  // Timestamp when timer finished (for elapsed timer)
) {
    val hasTargetSet: Boolean
        get() = targetHour >= 0 && targetMinute >= 0

    val remainingHours: Int
        get() = (remainingMillis / 3600000).toInt()

    val remainingMinutes: Int
        get() = ((remainingMillis % 3600000) / 60000).toInt()

    val remainingSeconds: Int
        get() = ((remainingMillis % 60000) / 1000).toInt()

    val progress: Float
        get() = if (totalMillis > 0) remainingMillis.toFloat() / totalMillis else 0f
}
