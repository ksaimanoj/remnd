# CLAUDE.md — remnd

This file provides guidance for AI assistants working on the **remnd** codebase.

## Project Overview

**remnd** is a modern Android reminders & to-do app built with Kotlin and Jetpack Compose. It supports scheduled alarms, priority levels, recurring reminders, filtering/search, and notification actions.

- **Language**: Kotlin 2.0.21
- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34 (Android 14)
- **Java Version**: JDK 17
- **Build System**: Gradle with Kotlin DSL + version catalog (`gradle/libs.versions.toml`)

---

## Repository Structure

```
remnd/
├── app/
│   ├── src/main/
│   │   ├── java/com/remnd/
│   │   │   ├── RemndApplication.kt          # App class; creates notification channel
│   │   │   ├── MainActivity.kt              # Entry activity; requests runtime permissions
│   │   │   ├── data/
│   │   │   │   ├── Reminder.kt              # Room @Entity; Priority + FrequencyType constants
│   │   │   │   ├── ReminderDao.kt           # Room DAO with Flow queries
│   │   │   │   ├── ReminderDatabase.kt      # Room DB (v2) with MIGRATION_1_2
│   │   │   │   ├── ReminderRepository.kt    # Single source of truth; wraps DAO
│   │   │   │   └── AlarmScheduler.kt        # Schedules AlarmManager alarms
│   │   │   ├── di/
│   │   │   │   └── DatabaseModule.kt        # Hilt @Module providing Room singleton
│   │   │   ├── receiver/
│   │   │   │   ├── AlarmReceiver.kt         # Fires notification; reschedules recurring
│   │   │   │   ├── BootReceiver.kt          # Reschedules all alarms after reboot
│   │   │   │   └── ReminderActionReceiver.kt # Handles Complete/Dismiss notification actions
│   │   │   ├── viewmodel/
│   │   │   │   └── ReminderViewModel.kt     # @HiltViewModel; FilterMode + search state
│   │   │   └── ui/
│   │   │       ├── RemndApp.kt              # NavHost with three routes
│   │   │       ├── theme/                   # Color, Theme, Type (Material 3)
│   │   │       ├── screens/
│   │   │       │   ├── HomeScreen.kt        # Reminder list + filter tabs + search bar
│   │   │       │   └── AddEditReminderScreen.kt # Create/edit form with date/time pickers
│   │   │       └── components/
│   │   │           └── ReminderItem.kt      # Individual reminder card
│   │   ├── res/                             # Drawables, strings, themes, backup rules
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts                     # App module Gradle config
│   └── proguard-rules.pro                   # Keeps Hilt + Room classes
├── gradle/
│   └── libs.versions.toml                   # Centralized version catalog
├── scripts/
│   └── build_and_upload.py                  # Local APK build + Google Drive upload
├── keystore/
│   └── debug.keystore                       # Shared debug signing key
├── .github/workflows/
│   └── build-apk.yml                        # CI: builds APK, uploads to GitHub/Drive
├── build.gradle.kts                         # Root build config (plugins only)
├── settings.gradle.kts                      # Module includes + repository config
└── gradle.properties                        # JVM args, AndroidX flag, code style
```

---

## Architecture

The app follows **MVVM** with a unidirectional data flow:

```
UI (Compose screens)
    ↕ StateFlow / events
ViewModel (ReminderViewModel)
    ↕ suspend fns / Flow
Repository (ReminderRepository)
    ↕ DAO + AlarmScheduler
Room DB         AlarmManager
```

- **UI layer** observes `uiState: StateFlow<ReminderUiState>` and dispatches user actions as function calls on the ViewModel.
- **ViewModel** combines `filterMode` and `searchQuery` flows, switches the Room query via `flatMapLatest`, and applies in-memory search filtering.
- **Repository** wraps the DAO and also calls `AlarmScheduler` when reminders are saved/deleted.
- **Broadcast receivers** handle system events (alarm fires, device boot, notification taps) independently of the Compose lifecycle.

---

## Key Conventions

### Dependency Injection
- All DI is done with **Hilt**. Every `ViewModel` uses `@HiltViewModel`. The app entry points are annotated with `@AndroidEntryPoint`.
- Room database and DAO are provided as singletons via `di/DatabaseModule.kt`.
- Do not instantiate `ReminderDatabase` or `AlarmScheduler` manually — always inject via Hilt.

### Database & Migrations
- Room database name: `remnd_db`, current version: **2**.
- When adding a new column to `Reminder`, increment the version in `@Database(version = ...)` and add a `Migration` object in `ReminderDatabase.companion`.
- Do **not** set `exportSchema = true` without also adding a schema export directory in `build.gradle.kts`.

### Data Model
`Reminder.kt` constants to use (never raw integers):

| Concept | Constant | Value |
|---|---|---|
| Priority | `Priority.LOW` | 0 |
| Priority | `Priority.MEDIUM` | 1 |
| Priority | `Priority.HIGH` | 2 |
| Frequency | `FrequencyType.NONE` | 0 |
| Frequency | `FrequencyType.DAILY` | 1 |
| Frequency | `FrequencyType.HOURLY` | 2 |

### Navigation
Three routes defined in `ui/RemndApp.kt`:
- `home` — main list
- `add_reminder` — create new reminder
- `edit_reminder/{reminderId}` — edit existing reminder (reminderId is a `Long`)

Always use the route string constants rather than hardcoded strings when navigating.

### Notifications
- Notification channel ID: `remnd_reminders` (created in `RemndApplication`).
- Notifications are grouped with a summary notification.
- Notification IDs are derived from `reminder.id.toInt()`.
- Action intents (`Complete`, `Dismiss`) are handled by `ReminderActionReceiver`.

### Alarm Scheduling
- `AlarmScheduler` uses `setExactAndAllowWhileIdle()` to fire at `dueTimeMillis`.
- An **early notification** fires 5 minutes before the main alarm.
- Recurring reminders (`DAILY` / `HOURLY`) are rescheduled inside `AlarmReceiver` after firing.
- `BootReceiver` calls `AlarmScheduler` to restore all pending alarms after a reboot.

### Code Style
- Follow the **official Kotlin style guide** (`kotlin.code.style=official` in `gradle.properties`).
- Use `viewModelScope.launch { }` for all coroutine work in ViewModels.
- Expose state to the UI only via `StateFlow`; never expose mutable state directly.
- Keep Compose functions stateless where possible; hoist state to the ViewModel.

---

## Build & Development

### Prerequisites
- **Android Studio Hedgehog** (or newer)
- JDK 17
- Android SDK with API 26–34 installed

### Common Gradle Tasks

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests (framework present; no tests written yet)
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Clean build outputs
./gradlew clean
```

APK output path: `app/build/outputs/apk/{debug|release}/`

### Signing
- **Debug**: uses the shared keystore at `keystore/debug.keystore` (password: `android`, alias: `androiddebugkey`).
- **Release**: no signing config is set — add one before distributing a release build.

### Version Catalog
All dependency versions live in `gradle/libs.versions.toml`. When upgrading a library:
1. Update the version in `[versions]`.
2. Run `./gradlew assembleDebug` to verify compatibility.
3. Check for Room/Hilt annotation processor version alignment.

---

## CI/CD

**GitHub Actions** workflow: `.github/workflows/build-apk.yml`

| Trigger | Behavior |
|---|---|
| Push to `main` | Auto-build debug APK; upload to GitHub Artifacts |
| Manual `workflow_dispatch` | Choose debug/release + optional Google Drive upload |

**Secrets required for Drive upload**: `GDRIVE_SERVICE_ACCOUNT_JSON` — a Google service account JSON with `drive.file` scope.

**Local build + upload script**: `scripts/build_and_upload.py` — builds APK and uploads to a `remnd-builds` folder on Google Drive using OAuth 2.0.

---

## Testing

Test dependencies are declared but **no tests exist yet**. When adding tests:

- **Unit tests** go in `app/src/test/java/com/remnd/` — use JUnit 4 and Mockito/MockK for ViewModel and Repository testing.
- **Instrumentation tests** go in `app/src/androidTest/java/com/remnd/` — use Espresso and Compose test rules.
- Room DAOs should be tested with an in-memory database (`Room.inMemoryDatabaseBuilder`).
- ViewModels should be tested with `kotlinx-coroutines-test` and `turbine` for Flow assertions.

---

## Known Gaps / Roadmap

From the README, features not yet implemented:

- [ ] Time picker in Add/Edit screen (currently defaults to 9 AM on selected date)
- [ ] Repeat / recurring reminders UI (backend `FrequencyType` exists but UI entry is limited)
- [ ] Home screen widgets
- [ ] Backup & restore
- [ ] Categories / labels
- [ ] Lint/detekt configuration (none currently set up)
- [ ] Unit and instrumentation test coverage

---

## Permissions

Declared in `AndroidManifest.xml`:

| Permission | Purpose |
|---|---|
| `SCHEDULE_EXACT_ALARM` | Precise alarm scheduling |
| `USE_EXACT_ALARM` | Exact alarms on Android 13+ |
| `POST_NOTIFICATIONS` | Show notifications (Android 13+, requested at runtime) |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarms after reboot |
| `WAKE_LOCK` | Keep CPU awake while processing alarm |
