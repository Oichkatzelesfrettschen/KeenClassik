/*
 * GameViewModel.kt: ViewModel for game state management
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.oichkatzelesfrettschen.keenclassik.KeenModel
import com.oichkatzelesfrettschen.keenclassik.MenuActivity
import com.oichkatzelesfrettschen.keenclassik.TestEnvironment
import com.oichkatzelesfrettschen.keenclassik.TestHooks
import com.oichkatzelesfrettschen.keenclassik.data.GameMode
import com.oichkatzelesfrettschen.keenclassik.data.PuzzleRepository
import com.oichkatzelesfrettschen.keenclassik.data.PuzzleRepositoryImpl
import com.oichkatzelesfrettschen.keenclassik.data.PuzzleResult
import com.oichkatzelesfrettschen.keenclassik.data.SaveManager
import com.oichkatzelesfrettschen.keenclassik.data.SaveSlotInfo
import com.oichkatzelesfrettschen.keenclassik.data.GridValidator
import com.oichkatzelesfrettschen.keenclassik.data.KeenHints
import com.oichkatzelesfrettschen.keenclassik.data.KeenProfile
import com.oichkatzelesfrettschen.keenclassik.data.NativeGridValidator
import com.oichkatzelesfrettschen.keenclassik.data.UserStatsManager

class GameViewModel(
    private val repository: PuzzleRepository = PuzzleRepositoryImpl(),
    private val validator: GridValidator = NativeGridValidator
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var keenModel: KeenModel? = null
    private var saveManager: SaveManager? = null
    private var statsManager: UserStatsManager? = null
    private var settingsPrefs: SharedPreferences? = null

    // Timer management
    private var timerJob: Job? = null
    private var gameStartTime: Long = 0
    private var accumulatedTime: Long = 0  // For pause/resume

    // Game metadata (set from activity)
    private var currentDifficulty: Int = 1
    private var currentGameMode: GameMode = GameMode.STANDARD
    private var currentProfile: KeenProfile = KeenProfile.DEFAULT

    fun initSaveManager(context: Context) {
        val appContext = context.applicationContext
        if (saveManager == null) {
            saveManager = SaveManager(appContext)
        }
        if (statsManager == null) {
            statsManager = UserStatsManager(appContext)
        }
        if (settingsPrefs == null) {
            settingsPrefs = appContext.getSharedPreferences(
                appContext.packageName + "_preferences",
                Context.MODE_PRIVATE
            )
            val savedDarkTheme = settingsPrefs?.getBoolean(MenuActivity.DARK_MODE, _uiState.value.darkTheme)
                ?: _uiState.value.darkTheme
            _uiState.update { it.copy(darkTheme = savedDarkTheme) }
        }
    }

    fun startNewGame(
        size: Int,
        diff: Int,
        multOnly: Int,
        seed: Long,
        gameMode: GameMode = GameMode.STANDARD,
        profile: KeenProfile = KeenProfile.DEFAULT
    ) {
        val effectiveDiff = profile.clampDifficulty(diff)

        // Store metadata before loading
        currentDifficulty = effectiveDiff
        currentGameMode = gameMode
        currentProfile = profile

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showErrorDialog = false, errorMessage = null) }
            when (val result = repository.generatePuzzle(size, effectiveDiff, multOnly, seed, gameMode, profile)) {
                is PuzzleResult.Success -> {
                    loadModel(result.model, effectiveDiff, gameMode = gameMode, profile = profile)
                }
                is PuzzleResult.Failure -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        showErrorDialog = true,
                        errorMessage = result.errorMessage
                    ) }
                }
            }
        }
    }

    /**
     * Load a model with optional elapsed time restoration.
     *
     * @param model The KeenModel to load
     * @param difficulty Difficulty level (0-3)
     * @param gameMode The current game mode
     * @param preservedElapsedSeconds If provided, restore this elapsed time instead of resetting.
     *                                 Use this when resuming a saved game.
     */
    fun loadModel(
        model: KeenModel,
        difficulty: Int = currentDifficulty,
        gameMode: GameMode = currentGameMode,
        profile: KeenProfile = currentProfile,
        preservedElapsedSeconds: Long? = null
    ) {
        keenModel = model
        currentDifficulty = difficulty
        currentGameMode = gameMode
        currentProfile = profile

        // Stop any running timer
        stopTimer()

        // Restore or reset timer based on whether we're resuming
        val elapsed = preservedElapsedSeconds ?: 0L
        accumulatedTime = elapsed

        _uiState.update {
            it.copy(
                showVictoryAnimation = false,
                victoryAnimationComplete = false,
                elapsedTimeSeconds = elapsed,
                timerRunning = false,
                difficulty = difficulty,
                difficultyName = getDifficultyName(difficulty),
                gameMode = gameMode
            )
        }
        // refreshState() updates cells, zones, activeCell, and isInputtingNotes based on the model
        refreshState()

        // Start timer (for new game it starts at 0, for resumed game it continues from preserved time)
        if (!model.puzzleWon) {
            startTimer()
        }
    }

    private fun getDifficultyName(diff: Int): String = when (diff) {
        0 -> "Easy"
        1 -> "Normal"
        2 -> "Hard"
        3 -> "Extreme"
        else -> "Unknown"
    }

    // Timer functions
    private fun startTimer() {
        if (TestEnvironment.isInstrumentation()) {
            _uiState.update { it.copy(timerRunning = false) }
            return
        }
        if (timerJob?.isActive == true) return
        if (_uiState.value.isSolved) return

        gameStartTime = System.currentTimeMillis()
        _uiState.update { it.copy(timerRunning = true) }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val elapsed = accumulatedTime + (System.currentTimeMillis() - gameStartTime) / 1000
                _uiState.update { it.copy(elapsedTimeSeconds = elapsed) }
            }
        }
    }

    private fun stopTimer() {
        if (timerJob?.isActive == true) {
            accumulatedTime += (System.currentTimeMillis() - gameStartTime) / 1000
        }
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(timerRunning = false) }
        saveAutoSave()
    }

    fun pauseTimer() {
        stopTimer()
    }

    fun resumeTimer() {
        if (!_uiState.value.isSolved) {
            startTimer()
        }
    }

    fun getModel(): KeenModel? = keenModel

    /**
     * Get the current elapsed time for saving.
     * Includes accumulated time plus current session time.
     */
    fun getElapsedTimeForSave(): Long {
        return if (timerJob?.isActive == true) {
            accumulatedTime + (System.currentTimeMillis() - gameStartTime) / 1000
        } else {
            accumulatedTime
        }
    }

    /**
     * Get the current game mode for save/restore purposes.
     */
    fun getCurrentGameMode(): GameMode = currentGameMode

    fun getCurrentProfile(): KeenProfile = currentProfile

    private fun refreshState() {
        val model = keenModel ?: return
        model.ensureInitialized()
        val size = model.size
        val grid = buildGridArray(model)
        val errorFlags = if (TestEnvironment.isInstrumentation()) {
            IntArray(size * size)
        } else {
            validator.validateGrid(
                size,
                grid,
                model.dsf,
                model.clues,
                currentGameMode.cFlags
            ) ?: IntArray(size * size)
        }

        // Helper to safely get zone ID
        fun getZoneId(x: Int, y: Int): Int {
            if (x < 0 || x >= size || y < 0 || y >= size) return -1
            return model.getCell(x.toShort(), y.toShort()).zone.code
        }

        // Determine anchors for clues (Top-Left-most cell of each zone)
        val zoneAnchors = mutableMapOf<Int, Pair<Int, Int>>()
        for (y in 0 until size) {
            for (x in 0 until size) {
                 val zoneId = getZoneId(x, y)
                 if (!zoneAnchors.containsKey(zoneId)) {
                     zoneAnchors[zoneId] = x to y
                 }
            }
        }

        // Create 2D list of UiCells
        val uiCells = List(size) { x ->
            List(size) { y ->
                val cell = model.getCell(x.toShort(), y.toShort())
                val currentZoneId = cell.zone.code
                
                val borders = CellBorders(
                    top = getZoneId(x, y - 1) != currentZoneId,
                    bottom = getZoneId(x, y + 1) != currentZoneId,
                    left = getZoneId(x - 1, y) != currentZoneId,
                    right = getZoneId(x + 1, y) != currentZoneId
                )

                val notesList = mutableListOf<Boolean>()
                for (g in cell.guesses) {
                    notesList.add(g)
                }
                
                val isAnchor = zoneAnchors[currentZoneId] == (x to y)
                val clue = if (isAnchor) cell.zone.toString() else null

                UiCell(
                    x = x,
                    y = y,
                    value = if (cell.finalGuessValue == -1) null else cell.finalGuessValue,
                    notes = notesList,
                    zoneId = currentZoneId,
                    isSelected = (model.activeX.toInt() == x && model.activeY.toInt() == y),
                    borders = borders,
                    clue = clue,
                    errorFlags = errorFlags[x * size + y]
                )
            }
        }

        val uiZones = model.gameZones.map { zone ->
             UiZone(
                 id = zone.code,
                 label = zone.toString(),
                 color = 0xFFFFFFFF 
             )
        }

        _uiState.update { 
            it.copy(
                size = size,
                cells = uiCells,
                zones = uiZones,
                activeCell = if (model.activeX >= 0) (model.activeX.toInt() to model.activeY.toInt()) else null,
                isSolved = model.puzzleWon,
                isInputtingNotes = !model.finalGuess,
                isLoading = false
            )
        }
        TestHooks.onGameLoaded()
    }

    private fun buildGridArray(model: KeenModel): IntArray {
        val size = model.size
        val grid = IntArray(size * size)
        for (x in 0 until size) {
            for (y in 0 until size) {
                val value = model.getCell(x.toShort(), y.toShort()).finalGuessValue
                grid[x * size + y] = if (value == -1) 0 else value
            }
        }
        return grid
    }
    
    fun onCellClicked(x: Int, y: Int) {
        val model = keenModel ?: return
        
        // Double tap logic: if already selected, toggle note mode
        if (model.activeX.toInt() == x && model.activeY.toInt() == y) {
            model.toggleFinalGuess()
        } else {
            model.setActiveX(x.toShort())
            model.setActiveY(y.toShort())
        }
        refreshState()
    }
    
    fun onInput(number: Int) {
        val model = keenModel ?: return
        val x = model.activeX
        val y = model.activeY
        
        if (x < 0 || y < 0) return 
        
        // Add current state to undo stack before modifying
        model.addCurToUndo(x, y)
        
        if (model.finalGuess) {
             model.clearGuesses(x, y) // Clear notes when entering value
             model.setCellFinalGuess(x, y, number)
        } else {
             model.clearFinal(x, y) // Clear value when entering notes
             model.addToCellGuesses(x, y, number)
        }
        
        model.puzzleWon()
        refreshState()
        saveAutoSave()

        // Trigger victory animation if solved
        if (model.puzzleWon && !_uiState.value.showVictoryAnimation) {
            stopTimer()  // Stop timer on victory
            // Record completion stats for analytics and UX tuning.
            statsManager?.recordPuzzleSolved(
                gridSize = model.size,
                solveTimeSeconds = _uiState.value.elapsedTimeSeconds,
                hintsUsed = _uiState.value.hintsUsed,
                difficulty = currentDifficulty
            )
            _uiState.update { it.copy(showVictoryAnimation = true) }
        }
    }

    fun onUndo() {
        val model = keenModel ?: return
        model.undoOneStep()
        refreshState()
        saveAutoSave()
    }
    
    fun toggleNoteMode() {
        val model = keenModel ?: return
        model.toggleFinalGuess()
        refreshState()
        saveAutoSave()
    }
    
    fun setLayoutPreset(preset: LayoutPreset) {
        _uiState.update { it.copy(layoutPreset = preset) }
    }
    
    fun toggleInfoDialog() {
        _uiState.update { it.copy(showInfoDialog = !it.showInfoDialog) }
    }

    fun dismissErrorDialog() {
        _uiState.update { it.copy(showErrorDialog = false, errorMessage = null) }
    }

    fun toggleSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = !it.showSettingsDialog) }
    }

    fun toggleDarkTheme() {
        _uiState.update { current ->
            val next = !current.darkTheme
            settingsPrefs?.edit {
                putBoolean(MenuActivity.DARK_MODE, next)
            }
            current.copy(darkTheme = next)
        }
    }

    fun setFontScale(scale: Float) {
        _uiState.update { it.copy(fontScale = scale.coerceIn(0.8f, 1.5f)) }
    }

    fun toggleShowTimer() {
        _uiState.update { it.copy(showTimer = !it.showTimer) }
    }

    // Immersive mode management
    private var uiAutoHideJob: Job? = null
    private val uiAutoHideDelay = 3000L  // 3 seconds

    fun toggleImmersiveMode() {
        val newImmersive = !_uiState.value.immersiveMode
        _uiState.update { it.copy(immersiveMode = newImmersive, uiVisible = true) }
        if (newImmersive) {
            startUiAutoHideTimer()
        } else {
            cancelUiAutoHideTimer()
        }
    }

    fun showUi() {
        _uiState.update { it.copy(uiVisible = true) }
        if (_uiState.value.immersiveMode) {
            startUiAutoHideTimer()
        }
    }

    fun hideUi() {
        if (_uiState.value.immersiveMode) {
            _uiState.update { it.copy(uiVisible = false) }
        }
    }

    fun toggleUiVisibility() {
        if (_uiState.value.immersiveMode) {
            val newVisible = !_uiState.value.uiVisible
            _uiState.update { it.copy(uiVisible = newVisible) }
            if (newVisible) {
                startUiAutoHideTimer()
            }
        }
    }

    private fun startUiAutoHideTimer() {
        cancelUiAutoHideTimer()
        uiAutoHideJob = viewModelScope.launch {
            delay(uiAutoHideDelay)
            hideUi()
        }
    }

    private fun cancelUiAutoHideTimer() {
        uiAutoHideJob?.cancel()
        uiAutoHideJob = null
    }

    // Call this on any user interaction to reset auto-hide timer
    fun onUserInteraction() {
        if (_uiState.value.immersiveMode && _uiState.value.uiVisible) {
            startUiAutoHideTimer()
        }
    }

    fun setColorblindMode(mode: ColorblindMode) {
        _uiState.update { it.copy(colorblindMode = mode) }
    }

    fun cycleColorblindMode() {
        val modes = ColorblindMode.entries
        val currentIndex = modes.indexOf(_uiState.value.colorblindMode)
        val nextIndex = (currentIndex + 1) % modes.size
        _uiState.update { it.copy(colorblindMode = modes[nextIndex]) }
    }

    fun onVictoryAnimationComplete() {
        _uiState.update { it.copy(victoryAnimationComplete = true) }
    }

    // Keyboard navigation support
    fun moveSelection(dx: Int, dy: Int) {
        val model = keenModel ?: return
        val size = model.size
        val currentX = if (model.activeX >= 0) model.activeX.toInt() else 0
        val currentY = if (model.activeY >= 0) model.activeY.toInt() else 0

        val newX = (currentX + dx).coerceIn(0, size - 1)
        val newY = (currentY + dy).coerceIn(0, size - 1)

        model.setActiveX(newX.toShort())
        model.setActiveY(newY.toShort())
        refreshState()
    }

    fun clearCell() {
        val model = keenModel ?: return
        val x = model.activeX
        val y = model.activeY

        if (x < 0 || y < 0) return

        model.addCurToUndo(x, y)
        model.clearFinal(x, y)
        model.clearGuesses(x, y)
        refreshState()
        saveAutoSave()
    }

    // Save/Load dialog toggles
    fun toggleSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = !it.showSaveDialog) }
    }

    fun toggleLoadDialog() {
        _uiState.update { it.copy(showLoadDialog = !it.showLoadDialog) }
    }

    // Get all save slots info
    fun getSaveSlots(): List<SaveSlotInfo> {
        return saveManager?.getAllSlotInfo() ?: emptyList()
    }

    // Save current game to slot
    fun saveToSlot(slotIndex: Int): Boolean {
        val model = keenModel ?: return false
        val manager = saveManager ?: return false

        val result = manager.saveToSlot(
            slotIndex = slotIndex,
            model = model,
            difficultyName = _uiState.value.difficultyName,
            profileName = currentProfile.name,
            elapsedSeconds = _uiState.value.elapsedTimeSeconds
        )

        if (result) {
            _uiState.update { it.copy(showSaveDialog = false) }
        }
        return result
    }

    private fun saveAutoSave() {
        val model = keenModel ?: return
        val manager = saveManager ?: return
        // Don't auto-save if solved (user might want to replay or it's done)
        // But actually, we DO want to save "solved" state so they don't lose the victory screen.
        manager.saveAutoSave(
            model = model,
            difficultyName = _uiState.value.difficultyName,
            profileName = currentProfile.name,
            elapsedSeconds = getElapsedTimeForSave()
        )
    }

    fun loadAutoSave(): Boolean {
        val manager = saveManager ?: return false
        val (model, elapsed, profileName) = manager.loadAutoSave()
        if (model != null) {
            // Restore implicit state
            val restoredProfile = KeenProfile.fromName(profileName)
            loadModel(model, preservedElapsedSeconds = elapsed, profile = restoredProfile)
            return true
        }
        return false
    }

    // Load game from slot
    fun loadFromSlot(slotIndex: Int): Boolean {
        val manager = saveManager ?: return false
        val (model, elapsed, profileName) = manager.loadFromSlot(slotIndex)

        if (model != null) {
            accumulatedTime = elapsed
            val restoredProfile = KeenProfile.fromName(profileName)
            loadModel(model, profile = restoredProfile)
            _uiState.update { it.copy(showLoadDialog = false) }
            return true
        }
        return false
    }

    // Delete a save slot
    fun deleteSlot(slotIndex: Int): Boolean {
        return saveManager?.deleteSlot(slotIndex) ?: false
    }

    // Hint action
    fun requestHint() {
        val model = keenModel ?: return
        model.ensureInitialized()
        val size = model.size
        val x = model.activeX.toInt()
        val y = model.activeY.toInt()

        if (x < 0 || y < 0) {
            // No cell selected
            _uiState.update {
                it.copy(
                    showHintDialog = true,
                    currentHint = HintInfo(
                        suggestedDigit = 0,
                        confidence = 0f,
                        reasoning = "Select a cell first to get a hint.",
                        cellX = -1, cellY = -1
                    )
                )
            }
            return
        }

        val cell = model.getCell(x.toShort(), y.toShort())
        if (cell.finalGuessValue != -1) {
            _uiState.update {
                it.copy(
                    showHintDialog = true,
                    currentHint = HintInfo(
                        suggestedDigit = cell.finalGuessValue,
                        confidence = 1.0f,
                        reasoning = "You already have ${cell.finalGuessValue} here.",
                        cellX = x, cellY = y
                    )
                )
            }
            return
        }

        val grid = buildGridArray(model)
        val hint = KeenHints.explain(
            size = size,
            cell = x * size + y,
            grid = grid,
            dsf = model.dsf,
            clues = model.clues,
            solution = null,
            modeFlags = currentGameMode.cFlags
        ) ?: KeenHints.getHint(
            size = size,
            grid = grid,
            dsf = model.dsf,
            clues = model.clues,
            solution = null,
            modeFlags = currentGameMode.cFlags
        )

        val hintX = hint?.col ?: x
        val hintY = hint?.row ?: y
        val suggestedDigit = hint?.value ?: 0
        val reasoning = hint?.explain()
            ?: "No hints available right now. Try using notes to eliminate candidates."
        val confidence = if (suggestedDigit > 0) 1.0f else 0.0f

        _uiState.update {
            it.copy(
                showHintDialog = true,
                currentHint = HintInfo(
                    suggestedDigit = suggestedDigit,
                    confidence = confidence,
                    reasoning = reasoning,
                    cellX = hintX,
                    cellY = hintY
                ),
                hintsUsed = it.hintsUsed + if (hint != null) 1 else 0
            )
        }
    }

    fun dismissHint() {
        _uiState.update { it.copy(showHintDialog = false, currentHint = null) }
    }

    fun applyHint() {
        val hint = _uiState.value.currentHint ?: return
        if (hint.suggestedDigit > 0 && hint.cellX >= 0) {
            val model = keenModel ?: return
            model.setActiveX(hint.cellX.toShort())
            model.setActiveY(hint.cellY.toShort())
            model.addCurToUndo(hint.cellX.toShort(), hint.cellY.toShort())
            model.setCellFinalGuess(hint.cellX.toShort(), hint.cellY.toShort(), hint.suggestedDigit)
            model.puzzleWon()
            refreshState()

            if (model.puzzleWon && !_uiState.value.showVictoryAnimation) {
                stopTimer()
                statsManager?.recordPuzzleSolved(
                    gridSize = model.size,
                    solveTimeSeconds = _uiState.value.elapsedTimeSeconds,
                    hintsUsed = _uiState.value.hintsUsed,
                    difficulty = currentDifficulty
                )
                _uiState.update { it.copy(showVictoryAnimation = true) }
            }
        }
        dismissHint()
    }

    // Expose stats for UI display (Phase 4b)
    fun getPlayerStats() = statsManager?.getStats()

}
