/*
 * KeenProfile.kt: Profile-level constraints for Classik play
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik.data

enum class KeenProfile(
    val displayName: String,
    val nativeId: Int,
    val minGridSize: Int,
    val maxGridSize: Int,
    val maxDifficulty: Difficulty,
    val standardOnly: Boolean
) {
    CLASSIK_MODERN(
        displayName = "Classik (Modern)",
        nativeId = 0,
        minGridSize = 3,
        maxGridSize = 9,
        maxDifficulty = Difficulty.EXTREME,
        standardOnly = true
    ),
    CLASSIK_LEGACY(
        displayName = "Classik (Legacy)",
        nativeId = 1,
        minGridSize = 3,
        maxGridSize = 9,
        maxDifficulty = Difficulty.EXTREME,
        standardOnly = true
    );

    fun supportsSize(size: Int): Boolean = size in minGridSize..maxGridSize

    fun allowedDifficulties(): List<Difficulty> =
        Difficulty.entries.filter { it.level <= maxDifficulty.level }

    fun clampDifficulty(diff: Int): Int = diff.coerceIn(Difficulty.EASY.level, maxDifficulty.level)

    fun allowsMode(mode: GameMode): Boolean = !standardOnly || mode == GameMode.STANDARD

    companion object {
        @JvmField
        val DEFAULT = CLASSIK_MODERN

        @JvmStatic
        fun fromName(rawName: String?): KeenProfile =
            entries.find { it.name == rawName } ?: DEFAULT

        @JvmStatic
        fun availableProfiles(): List<KeenProfile> {
            return listOf(CLASSIK_MODERN, CLASSIK_LEGACY)
        }
    }
}
