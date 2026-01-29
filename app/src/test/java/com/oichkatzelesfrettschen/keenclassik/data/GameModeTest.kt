/*
 * GameModeTest.kt: Unit tests for GameMode enum and mode filtering
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2026 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for GameMode enum functionality, including:
 * - Available modes filtering by profile
 * - Mode flags (cFlags) correctness
 * - Profile compatibility checks
 */
class GameModeTest {

    @Test
    fun `STANDARD mode has correct properties`() {
        val mode = GameMode.STANDARD
        assertEquals("Standard", mode.displayName)
        assertEquals(0x00, mode.cFlags)
        assertEquals(1, mode.phase)
        assertTrue(mode.implemented)
        assertEquals("calculate", mode.iconName)
    }

    @Test
    fun `MULTIPLICATION_ONLY mode has correct properties`() {
        val mode = GameMode.MULTIPLICATION_ONLY
        assertEquals("Multiplication Only", mode.displayName)
        assertEquals(0x01, mode.cFlags)
        assertEquals(1, mode.phase)
        assertTrue(mode.implemented)
        assertEquals("close", mode.iconName)
        assertNotNull(mode.extendedTip)
        assertTrue(mode.extendedTip!!.contains("multiplication"))
    }

    @Test
    fun `availableModes returns both modes for Classik profiles`() {
        val modernModes = GameMode.availableModes(KeenProfile.CLASSIK_MODERN)
        assertEquals(2, modernModes.size)
        assertTrue(modernModes.contains(GameMode.STANDARD))
        assertTrue(modernModes.contains(GameMode.MULTIPLICATION_ONLY))

        val legacyModes = GameMode.availableModes(KeenProfile.CLASSIK_LEGACY)
        assertEquals(2, legacyModes.size)
        assertTrue(legacyModes.contains(GameMode.STANDARD))
        assertTrue(legacyModes.contains(GameMode.MULTIPLICATION_ONLY))
    }

    @Test
    fun `availableModes without profile uses DEFAULT profile`() {
        val modes = GameMode.availableModes()
        val defaultModes = GameMode.availableModes(KeenProfile.DEFAULT)
        assertEquals(defaultModes, modes)
    }

    @Test
    fun `allModes returns all phase 1 modes`() {
        val allModes = GameMode.allModes()
        assertEquals(2, allModes.size)
        assertTrue(allModes.contains(GameMode.STANDARD))
        assertTrue(allModes.contains(GameMode.MULTIPLICATION_ONLY))
    }

    @Test
    fun `byPhase filters modes by implementation phase`() {
        val phase1Modes = GameMode.byPhase(1)
        assertEquals(2, phase1Modes.size)
        assertTrue(phase1Modes.all { it.phase == 1 })

        val phase2Modes = GameMode.byPhase(2)
        assertTrue(phase2Modes.isEmpty())
    }

    @Test
    fun `byPhase respects profile constraints`() {
        val phase1 = GameMode.byPhase(1, KeenProfile.CLASSIK_MODERN)
        assertEquals(2, phase1.size)
        assertTrue(phase1.contains(GameMode.STANDARD))
        assertTrue(phase1.contains(GameMode.MULTIPLICATION_ONLY))
    }

    @Test
    fun `isAvailable returns true for implemented Classik modes`() {
        assertTrue(GameMode.isAvailable(GameMode.STANDARD))
        assertTrue(GameMode.isAvailable(GameMode.MULTIPLICATION_ONLY))

        assertTrue(GameMode.isAvailable(GameMode.STANDARD, KeenProfile.CLASSIK_MODERN))
        assertTrue(GameMode.isAvailable(GameMode.MULTIPLICATION_ONLY, KeenProfile.CLASSIK_MODERN))
    }

    @Test
    fun `DEFAULT mode is STANDARD`() {
        assertEquals(GameMode.STANDARD, GameMode.DEFAULT)
    }

    @Test
    fun `mode cFlags are distinct`() {
        val modes = GameMode.allModes()
        val cFlags = modes.map { it.cFlags }.toSet()
        assertEquals(modes.size, cFlags.size, "Each mode should have unique cFlags")
    }

    @Test
    fun `MODE_MULT_ONLY flag is correctly set`() {
        val multOnlyFlag = 0x01
        assertEquals(multOnlyFlag, GameMode.MULTIPLICATION_ONLY.cFlags)

        // Verify it's different from STANDARD
        assertNotEquals(GameMode.STANDARD.cFlags, GameMode.MULTIPLICATION_ONLY.cFlags)
    }

    @Test
    fun `all phase 1 modes are marked as implemented`() {
        val phase1Modes = GameMode.byPhase(1)
        assertTrue(phase1Modes.all { it.implemented },
                   "All phase 1 modes should be implemented")
    }

    @Test
    fun `mode display names are distinct`() {
        val modes = GameMode.allModes()
        val names = modes.map { it.displayName }.toSet()
        assertEquals(modes.size, names.size, "Each mode should have unique display name")
    }
}
