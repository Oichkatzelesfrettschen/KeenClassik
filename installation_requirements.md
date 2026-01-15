# Installation & Build Requirements

This file is the root index for build prerequisites. Module-specific requirements live in:
- `app/INSTALLATION_REQUIREMENTS.md`
- `core/INSTALLATION_REQUIREMENTS.md`

This repository ships the **Classik** mode only (Standard mode, 3x3-9x9 grids, difficulties 0-3, 4-digit clue cap).
Kenning/advanced modes live in the KeenKenning repo.

## Prerequisites (Repo-wide)

### System Requirements
* Operating System: Linux, macOS, or Windows (verified on Linux)
* RAM: 4GB+ available to the Gradle daemon (`org.gradle.jvmargs=-Xmx4096m`)

### Java/Android Toolchain
* JDK 21 (required by Gradle 8.13)
* Android SDK:
  * Compile SDK: 36
  * Min SDK: 23
  * Target SDK: 36
* Android NDK: 27.2.12479018 (side-by-side)
  * Latest LTS (r27d) is 27.3.13750724; repo pins 27.2.12479018 for now.
* CMake: 3.22.1+ (C23 standard enforced in CMake/Gradle)
* Ninja: via Android SDK Command Line Tools (recommended)

### Build Tooling
* Gradle Wrapper: 8.13
* Android Gradle Plugin (AGP): 8.13.2
* Kotlin: 2.3.0
* Compose plugin: 2.3.0
* Warnings-as-errors enforced for Kotlin, Java, and native C

### Native Build (JNI)
* ABIs supported: `armeabi-v7a`, `arm64-v8a`, `x86_64`
* Compiler flags: `-Werror -Wall -Wextra -Wshadow -Wconversion -Wformat=2`

## Configuration & Secrets

* Keep machine-specific paths and tokens out of git:
  * `local.properties` (e.g., `sdk.dir`, `github.token`)
  * `~/.gradle/gradle.properties` (e.g., `org.gradle.java.home`)
* Do not commit secrets or local SDK/NDK paths.

## Build Instructions (Root)

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd KeenClassik
   ```
2. Configure local Android SDK path in `local.properties`:
   ```properties
   sdk.dir=/path/to/android-sdk
   ```
3. If your default JDK is newer than 21, point Gradle at a JDK 21 install:
   ```bash
   export JAVA_HOME=/path/to/jdk-21
   ```
4. Build debug APKs:
   ```bash
   ./gradlew assembleDebug
   ```
5. Run unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
6. Run lint:
   ```bash
   ./gradlew lintDebug
   ```

## Troubleshooting

* Build hangs: for tests involving long-running generation, ensure `GameViewModel.pauseTimer()` is invoked in `@After` blocks.
* Memory errors: keep `org.gradle.jvmargs=-Xmx4096m` (or higher) in `gradle.properties`.
* Native build failures: verify CMake/Ninja are installed via Android SDK Command Line Tools.
* JDK mismatch: if your system defaults to Java 25+, set `JAVA_HOME` to a JDK 21 install.
