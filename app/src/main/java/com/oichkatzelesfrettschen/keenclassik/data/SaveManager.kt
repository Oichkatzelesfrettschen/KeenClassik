/*
 * SaveManager.kt: Game save/load persistence management
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.oichkatzelesfrettschen.keenclassik.KeenModel

/**
 * Save slot metadata for display in save/load UI
 */
data class SaveSlotInfo(
    val slotIndex: Int,
    val isEmpty: Boolean,
    val gridSize: Int = 0,
    val difficulty: String = "",
    val profileName: String = "",
    val timestamp: Long = 0,
    val elapsedSeconds: Long = 0,
    val isSolved: Boolean = false
) {
    val displayName: String
        get() = if (isEmpty) "Empty Slot ${slotIndex + 1}" else "Slot ${slotIndex + 1}: ${gridSize}x${gridSize} $difficulty"

    val formattedTime: String
        get() {
            if (isEmpty) return ""
            val mins = elapsedSeconds / 60
            val secs = elapsedSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }

    val formattedDate: String
        get() {
            if (isEmpty || timestamp == 0L) return ""
            val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.US)
            return sdf.format(java.util.Date(timestamp))
        }
}

/**
 * Manages 12 save slots for games in progress.
 * Each slot stores the serialized KeenModel plus metadata.
 */
class SaveManager(context: Context) {

    companion object {
        const val MAX_SLOTS = 12
        private const val PREFS_NAME = "keenkenning_save_slots"
        private const val KEY_MODEL_PREFIX = "slot_model_"
        private const val KEY_SIZE_PREFIX = "slot_size_"
        private const val KEY_DIFF_PREFIX = "slot_diff_"
        private const val KEY_MODE_PREFIX = "slot_mode_"
        private const val KEY_PROFILE_PREFIX = "slot_profile_"
        private const val KEY_TIME_PREFIX = "slot_time_"
        private const val KEY_ELAPSED_PREFIX = "slot_elapsed_"
        private const val KEY_SOLVED_PREFIX = "slot_solved_"

        // Auto-save keys for implicit persistence
        private const val KEY_AUTOSAVE_MODEL = "autosave_model"
        private const val KEY_AUTOSAVE_ELAPSED = "autosave_elapsed"
        private const val KEY_AUTOSAVE_TIMESTAMP = "autosave_timestamp"
        private const val KEY_AUTOSAVE_DIFF_NAME = "autosave_diff_name"
        private const val KEY_AUTOSAVE_MODE_NAME = "autosave_mode_name"
        private const val KEY_AUTOSAVE_PROFILE_NAME = "autosave_profile_name"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Get info for all save slots
     */
    fun getAllSlotInfo(): List<SaveSlotInfo> {
        return (0 until MAX_SLOTS).map { getSlotInfo(it) }
    }

    /**
     * Get info for a specific slot
     */
    fun getSlotInfo(slotIndex: Int): SaveSlotInfo {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return SaveSlotInfo(slotIndex, true)
        }

        val modelJson = prefs.getString("$KEY_MODEL_PREFIX$slotIndex", null)
        if (modelJson.isNullOrEmpty()) {
            return SaveSlotInfo(slotIndex, true)
        }

        return SaveSlotInfo(
            slotIndex = slotIndex,
            isEmpty = false,
            gridSize = prefs.getInt("$KEY_SIZE_PREFIX$slotIndex", 0),
            difficulty = prefs.getString("$KEY_DIFF_PREFIX$slotIndex", "") ?: "",
            profileName = prefs.getString("$KEY_PROFILE_PREFIX$slotIndex", "") ?: "",
            timestamp = prefs.getLong("$KEY_TIME_PREFIX$slotIndex", 0),
            elapsedSeconds = prefs.getLong("$KEY_ELAPSED_PREFIX$slotIndex", 0),
            isSolved = prefs.getBoolean("$KEY_SOLVED_PREFIX$slotIndex", false)
        )
    }

    /**
     * Save a game to a specific slot
     */
    fun saveToSlot(
        slotIndex: Int,
        model: KeenModel,
        difficultyName: String,
        modeName: String = GameMode.STANDARD.name,
        profileName: String,
        elapsedSeconds: Long
    ): Boolean {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) return false

        try {
            val modelJson = gson.toJson(model, KeenModel::class.java)
            prefs.edit {
                putString("$KEY_MODEL_PREFIX$slotIndex", modelJson)
                putInt("$KEY_SIZE_PREFIX$slotIndex", model.size)
                putString("$KEY_DIFF_PREFIX$slotIndex", difficultyName)
                putString("$KEY_MODE_PREFIX$slotIndex", modeName)
                putString("$KEY_PROFILE_PREFIX$slotIndex", profileName)
                putLong("$KEY_TIME_PREFIX$slotIndex", System.currentTimeMillis())
                putLong("$KEY_ELAPSED_PREFIX$slotIndex", elapsedSeconds)
                putBoolean("$KEY_SOLVED_PREFIX$slotIndex", model.puzzleWon)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Load a game from a specific slot
     * @return Quadruple of (model, elapsed time, mode name, profile name)
     */
    data class LoadResult(
        val model: KeenModel?,
        val elapsedSeconds: Long,
        val modeName: String,
        val profileName: String
    )

    fun loadFromSlot(slotIndex: Int): LoadResult {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return LoadResult(null, 0, GameMode.STANDARD.name, "")
        }

        val modelJson = prefs.getString("$KEY_MODEL_PREFIX$slotIndex", null)
        if (modelJson.isNullOrEmpty()) {
            return LoadResult(null, 0, GameMode.STANDARD.name, "")
        }

        return try {
            val model = gson.fromJson(modelJson, KeenModel::class.java)
            model.ensureInitialized()
            val elapsed = prefs.getLong("$KEY_ELAPSED_PREFIX$slotIndex", 0)
            val modeName = prefs.getString("$KEY_MODE_PREFIX$slotIndex", null) ?: GameMode.STANDARD.name
            val profileName = prefs.getString("$KEY_PROFILE_PREFIX$slotIndex", "") ?: ""
            LoadResult(model, elapsed, modeName, profileName)
        } catch (e: Exception) {
            e.printStackTrace()
            LoadResult(null, 0, GameMode.STANDARD.name, "")
        }
    }

    /**
     * Auto-save the current game state (implicit persistence)
     */
    fun saveAutoSave(
        model: KeenModel,
        difficultyName: String,
        modeName: String = GameMode.STANDARD.name,
        profileName: String,
        elapsedSeconds: Long
    ) {
        try {
            val modelJson = gson.toJson(model, KeenModel::class.java)
            prefs.edit {
                putString(KEY_AUTOSAVE_MODEL, modelJson)
                putString(KEY_AUTOSAVE_DIFF_NAME, difficultyName)
                putString(KEY_AUTOSAVE_MODE_NAME, modeName)
                putString(KEY_AUTOSAVE_PROFILE_NAME, profileName)
                putLong(KEY_AUTOSAVE_TIMESTAMP, System.currentTimeMillis())
                putLong(KEY_AUTOSAVE_ELAPSED, elapsedSeconds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load the auto-saved game state
     */
    fun loadAutoSave(): LoadResult {
        val modelJson = prefs.getString(KEY_AUTOSAVE_MODEL, null)
        if (modelJson.isNullOrEmpty()) {
            return LoadResult(null, 0, GameMode.STANDARD.name, "")
        }

        return try {
            val model = gson.fromJson(modelJson, KeenModel::class.java)
            model.ensureInitialized()
            val elapsed = prefs.getLong(KEY_AUTOSAVE_ELAPSED, 0)
            val modeName = prefs.getString(KEY_AUTOSAVE_MODE_NAME, null) ?: GameMode.STANDARD.name
            val profileName = prefs.getString(KEY_AUTOSAVE_PROFILE_NAME, "") ?: ""
            LoadResult(model, elapsed, modeName, profileName)
        } catch (e: Exception) {
            e.printStackTrace()
            LoadResult(null, 0, GameMode.STANDARD.name, "")
        }
    }

    /**
     * Check if an auto-save exists
     */
    fun hasAutoSave(): Boolean {
        return prefs.contains(KEY_AUTOSAVE_MODEL)
    }

    /**
     * Delete a save slot
     */
    fun deleteSlot(slotIndex: Int): Boolean {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) return false

        prefs.edit {
            remove("$KEY_MODEL_PREFIX$slotIndex")
            remove("$KEY_SIZE_PREFIX$slotIndex")
            remove("$KEY_DIFF_PREFIX$slotIndex")
            remove("$KEY_MODE_PREFIX$slotIndex")
            remove("$KEY_PROFILE_PREFIX$slotIndex")
            remove("$KEY_TIME_PREFIX$slotIndex")
            remove("$KEY_ELAPSED_PREFIX$slotIndex")
            remove("$KEY_SOLVED_PREFIX$slotIndex")
        }
        return true
    }

    /**
     * Find first empty slot, or -1 if all full
     */
    fun findEmptySlot(): Int {
        for (i in 0 until MAX_SLOTS) {
            if (getSlotInfo(i).isEmpty) return i
        }
        return -1
    }
}
