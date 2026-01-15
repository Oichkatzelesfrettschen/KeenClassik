# Master Task Tracker: Keen Classik Refinement

## I. Quality Assurance & Build Discipline
- [x] **Build Baseline**: `./gradlew assembleDebug` (JDK 21).
- [x] **Unit Tests**: `./gradlew testDebugUnitTest` (run with JDK 21).
- [x] **Instrumented Tests**: `./gradlew connectedDebugAndroidTest`.
- [x] **Lint Zeroing**: `./gradlew lintDebug` (warnings are errors, JDK 21).
- [ ] **Build Profiling**: capture `--profile`, configuration cache, build cache behavior.
- [ ] **Dependency Audit**: verify versions and suppress unresolvable upgrades (SDK 36 constraint).
- [x] **Gradle Strictness**: `org.gradle.warning.mode=fail` and configuration-cache problems set to fail.
- [x] **Machine-Specific Paths**: keep `org.gradle.java.home` out of git; use local properties.

## II. Structural Harmonization
- [x] **Documentation**: Move non-Classik roadmap/verification docs into KeenKenning.
- [x] **Scripts**: Audit `scripts/` for executable permissions and headers.
- [x] **JNI**: Organized unused reference implementations into `app/src/main/jni/unused/`.
- [x] **Module Split**: Add `:core` (interfaces + shared models).
- [x] **Scope Reduction**: No ML/story modules or assets.
- [x] **Flavor Removal**: Classik-only build config and tasks.
- [x] **Makefile Alignment**: Classik-only build/test/lint commands.

## III. Codebase Deep Dive
- [x] **Java/Kotlin**: Updated `KeenModelBuilder` Javadoc and removed dead code.
- [x] **C/C++**: Verified JNI Bridge `keen-android-jni.c` handles algorithmic generation and validation paths.
- [x] **Generator Path**: Algorithmic-only generation (no ML/ONNX assets).
- [ ] **Native Mode Split**: Extract advanced mode ops (bitwise/number theory/etc.) into KeenKenning and keep Classik-only C paths.

## III-B. Classik Profile Split
- [x] **GenerationProfile Model**: Classik Modern/Legacy profiles in core.
- [x] **Difficulty Ladder**: 0-3 for Classik profiles.
- [x] **Legacy Generator Rules**: + - x / only, cage size max 6, clue cap 9999.
- [x] **Disable Auto Upgrades**: Disable 3x3 auto-upgrade in Classik profiles.
- [x] **Classik 3x3 Guard**: Downgrade Hard+ to Normal (legacy parity).
- [x] **Multiplication Cage Cap**: Per-size limit for 4-digit clue cap preservation.
- [x] **Profile-Aware UI**: Profile selector + difficulty gating in menu.
- [x] **Profile Persistence**: Save/load profile in SaveManager and intents.
- [x] **Kenning Profile Removal**: Remove Kenning profile IDs from core and JNI range checks.
- [x] **Clue Cap Parsing**: Enforce 4-digit clue cap in Kotlin parser + tests.
- [x] **Menu Mode Trim**: Hide mode selector when only Standard is available.
- [ ] **Legacy Theme Pass**: High-DPI/modern layout with classic rules.
- [x] **Difficulty Stability Tests**: Profile bounds tests + native clue-cap regression harness.
- [x] **Legacy Snapshot Scripts**: Fetch KeenForAndroid C snapshot and generate comparison report.
- [ ] **Legacy Heuristic Translation**: Document domino-first heuristics and map to C23 implementation.

## IV. Performance & Instrumentation
- [x] **Sanitizers**: document and wire `keenSanitizers`, `keenCoverage`, `keenFuncTrace` in Gradle properties.
- [x] **Perf/Flamegraph**: scripts for perf + flamegraph on native hot paths.
- [x] **Coverage**: gcovr workflow for native code (host or device build).
- [x] **Valgrind/Heaptrack**: host-native memcheck and heaptrack instructions documented.
- [x] **Infer**: static analysis workflow (see `~/Documents/Code-Analysis-Tooling`).
- [ ] **Perfetto/Tracing**: capture CPU/GPU traces for puzzle generation and rendering.

## V. Emulator & UI Automation
- [x] **Headless Emulator**: boot script + readiness checks (system_server + package service).
- [x] **Smoke Tests**: Espresso UI flow: launch -> new puzzle -> input -> solve/validate.
- [x] **Crash Repro**: automated test cases for activity recreate and navigation.

## VI. Benchmarks & Telemetry
- [x] **Benchmarks**: parse/layout/solver timing (AndroidX Benchmark or JMH).
- [x] **Memory/GPU Metrics**: memory stats and gfxinfo baseline.
- [x] **Logging Hooks**: structured logs for perf and crash triage.

## VII. Quality & Validation
- [ ] **JNI Security Audit**: Implement boundary checks for grid arrays.
- [x] **Settings Persistence**: Persist dark theme toggle in SharedPreferences.
- [x] **UI Validation Integration**: Wire `KeenValidator` error highlights into Compose UI.
- [x] **Hint Integration**: Wire `KeenHints` into UI with progressive hints.
- [ ] **D-pad Navigation**: refine Android TV focus traversal and D-pad input flow.
- [x] **Adaptive Difficulty Removed**: Classik scope keeps fixed 0-3 ladder (no adaptive modes).
- [ ] **Unit Testing**:
    - [ ] C: Test maxflow correctness vs brute force.
    - [ ] Java: Robolectric tests for `KeenModel` state transitions.
- [ ] **Game Engine Refactor**: Extract `KeenController` into standalone logic unit.

## VIII. Documentation & Roadmap Alignment
- [x] **README Alignment**: modes, ABIs, min SDK, and feature statuses updated.
- [x] **Install Docs**: root + per-module installation requirements updated.
- [x] **Reproducible Build Checklist**: documented in `docs/REPRODUCIBLE_BUILD.md`.
- [x] **Math Invariants Doc**: `docs/MATH_INVARIANTS.md` with TLA+/Z3/Rocq alignment plan.
- [x] **Gemini Audit Doc**: create/update `gemini.md` with audit findings and hypotheses.
- [x] **Post-Update Re-Audit**: verify docs match code after changes.
- [x] **Classik Doc Focus**: moved non-Classik research/verification docs into KeenKenning.
