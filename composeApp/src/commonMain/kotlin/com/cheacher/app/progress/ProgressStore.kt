package com.cheacher.app.progress

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

/**
 * Where [TrainingRecord]s live between sessions.
 *
 * An interface with two methods because that is all training needs: watch everything,
 * and apply a pure transform to one repertoire's record. A real database can slot in
 * behind this later without the trainers noticing.
 */
interface ProgressStore {
    /** All records, keyed by repertoire id. Emits on every change. */
    val records: Flow<Map<String, TrainingRecord>>

    /** Atomically rewrites one repertoire's record. Starts from [TrainingRecord.empty] if absent. */
    suspend fun update(repertoireId: String, transform: (TrainingRecord) -> TrainingRecord)
}

/**
 * DataStore-preferences implementation: one JSON blob per repertoire under a
 * `record.<id>` key. Crude but honest for a skeleton — the JSON is versionable
 * (unknown keys ignored) and the whole store is one file per platform.
 */
class DataStoreProgressStore(
    private val dataStore: DataStore<Preferences>,
) : ProgressStore {
    private val json = Json { ignoreUnknownKeys = true }

    override val records: Flow<Map<String, TrainingRecord>> =
        dataStore.data.map { preferences ->
            preferences.asMap().entries.mapNotNull { (key, value) ->
                val id = key.name.removePrefix(KEY_PREFIX)
                if (key.name == id || value !is String) return@mapNotNull null
                runCatching { json.decodeFromString<TrainingRecord>(value) }
                    .getOrNull()
                    ?.let { id to it }
            }.toMap()
        }

    override suspend fun update(repertoireId: String, transform: (TrainingRecord) -> TrainingRecord) {
        val key = stringPreferencesKey(KEY_PREFIX + repertoireId)
        dataStore.edit { preferences ->
            val current = preferences[key]
                ?.let { blob -> runCatching { json.decodeFromString<TrainingRecord>(blob) }.getOrNull() }
                ?: TrainingRecord.empty(repertoireId)
            preferences[key] = json.encodeToString(transform(current))
        }
    }

    private companion object {
        const val KEY_PREFIX = "record."
    }
}

/** In-memory store for tests and previews; same contract, no disk. */
class InMemoryProgressStore : ProgressStore {
    private val state = MutableStateFlow<Map<String, TrainingRecord>>(emptyMap())

    override val records: Flow<Map<String, TrainingRecord>> = state

    override suspend fun update(repertoireId: String, transform: (TrainingRecord) -> TrainingRecord) {
        state.update { all ->
            all + (repertoireId to transform(all[repertoireId] ?: TrainingRecord.empty(repertoireId)))
        }
    }
}
