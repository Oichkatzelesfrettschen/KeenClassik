package com.oichkatzelesfrettschen.keenclassik.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import com.oichkatzelesfrettschen.keenclassik.KeenModel

@RunWith(RobolectricTestRunner::class)
class SaveManagerProfileTest {

    private lateinit var context: Context
    private lateinit var saveManager: SaveManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        clearPrefs()
        saveManager = SaveManager(context)
    }

    @After
    fun tearDown() {
        clearPrefs()
    }

    @Test
    fun `save slot persists profile name`() {
        val model = buildModel(3)
        val profileName = KeenProfile.CLASSIK_LEGACY.name

        val saved = saveManager.saveToSlot(0, model, "Easy", profileName = profileName, elapsedSeconds = 42L)
        assertTrue(saved)

        val (loaded, elapsed, modeName, loadedProfile) = saveManager.loadFromSlot(0)
        assertNotNull(loaded)
        assertEquals(3, loaded!!.size)
        assertEquals(42L, elapsed)
        assertEquals(profileName, loadedProfile)
    }

    @Test
    fun `autosave persists profile name`() {
        val model = buildModel(4)
        val profileName = KeenProfile.CLASSIK_MODERN.name

        saveManager.saveAutoSave(model, "Normal", profileName = profileName, elapsedSeconds = 1337L)

        val (loaded, elapsed, modeName, loadedProfile) = saveManager.loadAutoSave()
        assertNotNull(loaded)
        assertEquals(4, loaded!!.size)
        assertEquals(1337L, elapsed)
        assertEquals(profileName, loadedProfile)
    }

    private fun clearPrefs() {
        context.getSharedPreferences("keenkenning_save_slots", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun buildModel(size: Int): KeenModel {
        val zones = Array(size * size) { idx ->
            KeenModel.Zone(KeenModel.Zone.Type.ADD, 1, idx)
        }
        val grid = Array(size) { x ->
            Array(size) { y ->
                val zone = zones[x * size + y]
                KeenModel.GridCell(0, zone)
            }
        }
        return KeenModel(size, zones, grid, IntArray(size * size), LongArray(size * size))
    }
}
