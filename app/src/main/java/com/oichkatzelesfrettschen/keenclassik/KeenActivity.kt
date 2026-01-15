/*
 * KeenActivity.kt: Main game activity with Compose UI
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import com.google.gson.Gson
import com.oichkatzelesfrettschen.keenclassik.TestHooks
import com.oichkatzelesfrettschen.keenclassik.data.GameMode
import com.oichkatzelesfrettschen.keenclassik.data.KeenProfile
import com.oichkatzelesfrettschen.keenclassik.ui.GameScreen
import com.oichkatzelesfrettschen.keenclassik.ui.GameViewModel

class KeenActivity : AppCompatActivity() {

    companion object {
        @JvmField val CAN_CONT = "can_continue"
    }

    private val viewModel: GameViewModel by viewModels()

    // Default game data
    private var size = 3
    private var diff = 1
    private var multOnly = 0
    private var seed = 10101L
    private var gameMode = GameMode.STANDARD
    private var gameProfile = KeenProfile.DEFAULT
    private var continuing = false
    private var gameModel: KeenModel? = null

    // Names by which to read from saved prefs
    private val SAVE_MODEL = "save_model"
    private val IS_CONT = "is_continuing"
    private val SAVE_ELAPSED_TIME = "save_elapsed_time"
    private val SAVE_GAME_MODE = "save_game_mode"
    private val SAVE_GAME_PROFILE = "save_game_profile"

    // Shared prefs file
    private val sharedPref by lazy {
        getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
    }

    fun getGameData(): Bundle {
        val data = Bundle()
        data.putInt("size", size)
        data.putInt("diff", diff)
        data.putInt("mult", multOnly)
        data.putLong("seed", seed)
        return data
    }

    fun isGameLoadedForTests(): Boolean = viewModel.uiState.value.size > 0

    @VisibleForTesting
    fun getViewModelForTests(): GameViewModel = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SaveManager for save slots
        viewModel.initSaveManager(this)

        // Default to Compose Loading
        setContent {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (TestEnvironment.isInstrumentation()) {
                    CircularProgressIndicator(progress = { 0f })
                } else {
                    CircularProgressIndicator()
                }
            }
        }

        continuing = savedInstanceState?.getBoolean(IS_CONT, false) ?: false
    }

    fun returnToMainMenu() {
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_CONT, continuing)
    }

    override fun onResume() {
        super.onResume()

        // If game is already loaded and running, just resume the timer
        if (gameModel != null && viewModel.uiState.value.size > 0) {
            Log.d("KEEN", "Game already loaded, resuming timer")
            viewModel.resumeTimer()
            return
        }

        val extras = intent.extras
        // Only start new game if explicitly requested via GAME_CONT=false from menu
        val explicitNewGame = extras?.containsKey(MenuActivity.GAME_CONT) == true &&
                              extras.getBoolean(MenuActivity.GAME_CONT) == false

        // Check for saved game in SharedPreferences
        val savedModelJson = sharedPref.getString(SAVE_MODEL, "")
        val hasSavedGame = !savedModelJson.isNullOrEmpty()

        Log.d("KEEN", "onResume: explicitNewGame=$explicitNewGame, hasSavedGame=$hasSavedGame")

        // Restore saved game unless explicitly starting new game from menu
        if (hasSavedGame && !explicitNewGame) {
            try {
                val model = Gson().fromJson(savedModelJson, KeenModel::class.java)
                model.ensureInitialized()  // Reinitialize transient fields after deserialization

                // Restore timer state
                val savedElapsedTime = sharedPref.getLong(SAVE_ELAPSED_TIME, 0L)
                val savedModeName = sharedPref.getString(SAVE_GAME_MODE, GameMode.STANDARD.name)
                val savedProfileName = sharedPref.getString(SAVE_GAME_PROFILE, KeenProfile.DEFAULT.name)
                val savedGameMode = try {
                    GameMode.valueOf(savedModeName ?: GameMode.STANDARD.name)
                } catch (e: IllegalArgumentException) {
                    GameMode.STANDARD
                }
                val savedProfile = KeenProfile.fromName(savedProfileName)

                Log.d("KEEN", "Restoring saved game size=${model.size}, timer=$savedElapsedTime, mode=$savedModeName")
                runGameModel(model, savedElapsedTime, savedGameMode, savedProfile)
                return
            } catch (e: Exception) {
                Log.e("KEEN", "Failed to restore saved game: ${e.message}")
                e.printStackTrace()
                // Fall through to start new game
            }
        }

        // Start new game with parameters from intent
        size = extras?.getInt(MenuActivity.GAME_SIZE, 0) ?: 3
        diff = extras?.getInt(MenuActivity.GAME_DIFF, 0) ?: 1
        multOnly = extras?.getInt(MenuActivity.GAME_MULT, 0) ?: 0
        seed = extras?.getLong(MenuActivity.GAME_SEED, 0L) ?: 0L
        // Parse game mode from intent
        val modeName = extras?.getString(MenuActivity.GAME_MODE) ?: GameMode.STANDARD.name
        gameMode = try {
            GameMode.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            GameMode.STANDARD
        }
        val profileName = extras?.getString(MenuActivity.GAME_PROFILE) ?: KeenProfile.DEFAULT.name
        gameProfile = KeenProfile.fromName(profileName)

        // Classik supports 3x3 to 9x9 grids.
        if (size < 3 || size > 9) {
            Log.e("KEEN", "Got invalid game size, quitting...")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        Log.d("KEEN", "Starting new game: size=$size")
        runGame()
    }

    override fun onPause() {
        // Pause the timer before saving
        viewModel.pauseTimer()
        sharedPref.edit {
            val currentModel = gameModel
            if (currentModel != null) {
                val modelAsString = Gson().toJson(currentModel, KeenModel::class.java)
                putString(SAVE_MODEL, modelAsString)
                // Save timer state for this game mode
                val elapsedTime = viewModel.getElapsedTimeForSave()
                val currentGameMode = viewModel.getCurrentGameMode()
                putLong(SAVE_ELAPSED_TIME, elapsedTime)
                putString(SAVE_GAME_MODE, currentGameMode.name)
                putString(SAVE_GAME_PROFILE, viewModel.getCurrentProfile().name)
                (application as ApplicationCore).setCanCont(!currentModel.puzzleWon)
                Log.d("KEEN", "onPause: saved timer=$elapsedTime, mode=${currentGameMode.name}")
            } else {
                putString(SAVE_MODEL, "")
                putLong(SAVE_ELAPSED_TIME, 0L)
                putString(SAVE_GAME_MODE, GameMode.STANDARD.name)
                putString(SAVE_GAME_PROFILE, KeenProfile.DEFAULT.name)
                (application as ApplicationCore).setCanCont(false)
            }
        }
        super.onPause()
    }

    /**
     * Load and run a game model with optional preserved timer state.
     *
     * @param gameModel The game model to load
     * @param preservedElapsedSeconds If provided, restore timer to this value
     * @param restoredGameMode The game mode to use (defaults to current gameMode)
     */
    fun runGameModel(
        gameModel: KeenModel,
        preservedElapsedSeconds: Long? = null,
        restoredGameMode: GameMode? = null,
        restoredProfile: KeenProfile? = null
    ) {
        this.gameModel = gameModel

        val effectiveGameMode = restoredGameMode ?: gameMode
        val effectiveProfile = restoredProfile ?: gameProfile
        viewModel.loadModel(
            gameModel,
            gameMode = effectiveGameMode,
            profile = effectiveProfile,
            preservedElapsedSeconds = preservedElapsedSeconds
        )
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            if (uiState.size > 0) {
                 GameScreen(viewModel, onMenuClick = { returnToMainMenu() })
            } else {
                 Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     if (TestEnvironment.isInstrumentation()) {
                         CircularProgressIndicator(progress = { 0f })
                     } else {
                         CircularProgressIndicator()
                     }
                 }
            }
        }
        continuing = true
    }

    fun runGame() {
        // Set up reactive Compose UI that observes ViewModel state
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            if (uiState.size > 0) {
                // Game loaded - track model for save/restore
                gameModel = viewModel.getModel()
                continuing = true  // Mark as continuing so onResume doesn't regenerate
                GameScreen(viewModel, onMenuClick = { returnToMainMenu() })
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (TestEnvironment.isInstrumentation()) {
                        CircularProgressIndicator(progress = { 0f })
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        TestHooks.resetGameLoaded()
        viewModel.startNewGame(size, diff, multOnly, seed, gameMode, gameProfile)
    }
}
