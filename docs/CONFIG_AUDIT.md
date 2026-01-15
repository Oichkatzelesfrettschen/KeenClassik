# Configuration Audit (2026-01-06)

## Scope
Review module/config coverage, required files, and external dependencies. Emphasis on missing
configs/modules/packages that would block a clean build.

## Module Coverage
- Active modules: `:app`, `:core`.
- Removed scope: advanced ML/story modules; Classik repo contains only `:app` and `:core`.
- Module build files present: `app/build.gradle`, `core/build.gradle`, root `build.gradle`.
- Module install docs present: `app/INSTALLATION_REQUIREMENTS.md`, `core/INSTALLATION_REQUIREMENTS.md`.
- Profiles: Classik Modern/Legacy only; JNI profile range is 0-1.

## Build/Tooling Configs
- `settings.gradle` includes the active modules.
- `gradle.properties` enforces warnings-as-errors and config-cache failures.
- `local.properties` is expected to be local-only and remains untracked.

## External Dependencies
- Git submodule `external/latin-square-toolbox` is present and initialized.

## Gaps/Actions
- No missing module config files detected.
- Re-audit confirms no ML/ONNX/story assets in `app`, `core`, or `scripts`.
