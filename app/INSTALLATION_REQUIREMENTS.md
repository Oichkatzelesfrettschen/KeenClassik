# App Module Requirements

This module contains the Android application, Compose UI, and JNI bridge.

## Prerequisites

* JDK 21
* Android SDK API 36 (compile/target), min SDK 23
* Android NDK 27.2.12479018 (side-by-side; latest LTS is 27.3.13750724)
* CMake 3.22.1+ (C23 enforced)
* Gradle Wrapper 8.13 (via `./gradlew`)

## Build Targets

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Unit tests
./gradlew testDebugUnitTest

# Lint (warnings are errors)
./gradlew lintDebug
```

## Notes

* ABIs supported: `armeabi-v7a`, `arm64-v8a`, `x86_64`.
* Native warnings are errors (`-Werror`); Kotlin and Java warnings are errors as well.
* Profile constraints: Classik (Modern/Legacy) uses 3x3-9x9 with difficulties 0-3; Standard mode only; 4-digit clue cap enforced.
