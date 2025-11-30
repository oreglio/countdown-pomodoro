# PRP: Pomodoro Feature with E-Ink Optimized Landscape Display

## Executive Summary
Add a Pomodoro timer feature to the existing countdown app with:
1. Tab-based navigation (Countdown Timer | Pomodoro)
2. E-ink optimized display with high contrast monochrome theme
3. Landscape mode with large, elegant typography showing current time (24h)
4. Beautiful UX designed for e-ink devices (minimal animations, high contrast)

## Current State Analysis

### Project Structure
- **Project Type**: Android app with Jetpack Compose
- **Key Directories**:
  - `app/src/main/java/com/countdownhour/` - Main source code
  - `app/src/main/java/com/countdownhour/ui/components/` - UI components
  - `app/src/main/java/com/countdownhour/ui/screens/` - Screen composables
  - `app/src/main/java/com/countdownhour/ui/theme/` - Theming
  - `app/src/main/java/com/countdownhour/data/` - Data models
  - `app/src/main/java/com/countdownhour/viewmodel/` - ViewModels
- **Configuration Files**:
  - `app/build.gradle.kts`
  - `gradle/libs.versions.toml`

### Existing Files to Modify
- `MainActivity.kt` - Add navigation structure
- `Theme.kt` - Add e-ink optimized theme
- `libs.versions.toml` - Add navigation dependency

### New Files Required
- `data/PomodoroState.kt` - Pomodoro state model
- `viewmodel/PomodoroViewModel.kt` - Pomodoro logic
- `ui/screens/PomodoroScreen.kt` - Pomodoro UI (portrait)
- `ui/screens/PomodoroLandscapeScreen.kt` - Landscape display
- `ui/screens/MainScreen.kt` - Tab container
- `ui/components/PomodoroProgress.kt` - Pomodoro circular progress
- `ui/theme/EinkTheme.kt` - E-ink specific theming
- `ui/navigation/AppNavigation.kt` - Navigation setup

### Dependencies to Add
```toml
navigation-compose = "2.7.7"
```

### Build System
- **Build Tool**: Gradle with Kotlin DSL
- **Compose BOM**: 2024.02.00
- **Kotlin**: 2.0.0

## Research Findings

### Official Documentation
Based on [Android Developers - Navigation Bar](https://developer.android.com/develop/ui/compose/components/navigation-bar):
- Use `NavigationBar` with `NavigationBarItem` for bottom tabs
- Use `Scaffold` to structure the layout
- Manage selected state with `rememberSaveable`

Based on [Android Developers - Work with fonts](https://developer.android.com/develop/ui/compose/text/fonts):
- Use `FontFamily.Monospace` for timer displays
- Use `fontFeatureSettings = "tnum"` for tabular (monospace) numbers
- Define custom typography in Theme

### Community Resources - E-Ink Optimization
Based on [Android Police - E Ink Friendly Apps](https://www.androidpolice.com/top-e-ink-friendly-apps-for-android-e-reader/):
- Use high contrast black/white colors
- Disable animations
- Use simple, clear icons
- Avoid gradients and shadows

Based on [Stack Overflow - E-Ink Display](https://stackoverflow.com/questions/1238708/android-with-e-ink-display):
- Minimize screen refreshes
- Use static UI elements
- Avoid continuous animations

### Pomodoro Technique
Based on [Wikipedia - Pomodoro Technique](https://en.wikipedia.org/wiki/Pomodoro_Technique):
- **Work interval**: 25 minutes (1 Pomodoro)
- **Short break**: 5 minutes
- **Long break**: 15-30 minutes (after 4 Pomodoros)
- **Cycles**: Track completed Pomodoros

Based on [Todoist - Pomodoro Guide](https://www.todoist.com/productivity-methods/pomodoro-technique):
- Pomodoros are indivisible
- Customizable intervals (25-50 min work, 5-15 min break)
- Visual progress tracking

## Implementation Blueprint

### Prerequisites
- [ ] Existing countdown app is functional
- [ ] Android Studio is set up
- [ ] Navigation Compose dependency added

### Step-by-Step Implementation

#### Step 1: Add Navigation Dependency
**File: `gradle/libs.versions.toml`**
```toml
[versions]
# ... existing versions ...
navigation-compose = "2.7.7"

[libraries]
# ... existing libraries ...
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
```

**File: `app/build.gradle.kts`**
```kotlin
dependencies {
    // ... existing dependencies ...
    implementation(libs.navigation.compose)
}
```

- **Expected outcome**: Navigation Compose available for use

#### Step 2: Create Pomodoro State Model
**File: `app/src/main/java/com/countdownhour/data/PomodoroState.kt`**
```kotlin
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
    val settings: PomodoroSettings = PomodoroSettings()
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
```

- **Expected outcome**: Data model for Pomodoro timer state

#### Step 3: Create Pomodoro ViewModel
**File: `app/src/main/java/com/countdownhour/viewmodel/PomodoroViewModel.kt`**
```kotlin
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
```

- **Expected outcome**: ViewModel with full Pomodoro logic

#### Step 4: Create E-Ink Optimized Theme
**File: `app/src/main/java/com/countdownhour/ui/theme/Theme.kt`** (Update)
```kotlin
package com.countdownhour.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// E-Ink optimized colors - pure black and white for maximum contrast
private val EinkColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color.Black,
    onSecondary = Color.White,
    tertiary = Color.Black,
    onTertiary = Color.White,
    error = Color.Black,
    onError = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
    outlineVariant = Color(0xFF808080)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFFFFCC80),
    tertiary = Color(0xFFA5D6A7),
    error = Color(0xFFEF9A9A),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFFFF9800),
    tertiary = Color(0xFF4CAF50),
    error = Color(0xFFF44336),
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

// Large monospace typography for timer displays
val TimerTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Light,
        fontSize = 96.sp,
        lineHeight = 112.sp,
        letterSpacing = (-1.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Light,
        fontSize = 72.sp,
        lineHeight = 84.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun CountdownHourTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    einkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        einkMode -> EinkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (einkMode) TimerTypography else TimerTypography,
        content = content
    )
}
```

- **Expected outcome**: E-ink optimized theme with high contrast and monospace typography

#### Step 5: Create Pomodoro Progress Component
**File: `app/src/main/java/com/countdownhour/ui/components/PomodoroProgress.kt`**
```kotlin
package com.countdownhour.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.countdownhour.data.PomodoroPhase

@Composable
fun PomodoroProgress(
    progress: Float,
    phase: PomodoroPhase,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    strokeWidth: Dp = 16.dp,
    content: @Composable () -> Unit = {}
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val arcSize = Size(
                width = size.toPx() - strokeWidthPx,
                height = size.toPx() - strokeWidthPx
            )
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

            // Background circle
            drawArc(
                color = backgroundColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Progress arc (no animation for e-ink)
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        content()
    }
}

@Composable
fun PomodoroIndicators(
    completedPomodoros: Int,
    currentInCycle: Int,
    totalInCycle: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalInCycle) { index ->
            val isCompleted = index < currentInCycle
            val isCurrent = index == currentInCycle

            Box(
                modifier = Modifier
                    .size(if (isCurrent) 16.dp else 12.dp)
                    .padding(horizontal = 4.dp)
            ) {
                Canvas(modifier = Modifier.size(if (isCurrent) 16.dp else 12.dp)) {
                    drawCircle(
                        color = if (isCompleted) Color.Black else Color.LightGray,
                        style = if (isCompleted) {
                            Stroke(width = 0f) // Filled
                        } else {
                            Stroke(width = 2.dp.toPx())
                        }
                    )
                    if (isCompleted) {
                        drawCircle(color = Color.Black)
                    }
                }
            }

            if (index < totalInCycle - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}
```

- **Expected outcome**: E-ink optimized circular progress (no animations)

#### Step 6: Create Pomodoro Screen
**File: `app/src/main/java/com/countdownhour/ui/screens/PomodoroScreen.kt`**
```kotlin
package com.countdownhour.ui.screens

import android.content.res.Configuration
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.countdownhour.data.PomodoroPhase
import com.countdownhour.ui.components.PomodoroIndicators
import com.countdownhour.ui.components.PomodoroProgress
import com.countdownhour.viewmodel.PomodoroViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = viewModel()
) {
    val state by viewModel.pomodoroState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        PomodoroLandscapeLayout(
            state = state,
            onStartWork = { viewModel.startWork() },
            onStartBreak = { viewModel.startBreak() },
            onPause = { viewModel.pause() },
            onResume = { viewModel.resume() },
            onReset = { viewModel.reset() },
            onSkip = { viewModel.skipPhase() }
        )
    } else {
        PomodoroPortraitLayout(
            state = state,
            onStartWork = { viewModel.startWork() },
            onStartBreak = { viewModel.startBreak() },
            onPause = { viewModel.pause() },
            onResume = { viewModel.resume() },
            onReset = { viewModel.reset() },
            onSkip = { viewModel.skipPhase() }
        )
    }
}

@Composable
private fun PomodoroPortraitLayout(
    state: com.countdownhour.data.PomodoroState,
    onStartWork: () -> Unit,
    onStartBreak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Current time display (24h)
        CurrentTimeDisplay()

        Spacer(modifier = Modifier.height(24.dp))

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
            onStartWork = onStartWork,
            onStartBreak = onStartBreak,
            onPause = onPause,
            onResume = onResume,
            onReset = onReset,
            onSkip = onSkip
        )
    }
}

@Composable
private fun PomodoroLandscapeLayout(
    state: com.countdownhour.data.PomodoroState,
    onStartWork: () -> Unit,
    onStartBreak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side - Large timer display
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Current time (24h format) - smaller in landscape
            CurrentTimeDisplay(fontSize = 32)

            Spacer(modifier = Modifier.height(16.dp))

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
            // Compact progress circle
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

            Text(
                text = "Total: ${state.completedPomodoros}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Compact control buttons for landscape
            PomodoroControlButtons(
                phase = state.phase,
                onStartWork = onStartWork,
                onStartBreak = onStartBreak,
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
private fun CurrentTimeDisplay(fontSize: Int = 24) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Update every second (minimal for e-ink)
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Text(
        text = timeFormat.format(Date(currentTime)),
        fontSize = fontSize.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PomodoroControlButtons(
    phase: PomodoroPhase,
    onStartWork: () -> Unit,
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(
                    onClick = onStartWork,
                    modifier = Modifier.fillMaxWidth(if (compact) 0.8f else 0.6f)
                ) {
                    Text("Start Focus", style = MaterialTheme.typography.labelLarge)
                }

                if (!compact) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onStartBreak,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text("Take Break", style = MaterialTheme.typography.labelLarge)
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
```

- **Expected outcome**: Pomodoro screen with portrait and landscape layouts

#### Step 7: Create Main Screen with Tab Navigation
**File: `app/src/main/java/com/countdownhour/ui/screens/MainScreen.kt`**
```kotlin
package com.countdownhour.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Tab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Countdown : Tab(
        title = "Countdown",
        selectedIcon = Icons.Filled.Schedule,
        unselectedIcon = Icons.Outlined.Schedule
    )

    data object Pomodoro : Tab(
        title = "Pomodoro",
        selectedIcon = Icons.Filled.Timer,
        unselectedIcon = Icons.Outlined.Timer
    )
}

@Composable
fun MainScreen() {
    val tabs = listOf(Tab.Countdown, Tab.Pomodoro)
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTabIndex == index) {
                                    tab.selectedIcon
                                } else {
                                    tab.unselectedIcon
                                },
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTabIndex) {
                0 -> TimerScreen()
                1 -> PomodoroScreen()
            }
        }
    }
}
```

- **Expected outcome**: Main screen with tab navigation

#### Step 8: Update MainActivity
**File: `app/src/main/java/com/countdownhour/MainActivity.kt`**
```kotlin
package com.countdownhour

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.countdownhour.ui.screens.MainScreen
import com.countdownhour.ui.theme.CountdownHourTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CountdownHourTheme(einkMode = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}
```

- **Expected outcome**: App launches with tab navigation and e-ink mode

### Validation Checklist
- [ ] App builds without errors: `./gradlew build`
- [ ] Tab navigation works between Countdown and Pomodoro
- [ ] Pomodoro timer starts/pauses/resumes correctly
- [ ] Work intervals are 25 minutes by default
- [ ] Short breaks are 5 minutes
- [ ] Long breaks occur after 4 Pomodoros (15 minutes)
- [ ] Pomodoro count increments after work sessions
- [ ] Landscape mode shows large timer display
- [ ] Current time displays in 24h format
- [ ] E-ink theme has high contrast (black/white)
- [ ] No unnecessary animations
- [ ] Portrait and landscape orientations work

## Risk Assessment

### High Risk
- **None identified** - Using stable Compose APIs

### Medium Risk
- **Tab state loss on rotation** → Mitigation: Using `rememberSaveable` for tab state
- **Timer drift in background** → Mitigation: Acceptable for MVP, consider WorkManager for v2

### Low Risk
- **Navigation dependency version** → Mitigation: Using stable 2.7.7 version
- **E-ink specific optimizations** → Mitigation: Manual testing on e-ink device recommended

### Rollback Plan
1. Revert MainActivity to use TimerScreen directly
2. Remove new files if issues persist
3. Git commit before implementation to enable easy rollback

## Quality Assessment

### Context Completeness Score: 9/10
- Project analysis: 10/10 (Full codebase review completed)
- Documentation coverage: 9/10 (Official docs + community best practices)
- Implementation detail: 9/10 (Complete code for all components)

### Confidence Score: 9/10
**Rationale:**
- Using stable Jetpack Compose navigation APIs
- Pomodoro logic is straightforward state machine
- E-ink optimization is primarily color/animation choices
- Tab navigation pattern is well-documented
- Existing project architecture supports new features

### Missing Information
- Actual e-ink device testing (simulated with high contrast theme)
- User preference for Pomodoro durations (using defaults)
- Sound/vibration alerts (not implemented - poor for e-ink UX)

## Next Steps
1. Add navigation dependency to version catalog
2. Create all new Kotlin files
3. Update existing Theme.kt
4. Update MainActivity.kt
5. Build and test on emulator
6. Test on actual e-ink device if available

## File Structure After Implementation
```
app/src/main/java/com/countdownhour/
├── MainActivity.kt (updated)
├── data/
│   ├── TimerState.kt (existing)
│   └── PomodoroState.kt (new)
├── viewmodel/
│   ├── TimerViewModel.kt (existing)
│   └── PomodoroViewModel.kt (new)
└── ui/
    ├── components/
    │   ├── CircularProgress.kt (existing)
    │   ├── ControlButtons.kt (existing)
    │   ├── TimePickerDialog.kt (existing)
    │   └── PomodoroProgress.kt (new)
    ├── screens/
    │   ├── TimerScreen.kt (existing)
    │   ├── PomodoroScreen.kt (new)
    │   └── MainScreen.kt (new)
    └── theme/
        └── Theme.kt (updated)
```
