package com.pseudocs.vocabulary.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pseudocs.vocabulary.MainActivity
import com.pseudocs.vocabulary.R
import com.pseudocs.vocabulary.data.local.AppSettings
import com.pseudocs.vocabulary.data.local.SentenceSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun showNotification(
        word: String,
        sentence: String?,
        wordId: Long,
        settings: AppSettings,
        source: String? = null
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Word of the Day",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily vocabulary word notifications"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WORD_ID_EXTRA, wordId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isNetworkAvailable = isNetworkAvailable(context)
        val bodyText = if (!sentence.isNullOrBlank()) {
            sentence
        } else if (!isNetworkAvailable) {
            "Tap to see details. (Offline — could not fetch example sentence)"
        } else {
            val preferredSource = settings.sentenceSource
            val isKeyMissing = when (preferredSource) {
                SentenceSource.GEMINI.name -> settings.geminiApiKey.isBlank()
                SentenceSource.WORDNIK.name -> settings.wordnikApiKey.isBlank()
                else -> false
            }
            if (isKeyMissing) {
                "No API key configured for $preferredSource — add one in Settings to get example sentences."
            } else {
                "Tap to see details. (Could not fetch example sentence)"
            }
        }

        val titleText = when (source) {
            "Gemini Riddle" -> "Riddle of the Day"
            "Gemini Psychology Fact" -> "Psychology Fact of the Day"
            "Gemini Joke" -> "Joke of the Day"
            "Gemini Dark Joke" -> "Dark Joke of the Day"
            else -> "Word of the Day: ${word.uppercase()}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_v)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.w("NotificationHelper", "Failed to check network availability, assuming online", e)
            true
        }
    }
}
