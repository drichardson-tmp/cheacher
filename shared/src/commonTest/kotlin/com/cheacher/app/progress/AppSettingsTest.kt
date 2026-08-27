package com.cheacher.app.progress

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.cheacher.app.training.MistakePolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsTest {
    @Test
    fun hapticsDefaultOnAndCanBeDisabled() = runTest {
        val store = InMemoryProgressStore()

        assertTrue(store.settings.first().hapticsEnabled)

        store.updateSettings { it.copy(hapticsEnabled = false) }

        assertFalse(store.settings.first().hapticsEnabled)
    }

    @Test
    fun recallChoicesDefaultToCoachPlanAndStrictTwoSidedPlay() = runTest {
        val settings = InMemoryProgressStore().settings.first()

        assertEquals(MistakePolicy.STRICT, settings.mistakePolicy)
        assertFalse(settings.oneSided)
        assertFalse(settings.fullTree)
    }

    @Test
    fun allSettingsSurviveReopeningTheDataStoreBackedStore() = runTest {
        val dataStore = MemoryPreferencesDataStore()
        val firstLaunch = DataStoreProgressStore(dataStore)

        firstLaunch.updateSettings {
            AppSettings(
                hapticsEnabled = false,
                mistakePolicy = MistakePolicy.ONE_ALLOWANCE,
                oneSided = true,
                fullTree = true,
            )
        }

        val reopened = DataStoreProgressStore(dataStore)
        assertEquals(
            AppSettings(
                hapticsEnabled = false,
                mistakePolicy = MistakePolicy.ONE_ALLOWANCE,
                oneSided = true,
                fullTree = true,
            ),
            reopened.settings.first(),
        )
    }
}

/** The DataStore contract in memory, so the preference-key round trip is under test. */
private class MemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
