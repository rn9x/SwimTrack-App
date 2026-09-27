package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppSettings
import com.example.domain.model.PoolLength
import com.example.domain.model.SessionType
import com.example.domain.model.ThemePreference
import com.example.domain.model.TimeFormatPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "swimtrack_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME = stringPreferencesKey("theme_preference")
        val TIME_FORMAT = stringPreferencesKey("time_format_preference")
        val DEFAULT_POOL = stringPreferencesKey("default_pool_length")
        val DEFAULT_SESSION = stringPreferencesKey("default_session_type")
        val COMPETITION_ONLY_PB = booleanPreferencesKey("competition_only_pb")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val language = prefs[Keys.LANGUAGE]?.let {
            AppLanguage.fromString(it)
        } ?: AppLanguage.ENGLISH

        val theme = prefs[Keys.THEME]?.let {
            runCatching { ThemePreference.valueOf(it) }.getOrNull()
        } ?: ThemePreference.SYSTEM

        val timeFormat = prefs[Keys.TIME_FORMAT]?.let {
            runCatching { TimeFormatPreference.valueOf(it) }.getOrNull()
        } ?: TimeFormatPreference.MINUTES_SECONDS

        val defaultPool = prefs[Keys.DEFAULT_POOL]?.let {
            PoolLength.fromString(it)
        } ?: PoolLength.POOL_25M

        val defaultSession = prefs[Keys.DEFAULT_SESSION]?.let {
            SessionType.fromString(it)
        } ?: SessionType.TRAINING

        val compOnlyPb = prefs[Keys.COMPETITION_ONLY_PB] ?: false

        AppSettings(
            language = language,
            themePreference = theme,
            timeFormatPreference = timeFormat,
            defaultPoolLength = defaultPool,
            defaultSessionType = defaultSession,
            competitionOnlyPb = compOnlyPb
        )
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun updateTheme(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun updateTimeFormat(format: TimeFormatPreference) {
        context.dataStore.edit { it[Keys.TIME_FORMAT] = format.name }
    }

    suspend fun updateDefaultPool(poolLength: PoolLength) {
        context.dataStore.edit { it[Keys.DEFAULT_POOL] = poolLength.name }
    }

    suspend fun updateDefaultSessionType(sessionType: SessionType) {
        context.dataStore.edit { it[Keys.DEFAULT_SESSION] = sessionType.name }
    }

    suspend fun updateCompetitionOnlyPb(enabled: Boolean) {
        context.dataStore.edit { it[Keys.COMPETITION_ONLY_PB] = enabled }
    }
}
