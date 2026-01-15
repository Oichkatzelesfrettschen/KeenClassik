/*
 * TestHooks.kt: Minimal test-only hooks gated by instrumentation checks
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik

import androidx.core.content.edit

object TestHooks {
    const val GAME_LOADED_PREF_KEY = "keen_game_loaded"

    @Volatile
    private var gameLoaded: Boolean = false

    fun resetGameLoaded() {
        gameLoaded = false
        val context = TestEnvironment.getAppContext()
        if (context != null) {
            context.getSharedPreferences(
                context.packageName + "_preferences",
                android.content.Context.MODE_PRIVATE
            ).edit {
                putBoolean(GAME_LOADED_PREF_KEY, false)
            }
        }
    }

    fun onGameLoaded() {
        gameLoaded = true
        val context = TestEnvironment.getAppContext()
        if (context != null) {
            context.getSharedPreferences(
                context.packageName + "_preferences",
                android.content.Context.MODE_PRIVATE
            ).edit {
                putBoolean(GAME_LOADED_PREF_KEY, true)
            }
        }
    }

    fun isGameLoaded(): Boolean {
        if (gameLoaded) {
            return true
        }
        val context = TestEnvironment.getAppContext() ?: return false
        val prefs = context.getSharedPreferences(
            context.packageName + "_preferences",
            android.content.Context.MODE_PRIVATE
        )
        return prefs.getBoolean(GAME_LOADED_PREF_KEY, false)
    }
}
