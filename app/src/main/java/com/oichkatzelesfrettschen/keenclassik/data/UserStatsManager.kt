/*
 * UserStatsManager.kt: Player performance tracking for Classik
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.math.max
import kotlin.math.min

/**
 * Player performance statistics for Classik.
 * Tracks solve times, success rates, hints, errors, and undos.
 */
data class PlayerStats(
    val totalPuzzlesSolved: Int = 0,
    val totalPuzzlesAbandoned: Int = 0,
    val totalHintsUsed: Int = 0,
    val averageSolveTimeSeconds: Long = 0,
    // Per-size stats: key = gridSize (3-9), value = (solveCount, avgTimeSeconds)
    val sizeStats: Map<Int, SizeStats> = emptyMap(),
    // Computed skill score: 0.0 (beginner) to 1.0 (expert)
    val skillScore: Float = 0.5f,

    // Extended tracking for Classik analytics
    /** Current consecutive win streak */
    val currentStreak: Int = 0,
    /** Best win streak achieved */
    val bestStreak: Int = 0,
    /** Average error rate (incorrect entries per puzzle) */
    val averageErrorRate: Float = 0f,
    /** Average undo frequency (undos per puzzle) */
    val averageUndoRate: Float = 0f,
    /** Session count (for long-term tracking) */
    val totalSessions: Int = 0,
    /** Last play timestamp (epoch millis) */
    val lastPlayedTimestamp: Long = 0
)

data class SizeStats(
    val solveCount: Int = 0,
    val averageTimeSeconds: Long = 0,
    val bestTimeSeconds: Long = Long.MAX_VALUE
)

/**
 * Manages player statistics persistence for Classik.
 */
class UserStatsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "keenkenning_user_stats"
        private const val KEY_TOTAL_SOLVED = "total_solved"
        private const val KEY_TOTAL_ABANDONED = "total_abandoned"
        private const val KEY_TOTAL_HINTS = "total_hints"
        private const val KEY_AVG_TIME = "avg_time"
        private const val KEY_SKILL_SCORE = "skill_score"
        private const val KEY_SIZE_STATS_PREFIX = "size_stats_"

        // Extended tracking keys
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_AVG_ERROR_RATE = "avg_error_rate"
        private const val KEY_AVG_UNDO_RATE = "avg_undo_rate"
        private const val KEY_TOTAL_SESSIONS = "total_sessions"
        private const val KEY_LAST_PLAYED = "last_played"

        // Target solve times per grid size (seconds) for "average" player
        // Used to calibrate skill score
        private val TARGET_TIMES = mapOf(
            3 to 30L,
            4 to 60L,
            5 to 120L,
            6 to 240L,
            7 to 360L,
            8 to 480L,
            9 to 600L
        )
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStats(): PlayerStats {
        val sizeStats = mutableMapOf<Int, SizeStats>()
        for (size in 3..9) {
            val key = "$KEY_SIZE_STATS_PREFIX$size"
            val count = prefs.getInt("${key}_count", 0)
            if (count > 0) {
                sizeStats[size] = SizeStats(
                    solveCount = count,
                    averageTimeSeconds = prefs.getLong("${key}_avg", 0),
                    bestTimeSeconds = prefs.getLong("${key}_best", Long.MAX_VALUE)
                )
            }
        }

        return PlayerStats(
            totalPuzzlesSolved = prefs.getInt(KEY_TOTAL_SOLVED, 0),
            totalPuzzlesAbandoned = prefs.getInt(KEY_TOTAL_ABANDONED, 0),
            totalHintsUsed = prefs.getInt(KEY_TOTAL_HINTS, 0),
            averageSolveTimeSeconds = prefs.getLong(KEY_AVG_TIME, 0),
            sizeStats = sizeStats,
            skillScore = prefs.getFloat(KEY_SKILL_SCORE, 0.5f),
            currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0),
            bestStreak = prefs.getInt(KEY_BEST_STREAK, 0),
            averageErrorRate = prefs.getFloat(KEY_AVG_ERROR_RATE, 0f),
            averageUndoRate = prefs.getFloat(KEY_AVG_UNDO_RATE, 0f),
            totalSessions = prefs.getInt(KEY_TOTAL_SESSIONS, 0),
            lastPlayedTimestamp = prefs.getLong(KEY_LAST_PLAYED, 0)
        )
    }

    /**
     * Record a completed puzzle for stats tracking.
     * @param gridSize The puzzle grid size (3-9)
     * @param solveTimeSeconds Time taken to solve
     * @param hintsUsed Number of hints used
     * @param difficulty Original difficulty level (0-3)
     * @param errorCount Number of incorrect entries made (optional, default 0)
     * @param undoCount Number of undo operations used (optional, default 0)
     */
    fun recordPuzzleSolved(
        gridSize: Int,
        solveTimeSeconds: Long,
        hintsUsed: Int,
        difficulty: Int,
        errorCount: Int = 0,
        undoCount: Int = 0
    ) {
        // Update global stats
        val newTotal = prefs.getInt(KEY_TOTAL_SOLVED, 0) + 1
        val oldAvg = prefs.getLong(KEY_AVG_TIME, 0)
        val newAvg = if (newTotal == 1) solveTimeSeconds
                     else (oldAvg * (newTotal - 1) + solveTimeSeconds) / newTotal
        val newHints = prefs.getInt(KEY_TOTAL_HINTS, 0) + hintsUsed

        // Update per-size stats
        val key = "$KEY_SIZE_STATS_PREFIX$gridSize"
        val sizeCount = prefs.getInt("${key}_count", 0) + 1
        val sizeOldAvg = prefs.getLong("${key}_avg", 0)
        val sizeNewAvg = if (sizeCount == 1) solveTimeSeconds
                         else (sizeOldAvg * (sizeCount - 1) + solveTimeSeconds) / sizeCount
        val sizeBest = min(prefs.getLong("${key}_best", Long.MAX_VALUE), solveTimeSeconds)

        // Update win streak
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0) + 1
        val bestStreak = max(prefs.getInt(KEY_BEST_STREAK, 0), currentStreak)

        // Update error rate (exponential moving average)
        val cellCount = gridSize * gridSize
        val errorRate = errorCount.toFloat() / cellCount
        val oldErrorRate = prefs.getFloat(KEY_AVG_ERROR_RATE, 0f)
        val newErrorRate = if (newTotal == 1) errorRate
                           else oldErrorRate * 0.8f + errorRate * 0.2f

        // Update undo rate (exponential moving average)
        val undoRate = undoCount.toFloat() / cellCount
        val oldUndoRate = prefs.getFloat(KEY_AVG_UNDO_RATE, 0f)
        val newUndoRate = if (newTotal == 1) undoRate
                          else oldUndoRate * 0.8f + undoRate * 0.2f

        // Update last played timestamp
        val lastPlayed = System.currentTimeMillis()

        // Recompute skill score with enhanced factors
        val skillScore = computeSkillScore(gridSize, solveTimeSeconds, hintsUsed, difficulty, errorCount, undoCount)
        val oldSkill = prefs.getFloat(KEY_SKILL_SCORE, 0.5f)
        // Exponential moving average: 20% new, 80% old for stability
        val newSkill = oldSkill * 0.8f + skillScore * 0.2f

        prefs.edit {
            putInt(KEY_TOTAL_SOLVED, newTotal)
            putLong(KEY_AVG_TIME, newAvg)
            putInt(KEY_TOTAL_HINTS, newHints)
            putInt("${key}_count", sizeCount)
            putLong("${key}_avg", sizeNewAvg)
            putLong("${key}_best", sizeBest)
            putInt(KEY_CURRENT_STREAK, currentStreak)
            putInt(KEY_BEST_STREAK, bestStreak)
            putFloat(KEY_AVG_ERROR_RATE, newErrorRate)
            putFloat(KEY_AVG_UNDO_RATE, newUndoRate)
            putLong(KEY_LAST_PLAYED, lastPlayed)
            putFloat(KEY_SKILL_SCORE, newSkill.coerceIn(0.1f, 0.95f))
        }
    }

    fun recordPuzzleAbandoned() {
        prefs.edit {
            putInt(KEY_TOTAL_ABANDONED, prefs.getInt(KEY_TOTAL_ABANDONED, 0) + 1)
            putInt(KEY_CURRENT_STREAK, 0)  // Reset streak on abandon
        }
    }

    /**
     * Record the start of a new session (app opened).
     */
    fun recordSessionStart() {
        prefs.edit {
            putInt(KEY_TOTAL_SESSIONS, prefs.getInt(KEY_TOTAL_SESSIONS, 0) + 1)
        }
    }

    /**
     * Compute skill score for a single puzzle solve.
     *
     * Factors considered (weighted):
     * - Time vs target (40%): faster = higher score
     * - Hint penalty (15%): each hint reduces score
     * - Error penalty (15%): high error rate reduces score
     * - Undo penalty (10%): excessive undos reduce score
     * - Difficulty bonus (20%): harder puzzles give more credit
     */
    private fun computeSkillScore(
        gridSize: Int,
        timeSeconds: Long,
        hintsUsed: Int,
        difficulty: Int,
        errorCount: Int = 0,
        undoCount: Int = 0
    ): Float {
        val targetTime = TARGET_TIMES[gridSize] ?: 300L

        // Time factor (40%): 1.0 if faster than target, decreases as slower
        val timeFactor = (targetTime.toFloat() / max(timeSeconds, 1).toFloat()).coerceIn(0.2f, 2.0f)

        // Hint penalty (15%): each hint reduces score
        val hintPenalty = min(hintsUsed * 0.15f, 0.5f)

        // Error penalty (15%): based on error rate relative to grid size
        val cellCount = gridSize * gridSize
        val errorRate = errorCount.toFloat() / cellCount
        val errorPenalty = min(errorRate * 2f, 0.4f)

        // Undo penalty (10%): excessive undos indicate struggle
        val undoRate = undoCount.toFloat() / cellCount
        val undoPenalty = min(undoRate * 1.5f, 0.3f)

        // Difficulty bonus (20%): harder puzzles give more credit
        val difficultyBonus = difficulty * 0.2f

        // Weighted combination
        val rawScore = (timeFactor * 0.4f) +
                       (difficultyBonus * 0.2f) -
                       (hintPenalty * 0.15f) -
                       (errorPenalty * 0.15f) -
                       (undoPenalty * 0.1f)

        return rawScore.coerceIn(0.0f, 1.0f)
    }

    /**
     * Get a recommended grid size based on player experience.
     * @return Recommended grid size (3-9)
     */
    fun getRecommendedGridSize(): Int {
        val stats = getStats()
        val totalSolved = stats.totalPuzzlesSolved

        return when {
            totalSolved < 5 -> 4      // Beginner: start small
            totalSolved < 15 -> 5     // Learning
            totalSolved < 30 -> 6     // Intermediate
            totalSolved < 60 -> 7     // Experienced
            stats.skillScore > 0.7f -> 8  // Expert: allow larger
            stats.skillScore > 0.85f -> 9 // Master
            else -> 6                 // Default to medium
        }
    }

    fun clearStats() {
        prefs.edit { clear() }
    }
}
