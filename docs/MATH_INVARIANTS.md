# Classik Math Invariants

## Scope

This document summarizes the invariants enforced for Classik puzzles.
Classik uses Standard mode only, grid sizes 3x3-9x9, difficulty 0-3,
and a strict 4-digit clue cap (max 9999).

## Implemented checks (tests)

- app/src/test/.../PuzzleInvariantsTest.kt: Latin square property, zone
  constraints, and structural sanity checks.
- app/src/test/.../DeterministicSeedingTest.kt: seeded Latin square
  determinism and distribution sanity checks.
- app/src/test/.../KeenProfileTest.kt: profile bounds and difficulty caps.
- app/src/test/.../PuzzleParserTest.kt: payload parsing and clue cap
  enforcement (<= 9999).
- tests/native/keen_test_harness.c: native generation sanity checks,
  clue cap validation, and cage size limits.

## Runtime guardrails

- JNI enforces profile range (0-1), grid size 3-9, and difficulty 0-3.
- Classik profiles only accept Standard mode at JNI and UI layers.
- Native generator rejects clue values > 9999 during encoding.

## Formal verification alignment (planned)
- TLA+: model state transitions (grid validity, cage constraints, win condition).
- Z3: encode cage equations and Latin constraints for small sizes to
  cross-check the solver.
Rocq: formal proofs for Latin square generation and solver soundness are
out of scope for this repo to avoid duplication.

## Next actions

- Decide whether to add a minimal TLA+ spec in this repo.
- Add optional Z3 scripts for 3x3-5x5 validation when Z3 is installed.
- Track Rocq proof alignment externally and link here if published.
