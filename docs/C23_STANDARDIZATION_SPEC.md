# C23 Standardization Specification

## 1. Objective
Refactor the Keen Classik native codebase to strictly adhere to the **ISO/IEC 9899:2023 (C23)** standard. This ensures long-term maintainability, type safety, and compiler portability across Android (Clang), Linux (GCC/Clang), and Windows (MSVC).

## 2. Core Mandates

### 2.1 Boolean Types
*   **Legacy**: Macros `TRUE` (1) and `FALSE` (0).
*   **C23 Standard**: Use the built-in `bool` type and keywords `true` / `false`.
*   **Action**:
    *   Remove `#define TRUE` and `#define FALSE` from `puzzles.h`.
    *   Replace all instances of `int` used as booleans with `bool`.
    *   Include `<stdbool.h>` is no longer required in C23 but remains backward compatible; we will prefer native keywords.

### 2.2 Null Pointers
*   **Legacy**: `NULL` or `0`.
*   **C23 Standard**: Use the `nullptr` keyword.
*   **Action**: Replace all pointer assignments to `NULL` with `nullptr`.

### 2.3 Memory Management
*   **Legacy**: `smalloc`, `sfree`, `snewn` macros wrapping `malloc`.
*   **Action**: Retain these wrappers for consistency with the `puzzles` architecture but verify they do not violate strict aliasing rules.

### 2.4 Mathematical Constants
*   **Legacy**: `#define PI ...`
*   **C23 Standard**: Use standard math constants if available, or typed `constexpr` equivalents if supported by compiler extensions (but extensions are OFF).
*   **Action**: Retain `#define` for PI but ensure type correctness in usage.

### 2.5 Build System (CMake)
*   **Requirement**: Modern CMake (>3.20).
*   **Scope**: Use `target_compile_features` and `target_compile_options` instead of global directory-level sets.
*   **Flag**: `CMAKE_C_EXTENSIONS OFF` must be enforced to prevent GNU/MSVC extensions.

## 3. Verification
*   **Compiler**: Clang (Android NDK default).
*   **Flags**: `-Wall -Wextra -Werror -pedantic -std=c23`.
*   **Linting**: Clang-Tidy with `modernize-*`, `readability-*`, and `bugprone-*` checks.

## 4. Legacy Heuristic Translation (Domino-First Cages)
*   **Goal**: Preserve classic feel by favoring 2-cell cages (sub/div friendly),
    while keeping C23 safety and avoiding GPL code reuse.
*   **Approach**:
    *   Generate a Latin square solution, shuffle a traversal order, then place
        random domino merges between adjacent singletons.
    *   Fold remaining singletons into nearby cages with a max size cap.
    *   Reject layouts with leftover singletons to avoid invalid cages.
*   **Classik constraints**:
    *   `MAXBLK_STANDARD = 6`.
    *   Per-size multiplication cage cap to keep clues <= 9999.
*   **Implementation reference**:
    *   `app/src/main/jni/keen_generate.c` (domino placement block).
    *   Behavioral parity checked against KeenForAndroid logic, but no GPL code
        is copied.

## 5. Deterministic Overflow Guards
*   **Clue bounds**: All clue generation paths enforce `MAX_CLUE_VALUE` during
    addition, multiplication, exponentiation, and encoding.
*   **Oversize handling**: Clues that exceed the cap are marked and trigger
    regeneration rather than truncation.
*   **Encoding gate**: `keen_generate.c` rechecks clue values before encoding
    into the final puzzle string to avoid overflow propagation.
