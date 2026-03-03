package com.mori.notireplyassistant.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private val EXCLUDED_PACKAGES_KEY = stringSetPreferencesKey("excluded_packages")

    val excludedPackagesFlow: Flow<Set<String>> = dataStore.data
        .map { preferences ->
            preferences[EXCLUDED_PACKAGES_KEY] ?: emptySet()
        }

    suspend fun setExcluded(packageName: String, excluded: Boolean) {
        dataStore.edit { preferences ->
            val current = preferences[EXCLUDED_PACKAGES_KEY] ?: emptySet()
            if (excluded) {
                preferences[EXCLUDED_PACKAGES_KEY] = current + packageName
            } else {
                preferences[EXCLUDED_PACKAGES_KEY] = current - packageName
            }
        }
    }

    suspend fun clearAllExcluded() {
        dataStore.edit { preferences ->
            preferences.remove(EXCLUDED_PACKAGES_KEY)
        }
    }
}
