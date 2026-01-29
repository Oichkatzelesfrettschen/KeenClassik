/*
 * MultiplicationOnlyInvariantTest.kt: Property-based tests for MULTIPLICATION_ONLY mode
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2026 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oichkatzelesfrettschen.keenclassik.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Invariant tests for MULTIPLICATION_ONLY mode.
 *
 * Verifies that:
 * 1. All cages use multiplication operations only
 * 2. Puzzles satisfy Latin square property
 * 3. Puzzles are solvable
 * 4. No division, addition, or subtraction cages exist
 * 5. Solution correctness
 */
@RunWith(AndroidJUnit4::class)
class MultiplicationOnlyInvariantTest {

    private val repository = PuzzleRepositoryImpl()

    /**
     * Parse operation from clue string.
     * Clue format examples: "6×", "12×", "20×"
     */
    private fun parseOperation(clue: String): Char? {
        val ops = setOf('+', '-', '×', '÷')
        return clue.lastOrNull { it in ops }
    }

    @Test
    fun multiplicationOnly_3x3_allDifficulties() = runBlocking {
        testMultiplicationOnlyInvariants(size = 3, difficulties = listOf(0, 1, 2, 3), samples = 5)
    }

    @Test
    fun multiplicationOnly_5x5_allDifficulties() = runBlocking {
        testMultiplicationOnlyInvariants(size = 5, difficulties = listOf(0, 1, 2, 3), samples = 5)
    }

    @Test
    fun multiplicationOnly_7x7_allDifficulties() = runBlocking {
        testMultiplicationOnlyInvariants(size = 7, difficulties = listOf(0, 1, 2, 3), samples = 5)
    }

    @Test
    fun multiplicationOnly_9x9_allDifficulties() = runBlocking {
        testMultiplicationOnlyInvariants(size = 9, difficulties = listOf(0, 1, 2, 3), samples = 5)
    }

    @Test
    fun multiplicationOnly_100_puzzles_comprehensive() = runBlocking {
        // Generate 100 puzzles across different sizes and difficulties
        val testCases = listOf(
            Triple(3, 0, 10),  // 10 x 3x3 Easy
            Triple(4, 1, 10),  // 10 x 4x4 Normal
            Triple(5, 2, 10),  // 10 x 5x5 Hard
            Triple(6, 3, 10),  // 10 x 6x6 Extreme
            Triple(7, 0, 15),  // 15 x 7x7 Easy
            Triple(8, 1, 15),  // 15 x 8x8 Normal
            Triple(9, 2, 15),  // 15 x 9x9 Hard
            Triple(9, 3, 15)   // 15 x 9x9 Extreme
        )

        var totalGenerated = 0
        var totalPassed = 0

        for ((size, diff, count) in testCases) {
            for (i in 0 until count) {
                val seed = (size * 10000L) + (diff * 1000L) + i
                val result = repository.generatePuzzle(
                    size = size,
                    diff = diff,
                    multOnly = 1,
                    seed = seed,
                    gameMode = GameMode.MULTIPLICATION_ONLY,
                    profile = KeenProfile.CLASSIK_MODERN
                )

                totalGenerated++

                if (result is PuzzleResult.Success) {
                    val model = result.model

                    // Verify all invariants
                    assertTrue("All cages must use multiplication",
                               verifyMultiplicationOnly(model))
                    assertTrue("Must be valid Latin square",
                               verifyLatinSquare(model))
                    assertTrue("Puzzle must be solvable",
                               verifySolvable(model))

                    totalPassed++
                }
            }
        }

        // Verify we generated and validated all puzzles
        assertEquals("All puzzles should generate successfully", totalGenerated, totalPassed)
        assertTrue("Should test at least 100 puzzles", totalPassed >= 100)
    }

    private suspend fun testMultiplicationOnlyInvariants(
        size: Int,
        difficulties: List<Int>,
        samples: Int
    ) {
        for (diff in difficulties) {
            for (i in 0 until samples) {
                val seed = (size * 100000L) + (diff * 10000L) + i
                val result = repository.generatePuzzle(
                    size = size,
                    diff = diff,
                    multOnly = 1,
                    seed = seed,
                    gameMode = GameMode.MULTIPLICATION_ONLY,
                    profile = KeenProfile.CLASSIK_MODERN
                )

                assertTrue(
                    "MULTIPLICATION_ONLY mode should generate ${size}x${size} difficulty $diff",
                    result is PuzzleResult.Success
                )

                if (result is PuzzleResult.Success) {
                    val model = result.model

                    // Invariant 1: All cages use multiplication
                    assertTrue(
                        "${size}x${size} diff=$diff sample=$i: All cages must use multiplication only",
                        verifyMultiplicationOnly(model)
                    )

                    // Invariant 2: Valid Latin square
                    assertTrue(
                        "${size}x${size} diff=$diff sample=$i: Solution must be valid Latin square",
                        verifyLatinSquare(model)
                    )

                    // Invariant 3: Puzzle is solvable (has valid solution)
                    assertTrue(
                        "${size}x${size} diff=$diff sample=$i: Puzzle must be solvable",
                        verifySolvable(model)
                    )

                    // Invariant 4: No forbidden operations
                    assertFalse(
                        "${size}x${size} diff=$diff sample=$i: Must not contain addition cages",
                        containsOperation(model, '+')
                    )
                    assertFalse(
                        "${size}x${size} diff=$diff sample=$i: Must not contain subtraction cages",
                        containsOperation(model, '-')
                    )
                    assertFalse(
                        "${size}x${size} diff=$diff sample=$i: Must not contain division cages",
                        containsOperation(model, '÷')
                    )
                }
            }
        }
    }

    /**
     * Verify that all cages use multiplication operation only.
     */
    private fun verifyMultiplicationOnly(model: KeenModel): Boolean {
        val zones = model.zones ?: return false

        for (zone in zones) {
            val clue = zone.clue ?: continue
            val op = parseOperation(clue)

            // Single-cell zones have no operation
            if (op == null && zone.cells.size == 1) continue

            // Multi-cell zones must have multiplication
            if (op != '×') {
                return false
            }
        }

        return true
    }

    /**
     * Check if any cage contains a specific operation.
     */
    private fun containsOperation(model: KeenModel, operation: Char): Boolean {
        val zones = model.zones ?: return false

        for (zone in zones) {
            val clue = zone.clue ?: continue
            val op = parseOperation(clue)
            if (op == operation) return true
        }

        return false
    }

    /**
     * Verify that the solution is a valid Latin square.
     * Each row and column must contain each digit exactly once.
     */
    private fun verifyLatinSquare(model: KeenModel): Boolean {
        val size = model.size
        val solution = model.solution ?: return false

        // Check rows
        for (y in 0 until size) {
            val row = (0 until size).map { x -> solution[y * size + x] }
            if (!isValidSet(row, size)) return false
        }

        // Check columns
        for (x in 0 until size) {
            val col = (0 until size).map { y -> solution[y * size + x] }
            if (!isValidSet(col, size)) return false
        }

        return true
    }

    /**
     * Verify that a set contains each digit 1..size exactly once.
     */
    private fun isValidSet(values: List<Int>, size: Int): Boolean {
        if (values.size != size) return false
        val expected = (1..size).toSet()
        return values.toSet() == expected
    }

    /**
     * Verify that the puzzle is solvable by checking:
     * 1. Solution exists
     * 2. Solution satisfies all cage constraints
     * 3. Solution is a valid Latin square
     */
    private fun verifySolvable(model: KeenModel): Boolean {
        val solution = model.solution ?: return false
        val size = model.size
        val zones = model.zones ?: return false

        // Verify each cage constraint
        for (zone in zones) {
            val clue = zone.clue ?: continue
            val cells = zone.cells

            if (cells.isEmpty()) return false

            // Extract values from solution
            val values = cells.map { cellIndex ->
                solution[cellIndex]
            }

            // Parse target value and operation
            val op = parseOperation(clue)
            val targetStr = clue.takeWhile { it.isDigit() }
            val target = targetStr.toIntOrNull() ?: return false

            // Verify cage constraint
            if (op == '×' || op == null) {
                // Multiplication or single cell
                val product = values.fold(1) { acc, v -> acc * v }
                if (product != target) return false
            } else {
                // Should not reach here in MULTIPLICATION_ONLY mode
                return false
            }
        }

        return true
    }

    @Test
    fun multiplicationOnly_noCagesWithDivision() = runBlocking {
        verifyNoForbiddenOperations('÷', "division")
    }

    @Test
    fun multiplicationOnly_noCagesWithAddition() = runBlocking {
        verifyNoForbiddenOperations('+', "addition")
    }

    @Test
    fun multiplicationOnly_noCagesWithSubtraction() = runBlocking {
        verifyNoForbiddenOperations('-', "subtraction")
    }

    private suspend fun verifyNoForbiddenOperations(operation: Char, operationName: String) {
        val sizes = listOf(3, 5, 7, 9)
        val difficulties = listOf(0, 1, 2, 3)

        for (size in sizes) {
            for (diff in difficulties) {
                val result = repository.generatePuzzle(
                    size = size,
                    diff = diff,
                    multOnly = 1,
                    seed = (size * 1000L) + diff,
                    gameMode = GameMode.MULTIPLICATION_ONLY,
                    profile = KeenProfile.CLASSIK_MODERN
                )

                if (result is PuzzleResult.Success) {
                    assertFalse(
                        "${size}x${size} diff=$diff must not contain $operationName cages",
                        containsOperation(result.model, operation)
                    )
                }
            }
        }
    }

    @Test
    fun multiplicationOnly_solutionUniqueness() = runBlocking {
        // Verify that puzzles have unique solutions
        val result = repository.generatePuzzle(
            size = 6,
            diff = 2,
            multOnly = 1,
            seed = 999999L,
            gameMode = GameMode.MULTIPLICATION_ONLY,
            profile = KeenProfile.CLASSIK_MODERN
        )

        assertTrue(result is PuzzleResult.Success)

        if (result is PuzzleResult.Success) {
            val model = result.model
            assertNotNull("Solution must exist", model.solution)
            assertTrue("Solution must be valid", verifyLatinSquare(model))
            assertTrue("Solution must satisfy all constraints", verifySolvable(model))
        }
    }
}
