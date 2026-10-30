package com.aram.mahakama.data

import android.content.Context
import java.io.IOException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONObject

private val Context.mahakamaDataStore by preferencesDataStore(name = "mahakama_preferences")

/** Persisted, device-local profile and trial state. */
data class UserState(
    val userName: String = "",
    val ambientMuted: Boolean = false,
    val selectedMusicTrack: String = "death",
    val hasChosenMusicTrack: Boolean = false,
    val musicVolume: Float = 0.82f,
    val orchestraIntroCompleted: Boolean = false,
    val trialStarted: Boolean = false,
    val totalActiveMillis: Long = 0L,
    val answers: Map<String, String> = emptyMap(),
    val answerTimestamps: Map<String, Long> = emptyMap(),
    val freeWriting: String = "",
    val freeWritingFaceId: String = "",
    val unmaskingFeedback: String = ""
)

class PreferencesRepository private constructor(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.mahakamaDataStore)

    /** Isolated store name for instrumentation tests; production uses the single app-level store. */
    internal constructor(context: Context, storageName: String) : this(
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { context.applicationContext.preferencesDataStoreFile(storageName) }
        )
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state = store.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map(::toUserState)
        .stateIn(scope, SharingStarted.Eagerly, UserState())

    suspend fun saveUserName(value: String) {
        store.edit { it[Keys.USER_NAME] = value.trim() }
    }

    suspend fun setAmbientMuted(muted: Boolean) {
        store.edit { it[Keys.AMBIENT_MUTED] = muted }
    }

    suspend fun setSelectedMusicTrack(trackId: String) {
        val safeTrack = when (trackId) {
            "death", "sin", "fate" -> trackId
            else -> "death"
        }
        store.edit {
            it[Keys.SELECTED_MUSIC_TRACK] = safeTrack
            it[Keys.HAS_CHOSEN_MUSIC_TRACK] = true
        }
    }

    suspend fun setMusicVolume(volume: Float) {
        store.edit { it[Keys.MUSIC_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setOrchestraIntroCompleted(completed: Boolean) {
        store.edit { it[Keys.ORCHESTRA_INTRO_COMPLETED] = completed }
    }

    suspend fun markTrialStarted() {
        store.edit { it[Keys.TRIAL_STARTED] = true }
    }

    suspend fun addActiveTime(millis: Long) {
        if (millis <= 0L) return
        store.edit { prefs ->
            val previous = prefs[Keys.TOTAL_ACTIVE_MILLIS] ?: 0L
            prefs[Keys.TOTAL_ACTIVE_MILLIS] = previous + millis
        }
    }

    /**
     * Atomically replaces the selected branch history. Answers on a branch abandoned by a new
     * choice are removed so they cannot affect future reflections or the echo timeline.
     */
    suspend fun replaceAnswers(activeAnswers: Map<String, String>, changedQuestionId: String, selectedOptionId: String) {
        store.edit { prefs ->
            val previousTimes = parseLongMap(prefs[Keys.ANSWER_TIMESTAMPS_JSON].orEmpty())
            val normalized = activeAnswers.toMutableMap().apply { put(changedQuestionId, selectedOptionId) }
            val retainedTimes = previousTimes.filterKeys { it in normalized }.toMutableMap()
            retainedTimes[changedQuestionId] = System.currentTimeMillis()
            prefs[Keys.ANSWERS_JSON] = encodeAnswers(normalized)
            prefs[Keys.ANSWER_TIMESTAMPS_JSON] = encodeLongMap(retainedTimes)
        }
    }

    /** Backward-compatible single-answer update for future callers. */
    suspend fun saveAnswer(questionId: String, optionId: String) {
        val previous = state.value.answers.toMutableMap()
        previous[questionId] = optionId
        replaceAnswers(previous, questionId, optionId)
    }

    suspend fun saveFreeWriting(text: String, linkedFaceId: String) {
        store.edit {
            it[Keys.FREE_WRITING] = text
            it[Keys.FREE_WRITING_FACE_ID] = linkedFaceId
        }
    }

    suspend fun saveUnmaskingFeedback(feedback: String) {
        store.edit { it[Keys.UNMASKING_FEEDBACK] = feedback }
    }

    private fun toUserState(prefs: Preferences): UserState = UserState(
        userName = prefs[Keys.USER_NAME].orEmpty(),
        ambientMuted = prefs[Keys.AMBIENT_MUTED] ?: false,
        selectedMusicTrack = prefs[Keys.SELECTED_MUSIC_TRACK] ?: "death",
        hasChosenMusicTrack = prefs[Keys.HAS_CHOSEN_MUSIC_TRACK] ?: false,
        musicVolume = (prefs[Keys.MUSIC_VOLUME] ?: 0.82f).coerceIn(0f, 1f),
        orchestraIntroCompleted = prefs[Keys.ORCHESTRA_INTRO_COMPLETED] ?: false,
        trialStarted = prefs[Keys.TRIAL_STARTED] ?: false,
        totalActiveMillis = prefs[Keys.TOTAL_ACTIVE_MILLIS] ?: 0L,
        answers = parseAnswers(prefs[Keys.ANSWERS_JSON].orEmpty()),
        answerTimestamps = parseLongMap(prefs[Keys.ANSWER_TIMESTAMPS_JSON].orEmpty()),
        freeWriting = prefs[Keys.FREE_WRITING].orEmpty(),
        freeWritingFaceId = prefs[Keys.FREE_WRITING_FACE_ID].orEmpty(),
        unmaskingFeedback = prefs[Keys.UNMASKING_FEEDBACK].orEmpty()
    )

    private fun parseAnswers(raw: String): Map<String, String> = try {
        val json = JSONObject(raw)
        buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, json.optString(key, ""))
            }
        }
    } catch (_: Exception) {
        emptyMap()
    }

    private fun parseLongMap(raw: String): Map<String, Long> = try {
        val json = JSONObject(raw)
        buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.optLong(key, 0L)
                if (value > 0L) put(key, value)
            }
        }
    } catch (_: Exception) {
        emptyMap()
    }

    private fun encodeAnswers(answers: Map<String, String>): String = JSONObject(answers).toString()
    private fun encodeLongMap(values: Map<String, Long>): String = JSONObject(values).toString()

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val AMBIENT_MUTED = booleanPreferencesKey("ambient_muted")
        val SELECTED_MUSIC_TRACK = stringPreferencesKey("selected_music_track")
        val HAS_CHOSEN_MUSIC_TRACK = booleanPreferencesKey("has_chosen_music_track")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val ORCHESTRA_INTRO_COMPLETED = booleanPreferencesKey("orchestra_intro_completed")
        val TRIAL_STARTED = booleanPreferencesKey("trial_started")
        val TOTAL_ACTIVE_MILLIS = longPreferencesKey("total_active_millis")
        val ANSWERS_JSON = stringPreferencesKey("answers_json")
        val ANSWER_TIMESTAMPS_JSON = stringPreferencesKey("answer_timestamps_json")
        val FREE_WRITING = stringPreferencesKey("free_writing")
        val FREE_WRITING_FACE_ID = stringPreferencesKey("free_writing_face_id")
        val UNMASKING_FEEDBACK = stringPreferencesKey("unmasking_feedback")
    }
}
