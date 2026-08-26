package com.cheacher.app.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
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
}
