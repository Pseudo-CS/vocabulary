package com.pseudocs.vocabulary.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.pseudocs.vocabulary.data.local.SettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Reschedules the daily notification after device reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val settings = settingsDataStore.settingsFlow.first()
            if (settings.notificationsEnabled) {
                DailyNotificationWorker.schedule(
                    context,
                    settings.notificationHour,
                    settings.notificationMinute
                )
            }
        }
    }
}
