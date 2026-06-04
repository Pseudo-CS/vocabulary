package com.pseudocs.vocabulary.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Broadcast receiver triggered when the notification is tapped.
 * (Currently unused — notification uses PendingIntent directly to MainActivity.)
 */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: the PendingIntent in the notification handles navigation directly
    }
}
