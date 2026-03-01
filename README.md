# remnd

A clean, modern Android reminders & to-do app built with Kotlin and Jetpack Compose.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM |
| Database | Room |
| DI | Hilt |
| Navigation | Navigation Compose |
| Async | Kotlin Coroutines + Flow |

## Project Structure

```
app/src/main/java/com/remnd/
├── RemndApplication.kt       # Application class, notification channel setup
├── MainActivity.kt           # Entry point activity
├── data/
│   ├── Reminder.kt           # Room Entity
│   ├── ReminderDao.kt        # Database access object
│   ├── ReminderDatabase.kt   # Room database definition
│   └── ReminderRepository.kt # Single source of truth
├── di/
│   └── DatabaseModule.kt     # Hilt DI module for Room
├── receiver/
│   ├── AlarmReceiver.kt      # Fires notifications at scheduled time
│   └── BootReceiver.kt       # Reschedules alarms after reboot
├── viewmodel/
│   └── ReminderViewModel.kt  # UI state management
└── ui/
    ├── RemndApp.kt           # Navigation host
    ├── theme/                # Material 3 theming
    ├── screens/
    │   ├── HomeScreen.kt         # Reminder list with filters & search
    │   └── AddEditReminderScreen.kt  # Create / edit reminders
    └── components/
        └── ReminderItem.kt   # Individual reminder card
```

## Features

- **Add / edit / delete** reminders
- **Priority levels** — Low, Medium, High
- **Due date & alarm** scheduling via AlarmManager
- **Filter** by All / Active / Completed
- **Search** reminders by title or description
- **Clear completed** reminders in bulk
- **Boot-safe** — alarms are rescheduled after device restart
- **Dark mode** support + Material You dynamic colours

## Getting Started

1. Open the project in **Android Studio Hedgehog** (or newer)
2. Let Gradle sync
3. Run on a device or emulator with **API 26+**

## Next Steps

- [ ] Time picker in Add/Edit screen (currently defaults to 9 AM)
- [ ] Repeat / recurring reminders
- [ ] Widgets (home screen)
- [ ] Backup & restore
- [ ] Categories / labels
