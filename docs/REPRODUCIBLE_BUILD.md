# Reproducible Build Checklist

This checklist captures the minimal steps to reproduce clean builds with warnings treated as errors.

## 1) Source + Submodules
```bash
git clone <repo-url>
cd KeenClassik
git submodule update --init --recursive
```

## 2) Toolchain
- JDK 21 installed and set via `JAVA_HOME`.
- Android SDK API 36 and NDK 27.2.12479018 (side-by-side).
- CMake 3.22.1+ via Android SDK.

## 3) Local Configuration
Create `local.properties` with the SDK path:
```properties
sdk.dir=/path/to/android-sdk
```

## 4) Gradle Builds (Warnings as Errors)
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

## 5) Expected Outputs
- APKs under `app/build/outputs/apk/`.

## 6) Sanity Checks
- No warnings in build output (warnings-as-errors enforced).
- Submodule `external/latin-square-toolbox` is initialized.
