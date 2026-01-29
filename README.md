# KeenClassik

Android Keen puzzle game (KenKen-style) focused on the Classik experience with classic operators and grid sizes 3x3-9x9.

**Version**: 1.5.0
**Package**: `com.oichkatzelesfrettschen.keenclassik`
**License**: MIT

---

## Features

- **Classic Puzzle Experience**: Grid sizes 3x3 through 9x9
- **Difficulty Levels**: Easy, Normal, Hard, Extreme (0-3)
- **Dual Mode Support**:
  - **Standard Mode**: All operations (+, -, x, /)
  - **Multiplication-Only Mode**: Practice multiplication tables
- **Modern UI/UX**:
  - Responsive layout using 60-70% of screen space
  - WCAG AAA accessible (6.5:1 contrast)
  - Dynamic clue positioning prevents overlap
  - Responsive button and text scaling
- **Save System**: 12 save slots + auto-save
- **Accessibility**: TalkBack support, D-pad navigation, high contrast
- **Native Performance**: C23/C++23 with JNI integration

---

## Building

### Requirements

- **JDK**: 21 (Gradle 8.13 compatibility)
- **Android SDK**: API 36
- **NDK**: 27.2.12479018 (side-by-side)
- **CMake**: 3.22.1+ (via Android SDK)

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Lint check
./gradlew lintDebug
```

---

## Architecture

```
UI (Compose) -> ViewModel -> Repository -> JNI Bridge -> Native C Layer
                    |
                    +-> SaveManager (Persistence)
```

### Key Components

- **GameMode**: Enum defining puzzle modes (STANDARD, MULTIPLICATION_ONLY)
- **KeenProfile**: Grid size and difficulty constraints (Classik: 3-9, 0-3)
- **PuzzleRepository**: Puzzle generation interface
- **SaveManager**: 12-slot save system with auto-save
- **Native Layer**: Puzzle generation, Latin square solving, DLX algorithm

---

## Testing

### Test Coverage

- **Unit Tests**: 45 tests (GameMode, PuzzleRepository, Layout)
- **Instrumented Tests**: 10+ tests (MultiplicationOnlyInvariant)
- **Manual Tests**: 17 documented test cases

See [docs/MANUAL_TEST_PLAN.md](docs/MANUAL_TEST_PLAN.md) for details.

---

## License

MIT License - see [LICENSE](LICENSE) file for details.

Copyright (c) 2024-2026 KeenKenning Contributors

---

## Links

- **Repository**: https://github.com/Oichkatzelesfrettschen/KeenClassik
- **Issues**: https://github.com/Oichkatzelesfrettschen/KeenClassik/issues
