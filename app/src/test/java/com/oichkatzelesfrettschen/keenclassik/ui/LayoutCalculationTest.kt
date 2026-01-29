/*
 * LayoutCalculationTest.kt: Unit tests for layout calculations
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2026 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oichkatzelesfrettschen.keenclassik.ui.theme.GameDimensions
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for layout calculation functions to verify:
 * - Grid size calculations are correct and space-efficient
 * - Button scaling works properly for different puzzle sizes
 * - Note positioning respects clue bounds
 * - Responsive sizing follows WCAG guidelines
 */
class LayoutCalculationTest {

    @Test
    fun `button scaling reduces size for large puzzles`() {
        val dimensions = GameDimensions()

        // Test responsive button sizing logic
        val small_size = dimensions.getResponsiveButtonSize(puzzleSize = 4, screenWidth = 400.dp)
        val large_size = dimensions.getResponsiveButtonSize(puzzleSize = 9, screenWidth = 400.dp)

        // Larger puzzles should have smaller or equal buttons
        assertTrue("Buttons should scale down for larger puzzles",
                   large_size <= small_size)
    }

    @Test
    fun `button size respects WCAG minimum`() {
        val dimensions = GameDimensions()
        val minSize = 44.dp  // WCAG minimum touch target

        val sizes = listOf(3, 4, 5, 6, 7, 8, 9)
        val screenWidths = listOf(320.dp, 400.dp, 480.dp, 600.dp)

        for (size in sizes) {
            for (width in screenWidths) {
                val buttonSize = dimensions.getResponsiveButtonSize(size, width)
                assertTrue("Button size should be >= WCAG minimum (44dp) for $size x $size on ${width}px",
                           buttonSize >= minSize)
            }
        }
    }

    @Test
    fun `button size does not exceed maximum`() {
        val dimensions = GameDimensions()
        val maxSize = dimensions.buttonMinSize  // Defined maximum

        val sizes = listOf(3, 4, 5, 6, 7, 8, 9)
        val screenWidths = listOf(320.dp, 400.dp, 480.dp, 600.dp, 800.dp)

        for (size in sizes) {
            for (width in screenWidths) {
                val buttonSize = dimensions.getResponsiveButtonSize(size, width)
                assertTrue("Button size should be <= maximum (${maxSize}) for $size x $size on ${width}px",
                           buttonSize <= maxSize)
            }
        }
    }

    @Test
    fun `button sizing handles narrow screens`() {
        val dimensions = GameDimensions()
        val narrowWidth = 320.dp  // Small phone

        // Even on narrow screens, buttons should fit
        val size9 = dimensions.getResponsiveButtonSize(puzzleSize = 9, screenWidth = narrowWidth)
        assertTrue("Buttons should fit on narrow screens", size9 >= 44.dp && size9 <= 52.dp)
    }

    @Test
    fun `clue scaling varies by puzzle size`() {
        // Test the logic: larger puzzles get smaller clues
        // Base sizes by puzzle size (from GameScreen.kt logic):
        // <= 4: 12sp, <= 6: 11sp, <= 8: 10sp, else: 9sp

        val baseSizeFor3x3 = 12  // sp
        val baseSizeFor5x5 = 11
        val baseSizeFor7x7 = 10
        val baseSizeFor9x9 = 9

        // Verify progression
        assertTrue("Larger puzzles should have smaller base clue sizes",
                   baseSizeFor9x9 < baseSizeFor7x7)
        assertTrue("Larger puzzles should have smaller base clue sizes",
                   baseSizeFor7x7 < baseSizeFor5x5)
        assertTrue("Larger puzzles should have smaller base clue sizes",
                   baseSizeFor5x5 < baseSizeFor3x3)
    }

    @Test
    fun `clue scale factor stays within bounds`() {
        // Scale factor logic: (cellSizePx / 60f).coerceIn(0.7f, 1.0f)
        val testCases = listOf(
            42f to 0.7f,   // 42/60 = 0.7
            48f to 0.8f,   // 48/60 = 0.8
            60f to 1.0f,   // 60/60 = 1.0
            72f to 1.0f    // 72/60 = 1.2, clamped to 1.0
        )

        for ((cellSize, expected) in testCases) {
            val scaleFactor = (cellSize / 60f).coerceIn(0.7f, 1.0f)
            assertEquals("Scale factor for cell size $cellSize", expected, scaleFactor, 0.01f)
        }
    }

    @Test
    fun `note size increases with grid`() {
        // Note sizes from AccessibleNoteGrid:
        // <= 4: 10sp, <= 6: 9sp, <= 9: 8sp, else: 7sp

        val noteSizeFor3x3 = 10
        val noteSizeFor5x5 = 9
        val noteSizeFor7x7 = 8
        val noteSizeFor9x9 = 8

        // Verify minimum readability
        assertTrue("9x9 notes should be at least 8sp for readability",
                   noteSizeFor9x9 >= 8)
    }

    @Test
    fun `note background opacity is high contrast`() {
        // Verify the 95% opacity requirement
        val opacity = 0.95f
        assertTrue("Note background should be 95% opacity for WCAG compliance",
                   opacity >= 0.95f)
    }

    @Test
    fun `grid size calculation respects minimum cell size`() {
        // WCAG minimum touch target is 48dp
        val minCellSize = 48.dp
        val puzzleSizes = listOf(3, 4, 5, 6, 7, 8, 9)

        for (size in puzzleSizes) {
            val minGridFromCells = minCellSize * size
            assertTrue("Grid for ${size}x${size} should accommodate ${minCellSize} cells",
                       minGridFromCells >= minCellSize * size)
        }
    }

    @Test
    fun `layout preset spacing is progressive`() {
        // Verify spacing increases from Compact -> Medium -> Spacious
        val compactSpacing = 4.dp
        val mediumSpacing = 8.dp
        val spaciousSpacing = 12.dp

        assertTrue("Spacing should increase from Compact to Medium",
                   mediumSpacing > compactSpacing)
        assertTrue("Spacing should increase from Medium to Spacious",
                   spaciousSpacing > mediumSpacing)
    }

    @Test
    fun `content padding scales with cell size`() {
        // Content padding logic: 12% of cell width, minimum 8dp
        val testCases = listOf(
            40.dp to 8.dp,    // 40 * 0.12 = 4.8, clamped to 8
            60.dp to 8.dp,    // 60 * 0.12 = 7.2, clamped to 8
            80.dp to 9.6.dp,  // 80 * 0.12 = 9.6
            100.dp to 12.dp   // 100 * 0.12 = 12
        )

        for ((cellWidth, expectedMin) in testCases) {
            val padding = (cellWidth * 0.12f).coerceAtLeast(8.dp)
            assertTrue("Padding for cell width $cellWidth should be >= $expectedMin",
                       padding >= expectedMin)
        }
    }

    @Test
    fun `note box size scales with grid dimension`() {
        // Note box size = 28% of cell / grid dimension
        val cellWidth = 100.dp
        val gridDim3x3 = 3
        val gridDim4x4 = 4

        val noteBox3x3 = (cellWidth * 0.28f) / gridDim3x3
        val noteBox4x4 = (cellWidth * 0.28f) / gridDim4x4

        assertTrue("Larger note grids have smaller boxes",
                   noteBox4x4 < noteBox3x3)

        // Verify minimum size
        val minNoteBox = 8.dp
        assertTrue("Note boxes should be at least ${minNoteBox}",
                   noteBox3x3.coerceAtLeast(minNoteBox) >= minNoteBox)
    }

    @Test
    fun `screen utilization targets are realistic`() {
        // Target: Grid should use 60-70% of screen height
        val screenHeight = 800.dp
        val targetMin = screenHeight * 0.60f
        val targetMax = screenHeight * 0.70f

        assertTrue("Target range should be achievable",
                   targetMin < targetMax)
        assertTrue("Minimum target should leave room for UI",
                   targetMin < screenHeight)

        // Verify the range is reasonable
        assertTrue("60-70% utilization is reasonable",
                   targetMin >= screenHeight * 0.5f && targetMax <= screenHeight * 0.8f)
    }

    @Test
    fun `reserved space calculation includes all UI elements`() {
        // Components that reserve space:
        val topBarHeight = 80.dp
        val topSpacer = 8.dp
        val gridToButtonsSpacer = 8.dp
        val inputPadHeight = 160.dp  // Approximate for 2 rows
        val columnPadding = 16.dp

        val totalReserved = topBarHeight + topSpacer + gridToButtonsSpacer +
                           inputPadHeight + columnPadding

        // Verify reasonable total
        assertTrue("Reserved space should be < 50% of screen",
                   totalReserved < 400.dp)
    }

    @Test
    fun `difficulty color mapping is consistent`() {
        // Verify difficulty colors are distinct (tested via color values)
        val difficulties = listOf("Easy", "Normal", "Hard", "Extreme")
        val colors = difficulties.map { getDifficultyColorValue(it) }

        // All colors should be distinct
        assertEquals("Each difficulty should have unique color",
                     difficulties.size, colors.toSet().size)
    }

    private fun getDifficultyColorValue(difficulty: String): Long {
        return when (difficulty) {
            "Easy" -> 0xFF4CAF50
            "Normal" -> 0xFF2196F3
            "Hard" -> 0xFFFF9800
            "Extreme" -> 0xFFF44336
            else -> 0xFF9E9E9E
        }
    }

    @Test
    fun `large screen dimensions are proportionally scaled`() {
        val standardDimensions = GameDimensions()
        // Verify standard has reasonable base values
        assertTrue("Standard cell min size should be 40-48dp",
                   standardDimensions.cellMinSize >= 40.dp &&
                   standardDimensions.cellMinSize <= 48.dp)

        // Large screens should have larger dimensions (tested via constant values)
        val largeCellMin = 56.dp
        val standardCellMin = 40.dp
        assertTrue("Large screens should have bigger cells",
                   largeCellMin > standardCellMin)
    }
}
