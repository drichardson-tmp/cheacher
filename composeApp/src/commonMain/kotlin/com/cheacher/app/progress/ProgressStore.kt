package com.cheacher.app.progress

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import okio.IOException

/**
 * The store's own condition, surfaced instead of swallowed. Cheacher's one promise is
 * that it remembers; a store that loses memory silently would break that promise in the
 * worst possible way, so the failure modes are values the UI can mention.
 */
data class StoreHealth(
    /** Persisted records that no longer decode. Quarantined on next write, never deleted. */
    val unreadableRecords: Int = 0,
    /** True once a write has failed even after retry; cleared by the next success. */
    val lastWriteFailed: Boolean = false,
) {
    val isHealthy: Boolean get() = unreadableRecords == 0 && !lastWriteFailed
}

/**
 * Where [TrainingRecord]s live between sessions.
 *
 * An interface with three members because that is all training needs: watch everything,
 * apply a pure transform to one repertoire's record, and know whether the disk is telling
 * the truth. A real database can slot in behind this later without the trainers noticing.
 */
interface ProgressStore {
    /** All records, keyed by repertoire id. Emits on every change. */
    val records: Flow<Map<String, TrainingRecord>>

    /** How the store is doing. Healthy forever for stores that cannot fail. */
    val health: Flow<StoreHealth> get() = flowOf(StoreHealth())

    /** Atomically rewrites one repertoire's record. Starts from [TrainingRecord.empty] if absent. */
    suspend fun update(repertoireId: String, transform: (TrainingRecord) -> TrainingRecord)
}

/**
 * DataStore-preferences implementation: one JSON blob per repertoire under a
 * `record.<id>` key. The JSON is versionable (unknown keys ignored) and the whole store
 * is one file per platform.
 *
 * Failure discipline, per the zero-silent-failures rule:
 * - A blob that no longer decodes is **quarantined**, not overwritten: the next write
 *   copies it to a `corrupt.<id>` key before starting that record fresh, so history is
 *   recoverable by a future version rather than destroyed by the present one.
 * - A failed write is retried once; a second failure raises the [health] flag and
 *   returns instead of throwing, because a crashed session loses *more* history than a
 *   skipped journal entry — the round itself lives in memory and keeps playing.
 */
class DataStoreProgressStore(
    private val dataStore: DataStore<Preferences>,
) : ProgressStore {
    private val json = Json { ignoreUnknownKeys = true }

    private val writeFailed = MutableStateFlow(false)
    private val unreadable = MutableStateFlow(0)

    override val records: Flow<Map<String, TrainingRecord>> =
        dataStore.data.map { preferences ->
            var broken = 0
            val decoded = preferences.asMap().entries.mapNotNull { (key, value) ->
                val id = key.name.removePrefix(RECORD_PREFIX)
                if (key.name == id || value !is String) return@mapNotNull null
                val record = decodeOrNull(value)
                if (record == null) {
                    broken++
                    null
                } else {
                    id to record
                }
            }.toMap()
            unreadable.value = broken
            decoded
        }

    override val health: Flow<StoreHealth> =
        combine(unreadable, writeFailed) { broken, failed ->
            StoreHealth(unreadableRecords = broken, lastWriteFailed = failed)
        }

    override suspend fun update(repertoireId: String, transform: (TrainingRecord) -> TrainingRecord) {
        try {
            editRecord(repertoireId, transform)
            writeFailed.value = false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            // One retry: transient IO (a busy filesystem, a mid-sync moment) is common
            // enough to be worth absorbing quietly.
            try {
                editRecord(repertoireId, transform)
                writeFailed.value = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                writeFailed.value = true
            }
        }
    }

    private suspend fun editRecord(repertoireId: String, transform: (TrainingRecord) -> TrainingRecord) {
        val key = stringPreferencesKey(RECORD_PREFIX + repertoireId)
        dataStore.edit { preferences ->
            val blob = preferences[key]
            val current = blob?.let(::decodeOrNull)
            if (blob != null && current == null) {
                // Quarantine before overwriting: history is the product.
                preferences[stringPreferencesKey(QUARANTINE_PREFIX + repertoireId)] = blob
            }
            preferences[key] = json.encodeToString(transform(current ?: TrainingRecord.empty(repertoireId)))
        }
    }

    private fun decodeOrNull(blob: String): TrainingRecord? =
        runCatching { json.decodeFromString<TrainingRecord>(blob) }.getOrNull()

    private companion object {
        const val RECORD_PREFIX = "record."
        const val QUARANTINE_PREFIX = "corrupt."
    }
}

/** In-memory store for tests and previews; same contract, no disk, permanently healthy. */
class InMemoryProgressStore : ProgressStore {
    private val state = MutableStateFlow<Map<String, TrainingRecord>>(emptyMap())

    override val records: Flow<Map<String, TrainingRecord>> = state

    override suspend fun update(repertoireId: String, transform: (TrainingRecord) -> TrainingRecord) {
        state.update { all ->
            all + (repertoireId to transform(all[repertoireId] ?: TrainingRecord.empty(repertoireId)))
        }
    }
}
