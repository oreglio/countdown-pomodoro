# CountDown Hour and Pomodoro - Simple & Clean UI

A minimalist Android countdown timer app with Pomodoro functionality, optimized for e-ink displays.
<br/>
<br/>

<img width="206" height="412" alt="Countdown Hour_20251130-194014" src="https://github.com/user-attachments/assets/e81ff049-59f6-4862-9f7b-8a2f99894f5f" />
<img width="206" height="412" alt="Countdown Hour_20251130-194027" src="https://github.com/user-attachments/assets/a211fd2e-bc30-4547-9015-cf9c2d2476b3" />
<img width="206" height="412" alt="Countdown Hour_20251130-194035" src="https://github.com/user-attachments/assets/0f4cde23-cdbb-40ce-8603-061c5f8d09ff" />
<img width="206" height="412" alt="Countdown Hour_20251130-194047" src="https://github.com/user-attachments/assets/e1923bcd-d952-4cc0-b8bf-2a13df0c1a77" />
<img width="206" height="412" alt="Countdown Hour_20251130-194107" src="https://github.com/user-attachments/assets/3a693793-b10b-4275-b7c1-8aa4e2b354eb" />
<img width="206" height="412" alt="Countdown Hour_20251130-194116" src="https://github.com/user-attachments/assets/1c1b1b19-4ce5-4a17-9754-c71fb2e8162c" />
<img width="206" height="412" alt="Countdown Hour_20251130-194132" src="https://github.com/user-attachments/assets/b4426c6e-e27c-410e-84bc-0017808eb1c5" />
<img width="206" height="412" alt="Countdown Hour_20251130-194153" src="https://github.com/user-attachments/assets/ad839f6e-38ca-4892-a561-bb17e3fa181e" />
<img width="412" height="206" alt="Countdown Hour_20251130-194142" src="https://github.com/user-attachments/assets/eff24f8b-c85c-4848-8ba0-ece1f0a81fe4" />


## Features

### Countdown Timer
- Set a target time (24h format) and countdown until you reach it
- Custom time picker with swipe gestures (swipe up/down to adjust)
- Tap on hour/minute to fine-tune by ±1
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
- Elapsed time display after session completion
- Extend time during session:
  - Tap +5 button to add 5 minutes
  - Long press +5 button to add 1 minute
  - Swipe up/down on timer to add/remove minutes

### Task Management
- Task pool to store up to 15 tasks
- Select up to 5 tasks for each focus session
- Tasks displayed in subtle card during focus
- Tap to mark tasks as completed (strikethrough)
- Completed tasks auto-unselected and moved to "DONE" section
- Long press on tasks to toggle completion in pool
- Long press on Focus button to start without task selection
- Double tap trash icon to clear all tasks

### Data Persistence
- Tasks, settings, and progress saved automatically
- Data survives app restarts and updates
- Task timestamps (created/completed) tracked

### E-Ink Optimized
- High contrast black & white theme
- Large monospace typography for excellent readability
- Minimal animations to reduce screen flickering
- Clean, distraction-free interface

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with ViewModel and StateFlow
- **Persistence**: DataStore Preferences + Kotlin Serialization
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)

### Background Support
- Timer continues running when app is in background
- Notification shows remaining time
- Screen stays awake while app is active

## Building

```bash
# Clone the repository
git clone https://github.com/oreglio/countdown-pomodoro.git

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
│   ├── PomodoroState.kt
│   └── PomodoroDataStore.kt
├── service/
│   └── TimerService.kt
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

## AI
The whole code is generated with Claude Code. I just needed to have those features in a simple and clean app.

## License

MIT License - feel free to use and modify as needed.
