package com.marcm.cadencia.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Modo de tema elegido por el usuario. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Ajustes de la app persistidos en DataStore (tema y estado de onboarding). */
class SettingsRepository(private val context: Context) {

    private val keyTheme = stringPreferencesKey("theme_mode")
    private val keyOnboardingDone = booleanPreferencesKey("onboarding_done")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[keyTheme] ?: ThemeMode.DARK.name) }
            .getOrDefault(ThemeMode.DARK)
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[keyOnboardingDone] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[keyTheme] = mode.name }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[keyOnboardingDone] = done }
    }
}
