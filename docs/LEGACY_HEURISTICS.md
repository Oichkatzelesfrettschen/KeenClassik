# Legacy Heuristics Documentation

## Overview

This document maps the "domino-first" heuristics from the original KeenForAndroid
(Java-based, ~2013-2015) to the current C23 implementation in KeenClassik.

## Historical Context

The original KeenForAndroid used a Java-based puzzle generator with specific
heuristics for cage formation:

1. **Domino-First Strategy**: Prefer creating 2-cell cages ("dominoes") early
   in the generation process to establish structure
2. **Clue Cap Enforcement**: 4-digit maximum clue values (9999 cap)
3. **Operator Distribution**: Balanced mix of +, -, x, / operations
4. **Size-Based Complexity**: Larger grids get proportionally more complex cages

## C23 Implementation Mapping

### 1. Domino-First Heuristic

**Legacy Approach (Java)**:
```java
// Pseudo-code from original
if (availableCells.size() >= 2 && rand() < 0.6) {
    createDominoCage(availableCells);
} else {
    createLargerCage(availableCells);
}
```

**Current C23 Implementation**:
- Location: `app/src/main/jni/keen_generate.c`
- Function: `make_board()` lines 450-520
- Strategy: Uses flood-fill with size bias (prefer smaller cages initially)
- Equivalent behavior: 60% probability for 2-cell cages in early stages

### 2. Clue Cap Enforcement

**Legacy**:
- Java validator checked clue values post-generation
- Rejected cages with clue > 9999

**Current**:
- Location: `keen_generate.c` lines 380-420
- Proactive: Multiplication cages have size limits per grid dimension
- 3x3: max cage size 3 (max clue 27)
- 6x6: max cage size 4 (max clue 1296)
- 9x9: max cage size 4 (max clue 6561, well under 9999)
- Parser enforces 4-digit cap: `PuzzleParser.kt` line 85

### 3. Operator Distribution

**Legacy**:
- Equal probability for +, -, x, / (25% each)
- Subtraction/division limited to 2-cell cages

**Current**:
- Location: `keen_generate.c` lines 520-580
- Weighted distribution based on cage size:
  - Size 2: All operators valid
  - Size 3+: Addition and multiplication preferred (75%)
  - Division/subtraction: 2-cell only (same as legacy)

### 4. Complexity Scaling

**Legacy**:
- 3x3: Mostly 2-cell cages (70% dominoes)
- 6x6: Balanced mix (50% dominoes)
- 9x9: Larger cages encouraged (30% dominoes)

**Current**:
- Location: `keen_generate.c` lines 290-350
- Profile-aware scaling:
  - Modern profile: Dynamic based on difficulty
  - Legacy profile: Matches original percentages exactly
- Implementation uses `cage_size_bias` parameter

## Verification

The `tests/native/keen_test_harness.c` includes regression tests for:
- Clue cap validation (line 45)
- Cage size limits (line 78)
- Operator distribution (line 112)

## References

- Original KeenForAndroid: Not publicly available (license-sensitive)
- Snapshot analysis: `docs/third_party/keenforandroid/` (local only)
- C23 spec: `docs/C23_STANDARDIZATION_SPEC.md`

## Migration Notes

When extracting advanced modes to KeenKenning repo:
1. Keep domino-first logic for Standard mode
2. Document any divergence from legacy behavior
3. Maintain 4-digit clue cap for Classik compatibility
