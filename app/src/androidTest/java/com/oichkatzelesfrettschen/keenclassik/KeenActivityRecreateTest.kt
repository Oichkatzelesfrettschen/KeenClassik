package com.oichkatzelesfrettschen.keenclassik

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class KeenActivityRecreateTest {
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
        TestHooks.resetGameLoaded()
    }

    @Test
    fun recreateKeepsGridVisible() {
        val scenario = ActivityScenario.launch(KeenActivity::class.java)
        try {
            waitForGameLoaded()
            TestHooks.resetGameLoaded()
            scenario.recreate()
            waitForGameLoaded()
            scenario.onActivity { activity ->
                val uiState = activity.getViewModelForTests().uiState.value
                assertTrue(uiState.size > 0)
                assertTrue(uiState.cells.isNotEmpty())
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
