package com.example.surfer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "search_preferences")

class SearchPreferencesRepository(private val context: Context) {
    private val SEARCH_ENGINE_KEY = stringPreferencesKey("search_engine")

    val selectedSearchEngine: Flow<SearchEngine> = context.dataStore.data
        .map { preferences ->
            val engineName = preferences[SEARCH_ENGINE_KEY] ?: SearchEngine.GOOGLE.name
            try {
                SearchEngine.valueOf(engineName)
            } catch (e: IllegalArgumentException) {
                SearchEngine.GOOGLE
            }
        }

    suspend fun saveSearchEngine(searchEngine: SearchEngine) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_ENGINE_KEY] = searchEngine.name
        }
    }
}
