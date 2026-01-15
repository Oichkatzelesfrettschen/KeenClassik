/*
 * GenerationResult.kt: Sealed result types for puzzle generation
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 *
 * Provides type-safe error handling for JNI boundary and generation pipeline.
 * Replaces implicit null/empty string error signaling with explicit variants.
 */

package com.oichkatzelesfrettschen.keenclassik.data

import com.oichkatzelesfrettschen.keenclassik.KeenModel

/**
 * Sealed hierarchy for puzzle generation results.
 * Eliminates ambiguous null/empty returns from JNI layer.
 */
sealed interface PuzzleGenerationResult {
    /**
     * Successful generation with a valid puzzle model.
     * @param model The generated puzzle
     * @param generationTimeMs Time taken to generate (for profiling)
     */
    data class Success(
        val model: KeenModel,
        val generationTimeMs: Long = 0
    ) : PuzzleGenerationResult

    /**
     * Generation failed with a specific reason.
     */
    sealed interface Failure : PuzzleGenerationResult {
        val message: String

        /** JNI layer returned null (grid invalid or solver failed) */
        data class NativeGenerationFailed(
            override val message: String = "Native generation returned null"
        ) : Failure

        /** Parsing the JNI string payload failed */
        data class ParsingFailed(
            override val message: String,
            val rawPayload: String? = null
        ) : Failure

        /** Invalid parameters (size out of range, etc.) */
        data class InvalidParameters(
            override val message: String,
            val paramName: String,
            val providedValue: Any?
        ) : Failure

        /** Incompatible mode combination */
        data class InvalidModes(
            override val message: String
        ) : Failure

        /** Requested grid size is too large for selected modes */
        data class SizeLimit(
            override val message: String
        ) : Failure
    }
}

/**
 * Extension to check if result is successful.
 */
fun PuzzleGenerationResult.isSuccess(): Boolean = this is PuzzleGenerationResult.Success

/**
 * Extension to get model or null.
 */
fun PuzzleGenerationResult.getModelOrNull(): KeenModel? =
    (this as? PuzzleGenerationResult.Success)?.model

/**
 * Extension to get error message or null.
 */
fun PuzzleGenerationResult.getErrorOrNull(): String? =
    (this as? PuzzleGenerationResult.Failure)?.message
