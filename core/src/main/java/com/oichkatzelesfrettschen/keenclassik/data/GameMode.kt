/*
 * GameMode.kt: Classik mode definitions and configuration
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik.data

/**
 * Game modes available in Keen Classik.
 *
 * Each mode has:
 * - displayName: Short name shown in UI
 * - description: Longer explanation of the mode
 * - iconName: Material icon identifier
 * - cFlags: Bit flags passed to native layer
 * - phase: Implementation phase (1-4)
 */
enum class GameMode(
    val displayName: String,
    val description: String,
    val iconName: String,
    val cFlags: Int,
    val phase: Int,
    val implemented: Boolean = false,
    /** Extended tips shown in UI (null = use description only) */
    val extendedTip: String? = null
) {
    STANDARD(
        displayName = "Standard",
        description = "All operations (+, -, ×, ÷)",
        iconName = "calculate",
        cFlags = 0x00,
        phase = 1,
        implemented = true
    ),

    MULTIPLICATION_ONLY(
        displayName = "Multiplication Only",
        description = "Only multiplication (×) operations",
        iconName = "close",  // × symbol as icon
        cFlags = 0x01,      // MODE_MULT_ONLY flag
        phase = 1,
        implemented = true,
        extendedTip = "All cages use multiplication only. Perfect for practicing times tables!"
    );

    companion object {
        fun availableModes(profile: KeenProfile): List<GameMode> =
            listOf(STANDARD, MULTIPLICATION_ONLY).filter { profile.allowsMode(it) }

        fun availableModes(): List<GameMode> = availableModes(KeenProfile.DEFAULT)

        fun allModes(profile: KeenProfile): List<GameMode> =
            listOf(STANDARD, MULTIPLICATION_ONLY)

        fun allModes(): List<GameMode> = allModes(KeenProfile.DEFAULT)

        fun byPhase(phase: Int, profile: KeenProfile): List<GameMode> =
            if (phase == 1) listOf(STANDARD, MULTIPLICATION_ONLY).filter { profile.allowsMode(it) }
            else emptyList()

        fun byPhase(phase: Int): List<GameMode> = byPhase(phase, KeenProfile.DEFAULT)

        fun isAvailable(mode: GameMode, profile: KeenProfile): Boolean =
            (mode == STANDARD || mode == MULTIPLICATION_ONLY) && profile.allowsMode(mode)

        fun isAvailable(mode: GameMode): Boolean = isAvailable(mode, KeenProfile.DEFAULT)

        /**
         * Default mode for new games
         */
        val DEFAULT = STANDARD
    }
}

/**
 * Grid size options for Classik (3x3 to 9x9).
 */
enum class GridSize(
    val size: Int,
    val displayName: String
) {
    SIZE_3(3, "3×3"),
    SIZE_4(4, "4×4"),
    SIZE_5(5, "5×5"),
    SIZE_6(6, "6×6"),
    SIZE_7(7, "7×7"),
    SIZE_8(8, "8×8"),
    SIZE_9(9, "9×9");

    companion object {
        /** Filter sizes by flavor and profile limits */
        private fun inRange(gs: GridSize, profile: KeenProfile): Boolean {
            val config = FlavorConfigProvider.get()
            val minSize = maxOf(config.minGridSize, profile.minGridSize)
            val maxSize = minOf(config.maxGridSize, profile.maxGridSize)
            return gs.size in minSize..maxSize
        }

        fun fromInt(size: Int): GridSize = entries.find { it.size == size } ?: SIZE_5
        fun allSizes(profile: KeenProfile = KeenProfile.DEFAULT): List<GridSize> =
            entries.filter { inRange(it, profile) }
    }
}

/**
 * Difficulty levels with human-readable names.
 *
 * Matches the 4-level Classik system in keen.c DIFFLIST:
 *   0=Easy, 1=Normal, 2=Hard, 3=Extreme
 *
 * Difficulty progression:
 *   - EASY: Basic single-candidate deductions
 *   - NORMAL: Set deductions (pointing pairs, box/line reduction)
 *   - HARD: Advanced sets (naked/hidden pairs, X-wing patterns)
 *   - EXTREME: Forcing chains, region-based elimination
 */
enum class Difficulty(val level: Int, val displayName: String) {
    EASY(0, "Easy"),
    NORMAL(1, "Normal"),
    HARD(2, "Hard"),
    EXTREME(3, "Extreme");

    companion object {
        fun fromInt(level: Int): Difficulty = entries.find { it.level == level } ?: NORMAL
        val DEFAULT = NORMAL

        /**
         * Get difficulties available for a given grid size and profile.
         * Profiles control the ladder: Classik=0..3.
         */
        fun forGridSize(gridSize: Int, profile: KeenProfile = KeenProfile.DEFAULT): List<Difficulty> {
            val maxLevel = profile.maxDifficulty.level
            return entries.filter { it.level <= maxLevel }
        }
    }
}
