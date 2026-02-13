# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**月环 (LunaRing)** - An Android menstruation period tracking application built with Jetpack Compose.

- **Package**: `com.example.menstruation`
- **Language**: Kotlin 2.0.21
- **Min SDK**: 24, **Target/Compile SDK**: 35
- **Architecture**: MVVM + Repository Pattern with Hilt DI

## Common Commands

### Build
```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK (requires keystore.properties)
./gradlew :app:assembleRelease

# Clean build
./gradlew clean
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run tests for specific class
./gradlew test --tests "com.example.menstruation.domain.usecase.PredictNextPeriodUseCaseTest"
```

### Development
```bash
# Install debug build on connected device
./gradlew :app:installDebug

# Check for dependency updates
./gradlew dependencyUpdates
```

## Architecture

### Data Layer
- **Database**: Room (SQLite) with `AppDatabase` as main database class
- **DataStore**: `SettingsDataStore` for user preferences (period length, cycle length)
- **Entities**: `PeriodEntity`, `DailyRecordEntity` - Room entities with type converters for complex types
- **DAO**: `PeriodDao`, `DailyRecordDao` - data access objects with Flow-based queries

### Repository Layer
- `PeriodRepository`: Manages period records (start/end dates)
- `DailyRecordRepository`: Manages daily symptom/mood records
- `SettingsRepository`: Manages user settings via DataStore
- All repositories expose Flows for reactive data updates

### Domain Layer
- **Use Cases**: `PredictNextPeriodUseCase` - weighted average algorithm using last 6 cycles
- **Models**: `Period`, `DailyRecord`, `UserSettings`, `Symptom`, `Mood` - domain models separate from entities

### UI Layer (MVVM)
- **Screens**: `HomeScreen`, `StatsScreen`, `SettingsScreen`
- **ViewModels**: `HomeViewModel`, `StatsViewModel`, `SettingsViewModel` - use `viewModelScope` with Flow collection
- **State**: `HomeUiState`, `StatsUiState` - immutable state classes, updated via `MutableStateFlow`
- **Navigation**: Bottom navigation with `NavHost` in `MainActivity`

### Key Components
- **Calendar**: Infinite vertical scrolling calendar using `LazyColumn` with month-based paging
- **Record Panel**: Bottom sheet for recording daily symptoms, accessed by clicking calendar dates
- **Notifications**: `PeriodNotificationWorker` (WorkManager) for period reminders
- **In-App Updates**: `AppUpdateRepository` checks GitHub Releases for updates

## Data Flow Patterns

### Reactive Data Flow
```kotlin
// Repository exposes Flow
fun getAllPeriods(): Flow<List<Period>>

// ViewModel combines multiple flows
combine(
    periodRepository.getAllPeriods(),
    settingsRepository.settings,
    predictionCoverageEndDate
) { periods, settings, coverageEndDate ->
    // Transform and emit UI state
}.collectLatest { ... }
```

### State Management
- UI state is centralized in `*UiState` data classes
- ViewModels expose `StateFlow<UiState>` to Composables
- State updates use immutable copy: `_uiState.value = _uiState.value.copy(...)`

## Testing Requirements

- **TDD Workflow**: Write tests first (RED), implement to pass (GREEN), refactor
- **Minimum Coverage**: 80% for all code
- **Unit Tests**: JUnit 4, located in `app/src/test/`
- **Instrumented Tests**: AndroidX Test + Espresso, located in `app/src/androidTest/`
- **Compose Tests**: Use `createComposeRule()` for UI testing

## Import/Export Format

JSON structure for data backup:
```json
{
  "version": 1,
  "exportDate": "2025-02-12T10:30:00Z",
  "settings": { "periodLength": 5, "cycleLength": 28 },
  "periods": [{ "startDate": "2025-01-15", "endDate": "2025-01-19" }],
  "dailyRecords": [{ "date": "2025-01-15", "isPeriodDay": true, ... }]
}
```

## Release Process

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`
2. Create and push tag: `git tag -a v1.0.3 -m "release: v1.0.3" && git push origin v1.0.3`
3. GitHub Actions automatically builds signed APK and creates release

Requires `keystore.properties` for release builds:
```properties
storeFile=path/to/keystore.jks
storePassword=password
keyAlias=alias
keyPassword=password
```

## Color Scheme

- **Primary**: `#F8BBD9` (light pink)
- **Primary Dark**: `#F48FB1`
- **Period Mark**: `#F48FB1` (solid fill)
- **Prediction Mark**: `#F8BBD9` (dashed border)
- **Background**: `#FFF5F7`
