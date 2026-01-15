# Core Module Requirements

The `:core` module provides shared Kotlin models and enums used by `:app`.

## Prerequisites

* JDK 21
* Android SDK API 36 (compile/target), min SDK 23
* Gradle Wrapper 8.13

## Build Targets

```bash
./gradlew :core:assembleDebug
./gradlew :core:testDebugUnitTest
```

## Notes

* This module is built transitively when building `:app`.
* No additional native toolchain requirements.
* Profile rules (Classik Modern/Legacy) live in `KeenProfile` and are enforced in core enums (Standard mode only, 3x3-9x9, difficulties 0-3).
