package com.pseudocs.vocabulary.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vocabulary_settings")

/**
 * Keys for all persisted settings.
 */
object SettingsKeys {
    val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
    val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
    val SENTENCE_SOURCE = stringPreferencesKey("sentence_source")
    val WORDNIK_API_KEY = stringPreferencesKey("wordnik_api_key")
    val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
}

/**
 * Settings data class representing all user preferences.
 */
data class AppSettings(
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0,
    val sentenceSource: String = SentenceSource.GEMINI.name,
    val wordnikApiKey: String = "",
    val geminiApiKey: String = "",
    val notificationsEnabled: Boolean = false
)

enum class SentenceSource {
    WORDNIK, GEMINI, VOCABULARY_COM
}

/**
 * DataStore-backed preferences manager for persisting all app settings.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            AppSettings(
                notificationHour = prefs[SettingsKeys.NOTIFICATION_HOUR] ?: 8,
                notificationMinute = prefs[SettingsKeys.NOTIFICATION_MINUTE] ?: 0,
                sentenceSource = prefs[SettingsKeys.SENTENCE_SOURCE] ?: SentenceSource.GEMINI.name,
                wordnikApiKey = prefs[SettingsKeys.WORDNIK_API_KEY] ?: "",
                geminiApiKey = prefs[SettingsKeys.GEMINI_API_KEY] ?: "",
                notificationsEnabled = prefs[SettingsKeys.NOTIFICATIONS_ENABLED] ?: false
            )
        }

    suspend fun updateNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.NOTIFICATION_HOUR] = hour
            prefs[SettingsKeys.NOTIFICATION_MINUTE] = minute
        }
    }

    suspend fun updateSentenceSource(source: String) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.SENTENCE_SOURCE] = source
        }
    }

    suspend fun updateWordnikApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.WORDNIK_API_KEY] = key
        }
    }

    suspend fun updateGeminiApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.GEMINI_API_KEY] = key
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
