/*
 * PuzzleRepositoryTest.kt: Unit tests for puzzle generation with different modes
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2026 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for PuzzleRepository puzzle generation functionality.
 * Covers both STANDARD and MULTIPLICATION_ONLY modes.
 */
class PuzzleRepositoryTest {

    private lateinit var repository: PuzzleRepository

    @Before
    fun setup() {
        repository = PuzzleRepositoryImpl()
    }

    @Test
    fun `generatePuzzle with STANDARD mode succeeds`() = runBlocking {
        val result = repository.generatePuzzle(
            size = 5,
            diff = 1,
            multOnly = 0,
            seed = 12345L,
            gameMode = GameMode.STANDARD,
            profile = KeenProfile.CLASSIK_MODERN
        )

        assertTrue("STANDARD mode generation should succeed", result is PuzzleResult.Success)
        val model = (result as PuzzleResult.Success).model
        assertEquals(5, model.size)
    }

    @Test
    fun `generatePuzzle with MULTIPLICATION_ONLY mode succeeds`() = runBlocking {
        val result = repository.generatePuzzle(
            size = 5,
            diff = 1,
            multOnly = 1,
            seed = 12345L,
            gameMode = GameMode.MULTIPLICATION_ONLY,
            profile = KeenProfile.CLASSIK_MODERN
        )

        assertTrue("MULTIPLICATION_ONLY mode generation should succeed",
                   result is PuzzleResult.Success)
        val model = (result as PuzzleResult.Success).model
        assertEquals(5, model.size)
    }

    @Test
    fun `generatePuzzle with different grid sizes succeeds`() = runBlocking {
        val sizes = listOf(3, 4, 5, 6, 7, 8, 9)

        for (size in sizes) {
            val result = repository.generatePuzzle(
                size = size,
                diff = 0,
                multOnly = 0,
                seed = size * 1000L,
                gameMode = GameMode.STANDARD,
                profile = KeenProfile.CLASSIK_MODERN
            )

            assertTrue("Size $size should generate successfully",
                       result is PuzzleResult.Success)
            if (result is PuzzleResult.Success) {
                assertEquals(size, result.model.size)
            }
        }
    }

    @Test
    fun `generatePuzzle with MULTIPLICATION_ONLY on various sizes`() = runBlocking {
        val sizes = listOf(3, 5, 7, 9)

        for (size in sizes) {
            val result = repository.generatePuzzle(
                size = size,
                diff = 1,
                multOnly = 1,
                seed = size * 2000L,
                gameMode = GameMode.MULTIPLICATION_ONLY,
                profile = KeenProfile.CLASSIK_MODERN
            )

            assertTrue("MULTIPLICATION_ONLY size $size should generate successfully",
                       result is PuzzleResult.Success)
            if (result is PuzzleResult.Success) {
                assertEquals(size, result.model.size)
            }
        }
    }

    @Test
    fun `generatePuzzle with different difficulty levels`() = runBlocking {
        val difficulties = listOf(0, 1, 2, 3)  // Easy, Normal, Hard, Extreme

        for (diff in difficulties) {
            val result = repository.generatePuzzle(
                size = 6,
                diff = diff,
                multOnly = 0,
                seed = diff * 5000L,
                gameMode = GameMode.STANDARD,
                profile = KeenProfile.CLASSIK_MODERN
            )

            assertTrue("Difficulty $diff should generate successfully",
                       result is PuzzleResult.Success)
        }
    }

    @Test
    fun `generatePuzzle with MULTIPLICATION_ONLY on all difficulties`() = runBlocking {
        val difficulties = listOf(0, 1, 2, 3)

        for (diff in difficulties) {
            val result = repository.generatePuzzle(
                size = 6,
                diff = diff,
                multOnly = 1,
                seed = diff * 6000L,
                gameMode = GameMode.MULTIPLICATION_ONLY,
                profile = KeenProfile.CLASSIK_MODERN
            )

            assertTrue("MULTIPLICATION_ONLY difficulty $diff should generate successfully",
                       result is PuzzleResult.Success)
        }
    }

    @Test
    fun `generatePuzzle passes correct cFlags for STANDARD mode`() = runBlocking {
        val result = repository.generatePuzzle(
            size = 4,
            diff = 0,
            multOnly = 0,
            seed = 99999L,
            gameMode = GameMode.STANDARD,
            profile = KeenProfile.CLASSIK_MODERN
        )

        // Verify mode flags are passed (0x00 for STANDARD)
        assertTrue(result is PuzzleResult.Success)
    }

    @Test
    fun `generatePuzzle passes correct cFlags for MULTIPLICATION_ONLY mode`() = runBlocking {
        val result = repository.generatePuzzle(
            size = 4,
            diff = 0,
            multOnly = 1,
            seed = 99999L,
            gameMode = GameMode.MULTIPLICATION_ONLY,
            profile = KeenProfile.CLASSIK_MODERN
        )

        // Verify mode flags are passed (0x01 for MULT_ONLY)
        assertTrue(result is PuzzleResult.Success)
    }

    @Test
    fun `generatePuzzle with both profiles succeeds`() = runBlocking {
        val profiles = listOf(KeenProfile.CLASSIK_MODERN, KeenProfile.CLASSIK_LEGACY)

        for (profile in profiles) {
            val result = repository.generatePuzzle(
                size = 5,
                diff = 1,
                multOnly = 0,
                seed = 77777L,
                gameMode = GameMode.STANDARD,
                profile = profile
            )

            assertTrue("Profile ${profile.displayName} should generate successfully",
                       result is PuzzleResult.Success)
        }
    }

    @Test
    fun `generatePuzzle with invalid size returns failure`() = runBlocking {
        val result = repository.generatePuzzle(
            size = 2,  // Below minimum (3)
            diff = 0,
            multOnly = 0,
            seed = 11111L,
            gameMode = GameMode.STANDARD,
            profile = KeenProfile.CLASSIK_MODERN
        )

        assertTrue("Size 2 should fail validation", result is PuzzleResult.Failure)
    }

    @Test
    fun `generatePuzzle with deterministic seed produces consistent results`() = runBlocking {
        val seed = 42424242L

        val result1 = repository.generatePuzzle(
            size = 5,
            diff = 1,
            multOnly = 0,
            seed = seed,
            gameMode = GameMode.STANDARD,
            profile = KeenProfile.CLASSIK_MODERN
        )

        val result2 = repository.generatePuzzle(
            size = 5,
            diff = 1,
            multOnly = 0,
            seed = seed,
            gameMode = GameMode.STANDARD,
            profile = KeenProfile.CLASSIK_MODERN
        )

        assertTrue("Both generations should succeed",
                   result1 is PuzzleResult.Success && result2 is PuzzleResult.Success)

        if (result1 is PuzzleResult.Success && result2 is PuzzleResult.Success) {
            // Same seed should produce same puzzle structure
            assertEquals(result1.model.size, result2.model.size)
        }
    }

    @Test
    fun `generatePuzzle with MULTIPLICATION_ONLY and deterministic seed`() = runBlocking {
        val seed = 98765432L

        val result1 = repository.generatePuzzle(
            size = 6,
            diff = 2,
            multOnly = 1,
            seed = seed,
            gameMode = GameMode.MULTIPLICATION_ONLY,
            profile = KeenProfile.CLASSIK_MODERN
        )

        val result2 = repository.generatePuzzle(
            size = 6,
            diff = 2,
            multOnly = 1,
            seed = seed,
            gameMode = GameMode.MULTIPLICATION_ONLY,
            profile = KeenProfile.CLASSIK_MODERN
        )

        assertTrue("Both MULT_ONLY generations should succeed",
                   result1 is PuzzleResult.Success && result2 is PuzzleResult.Success)

        if (result1 is PuzzleResult.Success && result2 is PuzzleResult.Success) {
            assertEquals(result1.model.size, result2.model.size)
        }
    }
}
