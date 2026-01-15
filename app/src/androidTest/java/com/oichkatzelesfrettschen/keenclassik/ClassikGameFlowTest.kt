package com.oichkatzelesfrettschen.keenclassik

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.oichkatzelesfrettschen.keenclassik.data.GameMode

@RunWith(AndroidJUnit4::class)
class ClassikGameFlowTest {
    companion object {
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
        Assume.assumeFalse("Classik-only UI flow", BuildConfig.ADVANCED_MODES_ENABLED)
        TestHooks.resetGameLoaded()
    }

    @Test
    fun startGameShowsGrid() {
        val scenario = ActivityScenario.launch(KeenActivity::class.java)
        try {
            waitForGameLoaded()
            scenario.onActivity { activity ->
                val uiState = activity.getViewModelForTests().uiState.value
                assertTrue(uiState.size > 0)
                assertTrue(uiState.cells.isNotEmpty())
                assertEquals(uiState.size, uiState.cells.size)
                assertEquals(uiState.size, uiState.cells.first().size)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun gameMetadataLoaded() {
        val scenario = ActivityScenario.launch(KeenActivity::class.java)
        try {
            waitForGameLoaded()
            scenario.onActivity { activity ->
                val uiState = activity.getViewModelForTests().uiState.value
                assertEquals(GameMode.STANDARD, uiState.gameMode)
                assertTrue(uiState.difficultyName.isNotBlank())
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun canEnterNumberInCell() {
        val scenario = ActivityScenario.launch(KeenActivity::class.java)
        try {
            waitForGameLoaded()
            scenario.onActivity { activity ->
                val viewModel = activity.getViewModelForTests()
                viewModel.onCellClicked(0, 0)
                if (viewModel.uiState.value.isInputtingNotes) {
                    viewModel.toggleNoteMode()
                }
                viewModel.onInput(1)
                val cell = viewModel.uiState.value.cells[0][0]
                assertEquals(1, cell.value)
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
