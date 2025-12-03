package com.countdownhour.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pomodoro_data")

class PomodoroDataStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val SETTINGS_KEY = stringPreferencesKey("settings")
        private val TODO_POOL_KEY = stringPreferencesKey("todo_pool")
        private val SELECTED_TODO_IDS_KEY = stringPreferencesKey("selected_todo_ids")
        private val COMPLETED_POMODOROS_KEY = intPreferencesKey("completed_pomodoros")
    }

    // Settings
    val settingsFlow: Flow<PomodoroSettings> = context.dataStore.data.map { prefs ->
        prefs[SETTINGS_KEY]?.let {
            try {
                json.decodeFromString<PomodoroSettings>(it)
            } catch (e: Exception) {
                PomodoroSettings()
            }
        } ?: PomodoroSettings()
    }

    suspend fun saveSettings(settings: PomodoroSettings) {
        context.dataStore.edit { prefs ->
            prefs[SETTINGS_KEY] = json.encodeToString(settings)
        }
    }

    // Todo Pool
    val todoPoolFlow: Flow<List<PomodoroTodo>> = context.dataStore.data.map { prefs ->
        prefs[TODO_POOL_KEY]?.let {
            try {
                json.decodeFromString<List<PomodoroTodo>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    suspend fun saveTodoPool(todos: List<PomodoroTodo>) {
        context.dataStore.edit { prefs ->
            prefs[TODO_POOL_KEY] = json.encodeToString(todos)
        }
    }

    // Selected Todo IDs
    val selectedTodoIdsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_TODO_IDS_KEY]?.let {
            try {
                json.decodeFromString<Set<String>>(it)
            } catch (e: Exception) {
                emptySet()
            }
        } ?: emptySet()
    }

    suspend fun saveSelectedTodoIds(ids: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_TODO_IDS_KEY] = json.encodeToString(ids)
        }
    }

    // Completed Pomodoros
    val completedPomodorosFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[COMPLETED_POMODOROS_KEY] ?: 0
    }

    suspend fun saveCompletedPomodoros(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[COMPLETED_POMODOROS_KEY] = count
        }
    }
}
