package com.pseudocs.vocabulary.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pseudocs.vocabulary.data.local.SettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver triggered by AlarmManager at the user-defined daily notification time.
 * Triggers the notification worker and schedules the next day's alarm.
 */
@AndroidEntryPoint
class NotificationAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationAlarmRec", "Alarm triggered")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = settingsDataStore.settingsFlow.first()
                if (settings.notificationsEnabled) {
                    // Trigger the notification display worker immediately
                    DailyNotificationWorker.triggerImmediately(context)
                    
                    // Reschedule for the next day
                    NotificationScheduler.schedule(
                        context,
                        settings.notificationHour,
                        settings.notificationMinute
                    )
                    Log.d("NotificationAlarmRec", "Triggered notification worker and rescheduled next alarm")
                }
            } catch (e: Exception) {
                Log.e("NotificationAlarmRec", "Error handling alarm broadcast", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
