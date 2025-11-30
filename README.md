# CountdownHour

A minimalist Android countdown timer app with Pomodoro functionality, optimized for e-ink displays.

## Features

### Countdown Timer
- Set a target time (24h format) and countdown until you reach it
- Visual circular progress indicator
- Real-time summary showing NOW → DIFF → TARGET
- Pause/Resume/Stop controls
- Portrait and landscape layouts with large, readable typography

### Pomodoro Timer
- Classic Pomodoro technique implementation
- Customizable durations:
  - Focus session (default: 25 min)
  - Short break (default: 5 min)
  - Long break (default: 15 min)
  - Pomodoros until long break (default: 4)
- Visual progress tracking with cycle indicators
- Completed pomodoros counter

### E-Ink Optimized
- High contrast black & white theme
- Large monospace typography for excellent readability
- Minimal animations to reduce screen flickering
- Clean, distraction-free interface

## Screenshots

| Countdown | Pomodoro |
|-----------|----------|
| Portrait & Landscape | Portrait & Landscape |

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with ViewModel and StateFlow
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)

## Building

```bash
# Clone the repository
git clone https://github.com/yourusername/countdownhour.git

# Build the project
./gradlew build

# Install on connected device
./gradlew installDebug
```

## Project Structure

```
app/src/main/java/com/countdownhour/
├── MainActivity.kt
├── data/
│   ├── TimerState.kt
│   └── PomodoroState.kt
├── viewmodel/
│   ├── TimerViewModel.kt
│   └── PomodoroViewModel.kt
└── ui/
    ├── components/
    │   ├── CircularProgress.kt
    │   ├── ControlButtons.kt
    │   ├── TimePickerDialog.kt
    │   ├── TimeSummaryCard.kt
    │   ├── PomodoroProgress.kt
    │   └── PomodoroSettingsDialog.kt
    ├── screens/
    │   ├── TimerScreen.kt
    │   ├── PomodoroScreen.kt
    │   └── MainScreen.kt
    └── theme/
        └── Theme.kt
```

## License

MIT License - feel free to use and modify as needed.
