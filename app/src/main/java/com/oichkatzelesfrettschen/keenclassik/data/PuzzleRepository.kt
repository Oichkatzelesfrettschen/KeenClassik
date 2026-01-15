/*
 * PuzzleRepository.kt: Repository for puzzle generation and data access
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik.data

import com.oichkatzelesfrettschen.keenclassik.KeenModel
import com.oichkatzelesfrettschen.keenclassik.data.KeenProfile

/**
 * Result of puzzle generation - either success with model or failure with error message.
 */
sealed class PuzzleResult {
    data class Success(val model: KeenModel) : PuzzleResult()
    data class Failure(val errorMessage: String) : PuzzleResult()
}

interface PuzzleRepository {
    suspend fun generatePuzzle(
        size: Int,
        diff: Int,
        multOnly: Int,
        seed: Long,
        gameMode: GameMode = GameMode.STANDARD,
        profile: KeenProfile = KeenProfile.DEFAULT
    ): PuzzleResult
}

class PuzzleRepositoryImpl : PuzzleRepository {

    override suspend fun generatePuzzle(
        size: Int,
        diff: Int,
        multOnly: Int,
        seed: Long,
        gameMode: GameMode,
        profile: KeenProfile
    ): PuzzleResult {
        // Run on IO dispatcher
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val builder = com.oichkatzelesfrettschen.keenclassik.KeenModelBuilder()
            // Pass game mode flags to C layer for mode-specific generation
            val model = builder.build(size, diff, multOnly, seed, gameMode.cFlags, profile.nativeId)

            if (model != null) {
                PuzzleResult.Success(model)
            } else {
                // Get error message from builder for user feedback
                val errorMsg = builder.lastJniError ?: "Puzzle generation failed"
                PuzzleResult.Failure(errorMsg)
            }
        }
    }
}
