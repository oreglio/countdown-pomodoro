package com.countdownhour.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.countdownhour.MainActivity
import com.countdownhour.R
import com.countdownhour.data.PomodoroPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Countdown timer state
    private val _countdownRemainingMillis = MutableStateFlow(0L)
    val countdownRemainingMillis: StateFlow<Long> = _countdownRemainingMillis.asStateFlow()

    private val _countdownRunning = MutableStateFlow(false)
    val countdownRunning: StateFlow<Boolean> = _countdownRunning.asStateFlow()

    private var countdownTargetTime: Long = 0L

    // Pomodoro timer state
    private val _pomodoroRemainingMillis = MutableStateFlow(0L)
    val pomodoroRemainingMillis: StateFlow<Long> = _pomodoroRemainingMillis.asStateFlow()

    private val _pomodoroRunning = MutableStateFlow(false)
    val pomodoroRunning: StateFlow<Boolean> = _pomodoroRunning.asStateFlow()

    private val _pomodoroPhase = MutableStateFlow(PomodoroPhase.IDLE)
    val pomodoroPhase: StateFlow<PomodoroPhase> = _pomodoroPhase.asStateFlow()

    private var pomodoroEndTime: Long = 0L

    // Callbacks for timer completion
    var onCountdownComplete: (() -> Unit)? = null
    var onPomodoroComplete: (() -> Unit)? = null

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_COUNTDOWN -> {
                val targetTime = intent.getLongExtra(EXTRA_TARGET_TIME, 0L)
                startCountdownTimer(targetTime)
            }
            ACTION_START_POMODORO -> {
                val durationMillis = intent.getLongExtra(EXTRA_DURATION, 0L)
                val phase = intent.getStringExtra(EXTRA_PHASE)?.let {
                    PomodoroPhase.valueOf(it)
                } ?: PomodoroPhase.WORK
                startPomodoroTimer(durationMillis, phase)
            }
            ACTION_PAUSE -> pauseTimers()
            ACTION_RESUME -> resumeTimers()
            ACTION_STOP -> stopTimers()
            ACTION_ADD_TIME -> {
                val addedMillis = intent.getLongExtra(EXTRA_ADDED_TIME, 0L)
                addTimeToPomodoro(addedMillis)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows timer progress"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CountdownHour::TimerWakeLock"
        ).apply {
            acquire(10 * 60 * 60 * 1000L) // 10 hours max
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun startCountdownTimer(targetTimeMillis: Long) {
        countdownTargetTime = targetTimeMillis
        val remaining = targetTimeMillis - System.currentTimeMillis()
        if (remaining <= 0) return

        _countdownRemainingMillis.value = remaining
        _countdownRunning.value = true

        startForeground(NOTIFICATION_ID, buildNotification("Countdown", formatTime(remaining)))
        startCountdownJob()
    }

    private fun startCountdownJob() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_countdownRunning.value && _countdownRemainingMillis.value > 0) {
                delay(1000L)
                val remaining = countdownTargetTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    _countdownRemainingMillis.value = 0
                    _countdownRunning.value = false
                    playCompletionSound()
                    onCountdownComplete?.invoke()
                    updateNotification("Countdown", "Finished!")
                } else {
                    _countdownRemainingMillis.value = remaining
                    updateNotification("Countdown", formatTime(remaining))
                }
            }
        }
    }

    fun startPomodoroTimer(durationMillis: Long, phase: PomodoroPhase) {
        pomodoroEndTime = System.currentTimeMillis() + durationMillis
        _pomodoroRemainingMillis.value = durationMillis
        _pomodoroRunning.value = true
        _pomodoroPhase.value = phase

        val phaseLabel = when (phase) {
            PomodoroPhase.WORK -> "Focus"
            PomodoroPhase.SHORT_BREAK -> "Short Break"
            PomodoroPhase.LONG_BREAK -> "Long Break"
            else -> "Pomodoro"
        }

        startForeground(NOTIFICATION_ID, buildNotification(phaseLabel, formatTime(durationMillis)))
        startPomodoroJob(phaseLabel)
    }

    private fun startPomodoroJob(phaseLabel: String) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_pomodoroRunning.value && _pomodoroRemainingMillis.value > 0) {
                delay(1000L)
                val remaining = pomodoroEndTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    val wasBreak = _pomodoroPhase.value == PomodoroPhase.SHORT_BREAK ||
                                   _pomodoroPhase.value == PomodoroPhase.LONG_BREAK
                    _pomodoroRemainingMillis.value = 0
                    _pomodoroRunning.value = false
                    _pomodoroPhase.value = PomodoroPhase.IDLE
                    playCompletionSound(isBreakEnd = wasBreak)
                    onPomodoroComplete?.invoke()
                    updateNotification(phaseLabel, "Finished!")
                } else {
                    _pomodoroRemainingMillis.value = remaining
                    updateNotification(phaseLabel, formatTime(remaining))
                }
            }
        }
    }

    private fun pauseTimers() {
        timerJob?.cancel()
        _countdownRunning.value = false
        _pomodoroRunning.value = false
        updateNotification("Timer", "Paused")
    }

    private fun resumeTimers() {
        if (_countdownRemainingMillis.value > 0) {
            countdownTargetTime = System.currentTimeMillis() + _countdownRemainingMillis.value
            _countdownRunning.value = true
            startCountdownJob()
        } else if (_pomodoroRemainingMillis.value > 0) {
            pomodoroEndTime = System.currentTimeMillis() + _pomodoroRemainingMillis.value
            _pomodoroRunning.value = true
            val phaseLabel = when (_pomodoroPhase.value) {
                PomodoroPhase.WORK -> "Focus"
                PomodoroPhase.SHORT_BREAK -> "Short Break"
                PomodoroPhase.LONG_BREAK -> "Long Break"
                else -> "Pomodoro"
            }
            startPomodoroJob(phaseLabel)
        }
    }

    private fun stopTimers() {
        timerJob?.cancel()
        _countdownRunning.value = false
        _countdownRemainingMillis.value = 0L
        _pomodoroRunning.value = false
        _pomodoroRemainingMillis.value = 0L
        _pomodoroPhase.value = PomodoroPhase.IDLE
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun addTimeToPomodoro(addedMillis: Long) {
        if (_pomodoroRunning.value && _pomodoroRemainingMillis.value > 0) {
            pomodoroEndTime += addedMillis
            _pomodoroRemainingMillis.value = _pomodoroRemainingMillis.value + addedMillis
            val phaseLabel = when (_pomodoroPhase.value) {
                PomodoroPhase.WORK -> "Focus"
                PomodoroPhase.SHORT_BREAK -> "Short Break"
                PomodoroPhase.LONG_BREAK -> "Long Break"
                else -> "Pomodoro"
            }
            updateNotification(phaseLabel, formatTime(_pomodoroRemainingMillis.value))
        }
    }

    fun syncCountdownState(remainingMillis: Long, isRunning: Boolean) {
        if (isRunning && remainingMillis > 0) {
            countdownTargetTime = System.currentTimeMillis() + remainingMillis
            _countdownRemainingMillis.value = remainingMillis
            _countdownRunning.value = true
            if (timerJob?.isActive != true) {
                startForeground(NOTIFICATION_ID, buildNotification("Countdown", formatTime(remainingMillis)))
                startCountdownJob()
            }
        }
    }

    fun syncPomodoroState(remainingMillis: Long, isRunning: Boolean, phase: PomodoroPhase) {
        if (isRunning && remainingMillis > 0) {
            pomodoroEndTime = System.currentTimeMillis() + remainingMillis
            _pomodoroRemainingMillis.value = remainingMillis
            _pomodoroRunning.value = true
            _pomodoroPhase.value = phase
            val phaseLabel = when (phase) {
                PomodoroPhase.WORK -> "Focus"
                PomodoroPhase.SHORT_BREAK -> "Short Break"
                PomodoroPhase.LONG_BREAK -> "Long Break"
                else -> "Pomodoro"
            }
            if (timerJob?.isActive != true) {
                startForeground(NOTIFICATION_ID, buildNotification(phaseLabel, formatTime(remainingMillis)))
                startPomodoroJob(phaseLabel)
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun playCompletionSound(isBreakEnd: Boolean = false) {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone?.play()

            if (isBreakEnd) {
                // Double sound for break end (time to focus!)
                serviceScope.launch {
                    delay(350L)
                    RingtoneManager.getRingtone(applicationContext, notification)?.play()
                }
            }
        } catch (e: Exception) {
            // Silently fail if sound can't be played
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        wakeLock?.release()
    }

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START_COUNTDOWN = "start_countdown"
        const val ACTION_START_POMODORO = "start_pomodoro"
        const val ACTION_PAUSE = "pause"
        const val ACTION_RESUME = "resume"
        const val ACTION_STOP = "stop"
        const val ACTION_ADD_TIME = "add_time"
        const val EXTRA_TARGET_TIME = "target_time"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_PHASE = "phase"
        const val EXTRA_ADDED_TIME = "added_time"

        fun startCountdown(context: Context, targetTimeMillis: Long) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START_COUNTDOWN
                putExtra(EXTRA_TARGET_TIME, targetTimeMillis)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startPomodoro(context: Context, durationMillis: Long, phase: PomodoroPhase) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START_POMODORO
                putExtra(EXTRA_DURATION, durationMillis)
                putExtra(EXTRA_PHASE, phase.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun addTime(context: Context, addedMillis: Long) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_ADD_TIME
                putExtra(EXTRA_ADDED_TIME, addedMillis)
            }
            context.startService(intent)
        }
    }
}
