# Gemini Audit Notes

## Repository Overview

Keen Classik is an Android puzzle app focused on the classic experience
(3x3-9x9 grids, classic operators). It ships a single app module, a shared
`:core` library. There is no ML or story pipeline in this repository.
Advanced modes live in the KeenKenning repo.

Core modules:
- `:app`: Compose UI, JNI bridge, game logic, tests.
- `:core`: Shared models and enums (grid sizes, config).
- `external/latin-square-toolbox`: Git submodule dependency.

## Audit Findings (Key Alignment)

- Repo scope is Classik-only; `app/build.gradle` sets `MAX_GRID_SIZE=9` and
  `ADVANCED_MODES_ENABLED=false`.
- Installation requirements reflect Classik build and test tasks.
- Instrumented-test hooks exist to avoid hangs (`TestEnvironment`, `TestHooks`).
- JDK 21 is configured via `gradle.properties` for Gradle runs.

## Hypotheses and Validation

1. Gradle 8.13 runs with JDK 21.
   - Status: Validated locally by running unit tests with JDK 21.
2. Classik debug build succeeds with current config.
   - Status: Not revalidated in this pass.
3. 4-digit clue cap is enforced for Classik generation paths.
   - Status: Not revalidated in this pass.

## Next Steps

- Re-run lint, unit, and instrumented tests in this repo.
- Audit legacy grid size/difficulty mapping against external sources.
- Re-validate 4-digit clue cap behavior with regression tests.

Authoritative tracker: `tasks/TODO_MASTER.md`.
