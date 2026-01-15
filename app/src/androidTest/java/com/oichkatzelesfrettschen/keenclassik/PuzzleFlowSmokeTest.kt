package com.oichkatzelesfrettschen.keenclassik

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class PuzzleFlowSmokeTest {
    private companion object {
        @JvmStatic
        @BeforeClass
        fun enableInstrumentationFlag() {
            System.setProperty("keen.instrumentation", "true")
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences(
                context.packageName + "_preferences",
                Context.MODE_PRIVATE
            ).edit().putBoolean(TestEnvironment.INSTRUMENTATION_PREF_KEY, true).apply()
            TestEnvironment.primeFromContext(context)
        }
    }

    @Before
    fun setUp() {
        TestHooks.resetGameLoaded()
    }

    @Test
    fun fullPuzzleFlowFromMenuToGameAndBack() {
        val scenario = ActivityScenario.launch(KeenActivity::class.java)
        try {
            waitForGameLoaded()
            scenario.onActivity { activity ->
                val viewModel = activity.getViewModelForTests()
                requireNotNull(viewModel.getModel()) { "Game model not loaded" }
                val targetCell = requireNotNull(
                    viewModel.uiState.value.cells.flatten().firstOrNull { it.value == null }
                ) { "No empty cell found in puzzle" }
                viewModel.onCellClicked(targetCell.x, targetCell.y)
                if (viewModel.uiState.value.isInputtingNotes) {
                    viewModel.toggleNoteMode()
                }
                viewModel.onInput(1)
                val cellAfterInput = viewModel.uiState.value.cells[targetCell.x][targetCell.y]
                assertEquals(1, cellAfterInput.value)
                viewModel.onUndo()
                val cellAfterUndo = viewModel.uiState.value.cells[targetCell.x][targetCell.y]
                assertTrue(cellAfterUndo.value == null || cellAfterUndo.value == 0)
            }
        } finally {
            scenario.close()
        }
    }

    private fun waitForGameLoaded(timeoutMillis: Long = 15000L) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (TestHooks.isGameLoaded()) return
            Thread.sleep(50)
        }
        error("Game did not load within ${timeoutMillis}ms")
    }
}
