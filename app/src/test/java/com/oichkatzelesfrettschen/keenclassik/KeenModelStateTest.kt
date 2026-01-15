/*
 * KeenModelStateTest.kt: Robolectric tests for KeenModel state transitions
 *
 * Tests core game state management including:
 * - Cell selection and active cell tracking
 * - Guess/note mode toggling
 * - Undo stack operations
 * - Puzzle completion detection
 * - Serialization/deserialization recovery
 *
 * SPDX-License-Identifier: MIT
 */

package com.oichkatzelesfrettschen.keenclassik

import com.oichkatzelesfrettschen.keenclassik.KeenModel.GridCell
import com.oichkatzelesfrettschen.keenclassik.KeenModel.Zone
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based tests for KeenModel state transitions.
 * Uses Android test runner to enable Context-dependent operations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class KeenModelStateTest {

    private lateinit var model: KeenModel
    private lateinit var zones: Array<Zone>
    private lateinit var grid: Array<Array<GridCell>>
    private val size = 3

    @Before
    fun setUp() {
        // Create a simple 3x3 puzzle with 3 zones
        zones = arrayOf(
            Zone(Zone.Type.ADD, 6, 0),   // Zone 0: sum = 6
            Zone(Zone.Type.TIMES, 2, 1), // Zone 1: product = 2
            Zone(Zone.Type.MINUS, 1, 2)  // Zone 2: difference = 1
        )

        // Create grid: zone assignments and expected values
        grid = Array(size) { x ->
            Array(size) { y ->
                val zoneId = when {
                    x == 0 && y == 0 -> 0  // Zone 0
                    x == 0 && y == 1 -> 0
                    x == 0 && y == 2 -> 0
                    x == 1 && y == 0 -> 1  // Zone 1
                    x == 1 && y == 1 -> 1
                    x == 2 && y == 0 -> 2  // Zone 2
                    else -> 2
                }
                // Expected values form a valid Latin square
                val expected = ((x + y) % size) + 1
                GridCell(expected, zones[zoneId])
            }
        }

        model = KeenModel(size, zones, grid, null, null)
    }

    // ===== Initialization Tests =====

    @Test
    fun `constructor sets default state correctly`() {
        assertEquals(size, model.size)
        assertFalse(model.puzzleWon)
        assertTrue(model.finalGuess) // Default is final guess mode
        assertFalse(model.hasActiveCords())
        assertEquals(-1, model.activeX.toInt())
        assertEquals(-1, model.activeY.toInt())
    }

    @Test
    fun `grid cells initialized with correct expected values`() {
        for (x in 0 until size) {
            for (y in 0 until size) {
                val cell = model.getCell(x.toShort(), y.toShort())
                val expected = ((x + y) % size) + 1
                assertEquals("Cell ($x,$y) expected value", expected, cell.expectedValue)
                assertEquals(-1, cell.finalGuessValue) // No guess yet
            }
        }
    }

    @Test
    fun `zones array accessible`() {
        val retrievedZones = model.gameZones
        assertEquals(3, retrievedZones.size)
        assertEquals(Zone.Type.ADD, retrievedZones[0].zoneType)
        assertEquals(6, retrievedZones[0].expectedValue)
    }

    // ===== Active Cell Selection Tests =====

    @Test
    fun `setActiveX and setActiveY update active cell`() {
        model.setActiveX(1)
        model.setActiveY(2)

        assertTrue(model.hasActiveCords())
        assertEquals(1, model.activeX.toInt())
        assertEquals(2, model.activeY.toInt())
    }

    @Test
    fun `hasActiveCords returns false when no cell selected`() {
        assertFalse(model.hasActiveCords())
    }

    @Test
    fun `hasActiveCords returns true when cell selected`() {
        model.setActiveX(0)
        model.setActiveY(0)
        assertTrue(model.hasActiveCords())
    }

    // ===== Final Guess Mode Tests =====

    @Test
    fun `setCellFinalGuess sets and toggles value`() {
        // Use value within grid size (3x3 grid, valid values 1-3)
        model.setCellFinalGuess(0, 0, 2)
        assertEquals(2, model.getCell(0, 0).finalGuessValue)

        // Same value again should clear
        model.setCellFinalGuess(0, 0, 2)
        assertEquals(-1, model.getCell(0, 0).finalGuessValue)
    }

    @Test
    fun `setCellFinalGuess respects size bounds`() {
        // Values outside 1..size should be rejected
        model.setCellFinalGuess(0, 0, 0) // Below range
        assertEquals(-1, model.getCell(0, 0).finalGuessValue)

        model.setCellFinalGuess(0, 0, size + 1) // Above range
        assertEquals(-1, model.getCell(0, 0).finalGuessValue)

        // Valid value should work
        model.setCellFinalGuess(0, 0, size)
        assertEquals(size, model.getCell(0, 0).finalGuessValue)
    }

    @Test
    fun `clearFinal clears final guess value`() {
        model.setCellFinalGuess(1, 1, 3)
        assertEquals(3, model.getCell(1, 1).finalGuessValue)

        model.clearFinal(1, 1)
        assertEquals(-1, model.getCell(1, 1).finalGuessValue)
    }

    // ===== Notes/Guesses Mode Tests =====

    @Test
    fun `addToCellGuesses toggles note on and off`() {
        val cell = model.getCell(0, 0)

        // Initially all false
        assertFalse(cell.guesses[0])

        // Add note for digit 1 (index 0)
        model.addToCellGuesses(0, 0, 1)
        assertTrue(cell.guesses[0])

        // Toggle off
        model.addToCellGuesses(0, 0, 1)
        assertFalse(cell.guesses[0])
    }

    @Test
    fun `addToCellGuesses respects size bounds`() {
        val cell = model.getCell(0, 0)

        // Values outside 1..size should be rejected
        model.addToCellGuesses(0, 0, 0) // Below range
        assertFalse(cell.guesses.any { it })

        model.addToCellGuesses(0, 0, size + 1) // Above range
        assertFalse(cell.guesses.any { it })
    }

    @Test
    fun `clearGuesses resets all notes`() {
        val cell = model.getCell(0, 0)

        // Set several notes
        model.addToCellGuesses(0, 0, 1)
        model.addToCellGuesses(0, 0, 2)
        model.addToCellGuesses(0, 0, 3)

        assertTrue(cell.guesses[0])
        assertTrue(cell.guesses[1])
        assertTrue(cell.guesses[2])

        model.clearGuesses(0, 0)
        assertFalse(cell.guesses.any { it })
    }

    // ===== Input Mode Toggle Tests =====

    @Test
    fun `toggleFinalGuess switches between modes`() {
        assertTrue(model.finalGuess) // Default

        model.toggleFinalGuess()
        assertFalse(model.finalGuess) // Now in notes mode

        model.toggleFinalGuess()
        assertTrue(model.finalGuess) // Back to final guess mode
    }

    @Test
    fun `getFinalGuess returns current mode`() {
        assertEquals(true, model.finalGuess)

        model.toggleFinalGuess()
        assertEquals(false, model.finalGuess)
    }

    // ===== Undo Stack Tests =====

    @Test
    fun `undo restores previous final guess state`() {
        // Set a final guess (use value within grid size 1-3)
        model.addCurToUndo(0, 0) // Save empty state
        model.setCellFinalGuess(0, 0, 2)

        assertEquals(2, model.getCell(0, 0).finalGuessValue)

        // Undo should restore to empty
        model.undoOneStep()
        assertEquals(-1, model.getCell(0, 0).finalGuessValue)
    }

    @Test
    fun `undo restores previous notes state`() {
        val cell = model.getCell(0, 0)

        // Set some notes
        model.addToCellGuesses(0, 0, 1)
        model.addToCellGuesses(0, 0, 2)

        // Save state before modification
        model.addCurToUndo(0, 0)

        // Modify notes
        model.addToCellGuesses(0, 0, 3)
        assertTrue(cell.guesses[2])

        // Undo should restore previous state (1 and 2 only)
        model.undoOneStep()
        assertTrue(cell.guesses[0])
        assertTrue(cell.guesses[1])
        assertFalse(cell.guesses[2])
    }

    @Test
    fun `undoOneStep does nothing when stack empty`() {
        // Should not crash
        model.undoOneStep()

        // State should be unchanged
        assertEquals(-1, model.getCell(0, 0).finalGuessValue)
    }

    @Test
    fun `multiple undos work correctly`() {
        val cell = model.getCell(1, 1)

        // State 1: empty
        model.addCurToUndo(1, 1)
        model.setCellFinalGuess(1, 1, 1)

        // State 2: value = 1
        model.addCurToUndo(1, 1)
        model.setCellFinalGuess(1, 1, 1) // Clear
        model.setCellFinalGuess(1, 1, 2) // Set to 2

        assertEquals(2, cell.finalGuessValue)

        // First undo -> back to 1
        model.undoOneStep()
        assertEquals(1, cell.finalGuessValue)

        // Second undo -> back to empty
        model.undoOneStep()
        assertEquals(-1, cell.finalGuessValue)
    }

    // ===== Puzzle Completion Tests =====

    @Test
    fun `puzzleWon returns false for incomplete puzzle`() {
        assertFalse(model.puzzleWon)

        model.puzzleWon() // Check completion
        assertFalse(model.puzzleWon)
    }

    @Test
    fun `puzzleWon returns true when all cells correct`() {
        // Fill all cells with correct values
        for (x in 0 until size) {
            for (y in 0 until size) {
                val expected = ((x + y) % size) + 1
                model.setCellFinalGuess(x.toShort(), y.toShort(), expected)
            }
        }

        model.puzzleWon()
        assertTrue(model.puzzleWon)
    }

    @Test
    fun `puzzleWon clears active cell on win`() {
        // Set active cell
        model.setActiveX(1)
        model.setActiveY(1)
        assertTrue(model.hasActiveCords())

        // Complete puzzle
        for (x in 0 until size) {
            for (y in 0 until size) {
                val expected = ((x + y) % size) + 1
                model.setCellFinalGuess(x.toShort(), y.toShort(), expected)
            }
        }

        model.puzzleWon()
        assertTrue(model.puzzleWon)
        assertFalse(model.hasActiveCords()) // Active cell cleared
    }

    @Test
    fun `puzzleWon returns false with one wrong cell`() {
        // Fill almost all cells correctly
        for (x in 0 until size) {
            for (y in 0 until size) {
                val expected = ((x + y) % size) + 1
                model.setCellFinalGuess(x.toShort(), y.toShort(), expected)
            }
        }

        // Make one cell wrong
        model.setCellFinalGuess(0, 0, 0) // Clear
        val wrongValue = if (model.getCell(0, 0).expectedValue == 1) 2 else 1
        model.setCellFinalGuess(0, 0, wrongValue)

        model.puzzleWon()
        assertFalse(model.puzzleWon)
    }

    // ===== Serialization Recovery Tests =====

    @Test
    fun `ensureInitialized creates missing dsf array`() {
        // Model created with null dsf
        assertNull(model.dsf)

        model.ensureInitialized()

        assertNotNull(model.dsf)
        assertEquals(size * size, model.dsf.size)
    }

    @Test
    fun `ensureInitialized creates missing clues array`() {
        // Model created with null clues
        assertNull(model.clues)

        model.ensureInitialized()

        assertNotNull(model.clues)
        assertEquals(size * size, model.clues.size)
    }

    @Test
    fun `ensureInitialized is idempotent`() {
        model.ensureInitialized()
        val dsf1 = model.dsf
        val clues1 = model.clues

        model.ensureInitialized()

        // Should be same arrays (not recreated)
        assertArrayEquals(dsf1, model.dsf)
        assertArrayEquals(clues1, model.clues)
    }

    // ===== Zone String Representation Tests =====

    @Test
    fun `zone toString formats correctly for ADD`() {
        val zone = Zone(Zone.Type.ADD, 10, 0)
        assertEquals("10 +", zone.toString())
    }

    @Test
    fun `zone toString formats correctly for MINUS`() {
        val zone = Zone(Zone.Type.MINUS, 3, 0)
        assertEquals("3 -", zone.toString())
    }

    @Test
    fun `zone toString formats correctly for TIMES`() {
        val zone = Zone(Zone.Type.TIMES, 24, 0)
        assertEquals("24 x", zone.toString())
    }

    @Test
    fun `zone toString formats correctly for DIVIDE`() {
        val zone = Zone(Zone.Type.DIVIDE, 2, 0)
        assertEquals("2 /", zone.toString())
    }

    // ===== GridCell Tests =====

    @Test
    fun `gridCell initializes with correct defaults`() {
        val cell = GridCell(5, zones[0])

        assertEquals(5, cell.expectedValue)
        assertEquals(-1, cell.finalGuessValue)
        assertEquals(KeenModel.MAX_SIZE, cell.guesses.size)
        assertFalse(cell.guesses.any { it })
        assertEquals(zones[0], cell.zone)
    }

    @Test
    fun `gridCell getters return correct values`() {
        val cell = GridCell(3, zones[1])

        assertEquals(-1, cell.finalGuessValue)
        assertNotNull(cell.guesses)
        assertEquals(zones[1], cell.zone)
    }
}
