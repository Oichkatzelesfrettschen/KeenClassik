# Keen Classik for Android

A focused, classic Keen-style logic puzzle game for Android phones, tablets, and Android TV.
This repository contains the **Classik** experience only (no Kenning/advanced modes, no ML, no story pipeline).
For extended modes and larger grids, see the Keen Kenning repo.

## Download

Releases are published in this repo once Classik packaging is finalized.

## Features

- **Classic Mode**: Standard operations (+, -, ×, ÷)
- **Grid Sizes**: 3×3 through 9×9
- **Difficulty**: Easy, Normal, Hard, Extreme (0–3)
- **Save/Load**: Multiple save slots with auto-save
- **Accessibility**: TalkBack support, focus cues, colorblind modes
- **Android TV**: D-pad + keyboard/gamepad navigation
- **Dark Theme**: Persistent toggle in Settings

## Requirements

- Android 6.0+ (API 23)
- APK size varies by ABI and assets

## Building from Source

### Prerequisites
- **JDK 21** (required by Gradle 8.13)
- **Android SDK** API 36
- **Android NDK** 27.2.12479018
- **CMake** 3.22.1+ (C23)

### Build Commands
```bash
# Clean build (strict warnings-as-errors)
./gradlew clean assembleDebug

# Unit tests (JUnit/Robolectric)
./gradlew testDebugUnitTest

# Instrumented tests
./gradlew connectedDebugAndroidTest

# Release build
./gradlew assembleRelease
```

> This project uses `allWarningsAsErrors = true`. Any warning fails the build.

Outputs: `app/build/outputs/apk/debug/` and `app/build/outputs/apk/release/`.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Compose UI Layer                         │
│  GameScreen.kt • MenuScreen.kt • VictoryAnimation.kt        │
├─────────────────────────────────────────────────────────────┤
│                   ViewModel Layer                           │
│  GameViewModel.kt • GameUiState.kt • SaveManager.kt         │
├─────────────────────────────────────────────────────────────┤
│                  Integration Layer                          │
│  KeenModelBuilder.java • PuzzleRepository.kt                │
├─────────────────────────────────────────────────────────────┤
│                    Native Layer (C)                         │
│  keen.c • keen-android-jni.c • keen_solver.c                │
└─────────────────────────────────────────────────────────────┘
```

## Known Issues

- First launch may take a second or two on low-end devices.

## License

MIT. See `LICENSE` and `THIRD_PARTY_LICENSES`.
