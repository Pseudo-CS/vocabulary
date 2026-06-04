package com.pseudocs.vocabulary.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pseudocs.vocabulary.data.local.AppSettings
import com.pseudocs.vocabulary.data.local.SettingsDataStore
import com.pseudocs.vocabulary.notification.DailyNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Persists preferences and manages notification scheduling.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun updateNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsDataStore.updateNotificationTime(hour, minute)
            // Reschedule the notification with new time
            if (settings.value.notificationsEnabled) {
                DailyNotificationWorker.schedule(context, hour, minute)
            }
        }
    }

    fun updateSentenceSource(source: String) {
        viewModelScope.launch {
            settingsDataStore.updateSentenceSource(source)
        }
    }

    fun updateWordnikApiKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.updateWordnikApiKey(key)
        }
    }

    fun updateGeminiApiKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.updateGeminiApiKey(key)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateNotificationsEnabled(enabled)
            if (enabled) {
                DailyNotificationWorker.schedule(
                    context,
                    settings.value.notificationHour,
                    settings.value.notificationMinute
                )
            } else {
                DailyNotificationWorker.cancel(context)
            }
        }
    }
}
