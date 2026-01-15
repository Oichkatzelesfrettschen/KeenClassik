# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Android Keen puzzle game (KenKen-style) focused on the **Classik** experience
with classic operators and grid sizes 3x3-9x9.

Package: `com.oichkatzelesfrettschen.keenclassik` (applicationId `com.oichkatzelesfrettschen.keenclassik.classik`)

## Build Commands
- **Build Debug:** `./gradlew assembleDebug`
- **Build Release:** `./gradlew assembleRelease`
- **Clean Build:** `./gradlew clean assembleDebug`
- **Run Unit Tests:** `./gradlew testDebugUnitTest`
- **Run InstrumentedTests:** `./gradlew connectedDebugAndroidTest`
- **Lint:** `./gradlew lintDebug`
- **Format Kotlin:** `./gradlew ktlintFormat`
- **Format C/C++:** `find app/src/main/jni -name "*.[ch]" -exec clang-format -i {} +`

## Code Style
- **Kotlin:** Follows strict Kotlin coding conventions (ktlint enforced).
  - Use `val` over `var` where possible.
  - Data classes for models.
  - Sealed classes for state.
  - `allWarningsAsErrors = true` is enabled.
- **C/C++:**
  - **Standard:** ISO/IEC 9899:2023 (C23) and ISO/IEC 14882:2023 (C++23).
  - **Compliance:** Strict "Warnings as Errors" (`-Werror -Wall -Wextra`).
  - **Formatting:** Google Style (Clang-Format).
  - **Conventions:**
    - Use `bool`, `true`, `false` (stdbool.h not required in C23).
    - Use `nullptr` instead of `NULL`.
    - Explicit casts for all arithmetic conversions (`size_t`, `int`, `char`).
    - Mark unused functions/params with `[[maybe_unused]]`.
    - No GNU extensions (`CMAKE_C_EXTENSIONS OFF`).

## Architecture

```
Compose UI (GameScreen.kt, MenuScreen.kt)
    |
ViewModel Layer (GameViewModel.kt)
    |
Data Layer (data/*.kt - 10 files)
    |
Integration Layer (KeenModelBuilder.java, PuzzleParser.kt)
    |
    +-- JNI Bridge (keen-android-jni.c)
    |       |
    |       +-- Native C Layer (keen.c, latin.c, dlx.c, tree234.c)
```

### Key Layers

**JNI Bridge**: `KeenModelBuilder.java` calls native methods that return structured responses:
- Success: `"OK:payload_data"`
- Error: `"ERR:code:message"`
- JNI functions in `keen-android-jni.c` follow naming: `Java_com_oichkatzelesfrettschen_keenclassik_<Class>_<Method>`

**Puzzle Payload Format**: Zone indices + zone definitions + solution digits
```
"00,00,01,00,02,01;a00006,m00002,s00001,123456789"
 ^-- zone indices   ^-- zones (op+value)  ^-- solution
```

### Game Modes (Classik-focused)

Classik builds keep advanced modes disabled (`BuildConfig.ADVANCED_MODES_ENABLED = false`).
The mode flags remain for shared native support but are not surfaced in the Classik UI.

Modes use bit flags passed to the native layer via `modeFlags` parameter:
```kotlin
STANDARD           = 0x00   // All operations
MULTIPLICATION_ONLY = 0x01   // Only multiplication
MYSTERY            = 0x02   // Hidden operations
ZERO_INCLUSIVE     = 0x04   // Range 0 to N-1
NEGATIVE_NUMBERS   = 0x08   // Range -N to +N
EXPONENT           = 0x10   // Includes ^ operator
MODULAR            = 0x20   // Wrap-around arithmetic
KILLER             = 0x40   // No repeated digits in cages
HINT_MODE          = 0x80   // Tutorial hints
ADAPTIVE           = 0x100  // Stats-based difficulty
BITWISE            = 0x800  // XOR operations
RETRO_8BIT         = 0x0000 // UI-only mode
```

Mode definitions: `core/src/main/java/org/yegie/keenkenning/data/GameMode.kt`

## Testing

**Unit Tests** (`app/src/test/`):
- `PuzzleParserTest.kt` - Payload parsing
- `JniResultParserTest.kt` - Error handling
- `GameViewModelTest.kt` - State management
- `PuzzleInvariantsTest.kt` - Generation constraints
- `DeterministicSeedingTest.kt` - Reproducibility

Run specific tests:
```bash
./gradlew testDebugUnitTest --tests "*PuzzleParser*"
./gradlew testDebugUnitTest --tests "*.data.*"  # All data layer tests
```

**Instrumented Tests** (`app/src/androidTest/`):
- `PuzzleFlowSmokeTest.kt` - Launch -> input -> solve flow
- `KeenBenchmarkTest.kt` - Benchmark harness

## Native Development

**Source files**: `app/src/main/jni/`

| File | Purpose |
|------|---------|
| `keen-android-jni.c` | JNI entry points |
| `keen.c` | Puzzle generation, cage creation |
| `latin.c` | Latin square solver |
| `dlx.c` | Dancing Links algorithm |
| `keen_hints.c` | Constraint propagation for hints |
| `keen_validate.c` | Solution validation |

**Compiler flags** (Gradle/CMake):
```
-Werror -Wall -Wextra -Wshadow -Wconversion
-Wformat=2 -Wimplicit-fallthrough
```

**ABIs**: arm64-v8a, armeabi-v7a, x86_64

## Standards

- **Warnings as errors**: Lint `warningsAsErrors = true`, Java `-Xlint:all -Werror`
- **Internal naming**: Use "Keen" prefix (not "KenKen" - trademarked)
- **JNI naming**: `Java_com_oichkatzelesfrettschen_keenclassik_<Class>_<Method>` (underscored package path)
- **Native library**: Must be `libkeen-android-jni.so` (matches CMakeLists.txt)
- **JDK**: 21 required (Gradle 8.13 supports Java 21; see compatibility matrix)

## Build Configuration

Defined in `app/build.gradle` (Classik-only):

| Setting | Value |
|---------|-------|
| MIN_GRID_SIZE | 3 |
| MAX_GRID_SIZE | 9 |
| ADVANCED_MODES_ENABLED | false |
| applicationId | com.oichkatzelesfrettschen.keenclassik.classik |

Access via `BuildConfig.MIN_GRID_SIZE`, `BuildConfig.MAX_GRID_SIZE`,
and `BuildConfig.ADVANCED_MODES_ENABLED`.

## CI/CD

GitHub Actions workflows:
- `android-release.yml` - Build, lint, test, package APKs
- `native-sanitizers.yml` - AddressSanitizer/UBSan on JNI changes

Triggered on push to main and PRs touching `app/src/main/jni/**`.

## Common Issues

1. **JDK mismatch**: Gradle 8.13 runs on JDK 21; JDK 25 requires Gradle 9.1+
2. **JNI null returns**: Check `KeenModelBuilder.getLastJniError()` for native errors
3. **Grid size limits**: Classik supports 3x3-9x9 only

## References

- @docs/ARCHITECTURE_MODULES.md - Module overview and ownership
- @docs/REPRODUCIBLE_BUILD.md - Build checklist and verification
- @docs/CLASSIK_TEST_REPORT.md - Test status and coverage notes
- @docs/CONFIG_AUDIT.md - Config baseline and key settings
