# Classik Test Report

**Date:** 2026-01-06  
**Flavor:** Classik (com.oichkatzelesfrettschen.keenclassik.classik)  
**Scope:** Standard mode only; grid sizes 3x3-9x9; difficulties 0-3 (Easy/Normal/Hard/Extreme)

## Status

Classik-only tests need a fresh run after the repo split and mode cleanup.

## Expected Coverage

- Unit tests: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Instrumented tests: `./gradlew connectedDebugAndroidTest` (device/emulator required)
- Build: `./gradlew assembleDebug`

## Notes

- 4-digit clue cap enforced in parser and UI.
- No advanced modes, AI/ONNX assets, or story pipeline in this repo.

