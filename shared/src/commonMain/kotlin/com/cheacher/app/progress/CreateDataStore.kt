package com.cheacher.app.progress

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Absolute path for the preferences file. Android answers with its files dir (via a
 * holder set at startup), iOS with the documents directory. Must end in
 * `.preferences_pb` — DataStore checks.
 */
expect fun cheacherDataStorePath(): String

/**
 * Process-wide singleton. DataStore refuses two instances on one file, so the app gets
 * exactly one, lazily, and everyone shares it. Manual wiring instead of DI: it's one value.
 */
object ProgressStoreProvider {
    val store: ProgressStore by lazy {
        DataStoreProgressStore(
            PreferenceDataStoreFactory.createWithPath(produceFile = { cheacherDataStorePath().toPath() }),
        )
    }
}

/** Epoch millis now. Session timestamps only need wall-clock honesty, not monotonicity. */
@OptIn(ExperimentalTime::class)
fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
