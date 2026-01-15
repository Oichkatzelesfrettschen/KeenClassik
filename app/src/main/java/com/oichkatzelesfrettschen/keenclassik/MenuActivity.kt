/*
 * MenuActivity.kt: Main menu activity with Compose UI
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.oichkatzelesfrettschen.keenclassik.data.Difficulty
import com.oichkatzelesfrettschen.keenclassik.data.GameMode
import com.oichkatzelesfrettschen.keenclassik.data.GridSize
import com.oichkatzelesfrettschen.keenclassik.data.KeenProfile
import com.oichkatzelesfrettschen.keenclassik.ui.MenuScreen
import com.oichkatzelesfrettschen.keenclassik.ui.MenuState

/**
 * Modern Compose-based Menu Activity
 */
class MenuActivity : AppCompatActivity() {

    companion object {
        // Intent extras for launching KeenActivity
        const val GAME_SIZE = "gameSize"
        const val GAME_DIFF = "gameDiff"
        const val GAME_MULT = "gameMultOnly"
        const val GAME_MODE = "gameMode"
        const val GAME_SEED = "gameSeed"
        const val GAME_PROFILE = "gameProfile"
        const val GAME_CONT = "contPrev"

        // SharedPreferences keys (used by ApplicationCore)
        @JvmField val MENU_SIZE = "menuSize"
        @JvmField val MENU_DIFF = "menuDiff"
        @JvmField val MENU_MULT = "menuMult"
        @JvmField val MENU_MODE = "menuMode"
        @JvmField val MENU_PROFILE = "menuProfile"
        @JvmField val DARK_MODE = "darkMode"
    }

    private lateinit var app: ApplicationCore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as ApplicationCore

        setContent {
            val prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)
            val lifecycleOwner = LocalLifecycleOwner.current

            val availableProfiles = KeenProfile.availableProfiles()
            val savedProfileName = prefs.getString(MENU_PROFILE, KeenProfile.DEFAULT.name)
            val savedProfile = KeenProfile.fromName(savedProfileName)
            val initialProfile = if (savedProfile in availableProfiles) {
                savedProfile
            } else {
                KeenProfile.DEFAULT
            }
            app.gameProfile = initialProfile

            val availableModes = GameMode.availableModes(initialProfile)
            val savedModeName = prefs.getString(MENU_MODE, GameMode.STANDARD.name)
            val savedMode = try {
                GameMode.valueOf(savedModeName ?: GameMode.STANDARD.name)
            } catch (e: IllegalArgumentException) {
                GameMode.STANDARD
            }
            val initialMode = if (savedMode in availableModes) savedMode else GameMode.DEFAULT

            val sizes = GridSize.allSizes(initialProfile)
            val initialSize = sizes.firstOrNull { it.size == app.gameSize }?.size
                ?: sizes.first().size
            val allowedDifficulties = Difficulty.forGridSize(initialSize, initialProfile)
            val initialDifficulty = allowedDifficulties.firstOrNull { it.level == app.gameDiff }?.level
                ?: Difficulty.DEFAULT.level

            var menuState by remember {
                mutableStateOf(
                    MenuState(
                        selectedSize = initialSize,
                        selectedDifficulty = initialDifficulty,
                        selectedMode = initialMode,
                        selectedProfile = initialProfile,
                        canContinue = app.isCanCont
                    )
                )
            }

            // Refresh state when Activity resumes (e.g., returning from game)
            // This ensures difficulty indicator matches saved game state
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        val currentProfileName = prefs.getString(MENU_PROFILE, KeenProfile.DEFAULT.name)
                        val currentProfile = KeenProfile.fromName(currentProfileName)
                        val clampedProfile = if (currentProfile in availableProfiles) {
                            currentProfile
                        } else {
                            KeenProfile.DEFAULT
                        }

                        val updatedSizes = GridSize.allSizes(clampedProfile)
                        val clampedSize = updatedSizes.firstOrNull { it.size == app.gameSize }?.size
                            ?: updatedSizes.first().size
                        val updatedDiffs = Difficulty.forGridSize(clampedSize, clampedProfile)
                        val clampedDiff = updatedDiffs.firstOrNull { it.level == app.gameDiff }?.level
                            ?: Difficulty.DEFAULT.level

                        val updatedModes = GameMode.availableModes(clampedProfile)
                        val currentModeName = prefs.getString(MENU_MODE, GameMode.STANDARD.name)
                        val currentMode = try {
                            GameMode.valueOf(currentModeName ?: GameMode.STANDARD.name)
                        } catch (e: IllegalArgumentException) {
                            GameMode.STANDARD
                        }
                        val clampedMode = if (currentMode in updatedModes) {
                            currentMode
                        } else {
                            GameMode.DEFAULT
                        }
                        menuState = menuState.copy(
                            selectedSize = clampedSize,
                            selectedDifficulty = clampedDiff,
                            selectedMode = clampedMode,
                            selectedProfile = clampedProfile,
                            canContinue = app.isCanCont
                        )
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MenuScreen(
                state = menuState,
                onSizeChange = { size ->
                    menuState = menuState.copy(selectedSize = size)
                    app.gameSize = size
                },
                onDifficultyChange = { diff ->
                    menuState = menuState.copy(selectedDifficulty = diff)
                    app.gameDiff = diff
                },
                onModeChange = { mode ->
                    menuState = menuState.copy(
                        selectedMode = mode
                    )
                    // Persist mode selection
                    prefs.edit { putString(MENU_MODE, mode.name) }
                },
                onProfileChange = { profile ->
                    val profileSizes = GridSize.allSizes(profile)
                    val clampedSize = profileSizes.firstOrNull { it.size == menuState.selectedSize }?.size
                        ?: profileSizes.first().size
                    val profileDiffs = Difficulty.forGridSize(clampedSize, profile)
                    val clampedDiff = profileDiffs.firstOrNull { it.level == menuState.selectedDifficulty }?.level
                        ?: Difficulty.DEFAULT.level
                    val profileModes = GameMode.availableModes(profile)
                    val clampedMode = if (menuState.selectedMode in profileModes) {
                        menuState.selectedMode
                    } else {
                        GameMode.DEFAULT
                    }
                    menuState = menuState.copy(
                        selectedProfile = profile,
                        selectedSize = clampedSize,
                        selectedDifficulty = clampedDiff,
                        selectedMode = clampedMode
                    )
                    app.gameSize = clampedSize
                    app.gameDiff = clampedDiff
                    app.gameProfile = profile
                    prefs.edit { putString(MENU_PROFILE, profile.name) }
                    prefs.edit { putString(MENU_MODE, clampedMode.name) }
                },
                onStartGame = { startGame(menuState) },
                onContinueGame = { continueGame(menuState) }
            )
        }
    }

    // Note: State refresh on resume is handled by DisposableEffect in setContent

    override fun onPause() {
        app.savePrefs()
        super.onPause()
    }

    private fun startGame(state: MenuState) {
        val intent = Intent(this, KeenActivity::class.java).apply {
            putExtra(GAME_CONT, false)
            putExtra(GAME_SIZE, state.selectedSize)
            putExtra(GAME_DIFF, state.selectedDifficulty)
            putExtra(GAME_MODE, state.selectedMode.name)
            putExtra(GAME_PROFILE, state.selectedProfile.name)
            putExtra(GAME_MULT, state.selectedMode.cFlags and 0x01) // Legacy compat
            putExtra(GAME_SEED, System.currentTimeMillis())
        }
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER")  // State reserved for future use
    private fun continueGame(state: MenuState) {
        val intent = Intent(this, KeenActivity::class.java).apply {
            putExtra(GAME_CONT, true)
        }
        startActivity(intent)
    }
}
