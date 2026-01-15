package com.oichkatzelesfrettschen.keenclassik.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeenProfileTest {

    private val originalConfig: FlavorConfig = FlavorConfigProvider.get()

    @After
    fun tearDown() {
        FlavorConfigProvider.set(originalConfig)
    }

    @Test
    fun `classik profiles expose difficulties 0-3`() {
        val levels = Difficulty.forGridSize(5, KeenProfile.CLASSIK_MODERN).map { it.level }
        assertEquals(listOf(0, 1, 2, 3), levels)
    }

    @Test
    fun `classik grid sizes stop at 9`() {
        FlavorConfigProvider.set(object : FlavorConfig {
            override val fullModeSet: Boolean = true
            override val minGridSize: Int = 3
            override val maxGridSize: Int = 16
        })
        val sizes = GridSize.allSizes(KeenProfile.CLASSIK_MODERN).map { it.size }
        assertTrue(9 in sizes)
        assertFalse(10 in sizes)
    }

    @Test
    fun `classik profile limits modes to standard`() {
        FlavorConfigProvider.set(object : FlavorConfig {
            override val fullModeSet: Boolean = true
            override val minGridSize: Int = 3
            override val maxGridSize: Int = 16
        })
        val modes = GameMode.availableModes(KeenProfile.CLASSIK_MODERN)
        assertEquals(listOf(GameMode.STANDARD), modes)
    }

    @Test
    fun `available profiles include modern and legacy only`() {
        val profiles = KeenProfile.availableProfiles()
        assertTrue(KeenProfile.CLASSIK_MODERN in profiles)
        assertTrue(KeenProfile.CLASSIK_LEGACY in profiles)
        assertEquals(listOf(KeenProfile.CLASSIK_MODERN, KeenProfile.CLASSIK_LEGACY), profiles)
    }

    @Test
    fun `unknown profile name defaults to classik modern`() {
        assertEquals(KeenProfile.DEFAULT, KeenProfile.fromName("NOT_A_PROFILE"))
    }

    @Test
    fun `difficulty clamping respects profile bounds`() {
        assertEquals(0, KeenProfile.CLASSIK_MODERN.clampDifficulty(-5))
        assertEquals(3, KeenProfile.CLASSIK_MODERN.clampDifficulty(6))
    }

    @Test
    fun `classik difficulty ranges stay capped across sizes`() {
        FlavorConfigProvider.set(object : FlavorConfig {
            override val fullModeSet: Boolean = true
            override val minGridSize: Int = 3
            override val maxGridSize: Int = 16
        })
        val expected = listOf(0, 1, 2, 3)
        GridSize.allSizes(KeenProfile.CLASSIK_MODERN).forEach { size ->
            assertEquals(expected, Difficulty.forGridSize(size.size, KeenProfile.CLASSIK_MODERN).map { it.level })
            assertEquals(expected, Difficulty.forGridSize(size.size, KeenProfile.CLASSIK_LEGACY).map { it.level })
        }
    }

}
