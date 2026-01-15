# Module Topology

## Overview
KeenClassik is a single Android app module with one shared library module. It
targets classic operators and grid sizes (3x3-9x9). There is no ML/story module
or asset pipeline in this repo.

## Modules

### :app
- Android application module (Compose UI, activities, JNI bridge).
- Entry point for build types, test runners, and native build integration.

### :core
- Shared logic and interfaces for both flavors.
- Pure Kotlin/Java models and non-Android dependencies.

## Dependencies

- :app -> :core (always)

## Profiles

Classik includes Modern + Legacy profiles that share the same grid and
difficulty bounds (0-3, 3x3-9x9) and classic operators only.

## Decisions (current)
- Single-classik app with shared logic in `:core`.
- No flavors in this repo; Classik only.
- Target ABIs: armeabi-v7a, arm64-v8a, x86_64; warnings are errors.

## Open Items
- Keep test runner split: AndroidJUnitRunner for UI, AndroidBenchmarkRunner via -PkeenBenchmark.
