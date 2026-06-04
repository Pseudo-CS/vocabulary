package com.pseudocs.vocabulary.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.pseudocs.vocabulary.MainActivity
import com.pseudocs.vocabulary.R
import com.pseudocs.vocabulary.data.local.SentenceSource
import com.pseudocs.vocabulary.data.local.SettingsDataStore
import com.pseudocs.vocabulary.data.repository.SentenceRepository
import com.pseudocs.vocabulary.data.repository.WordRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val CHANNEL_ID = "vocabulary_channel_v2"
const val NOTIFICATION_ID = 1001
const val WORD_ID_EXTRA = "word_id"

/**
 * HiltWorker that fires once daily to send a "Word of the Day" notification.
 * Picks a random word, fetches an example sentence, and displays a notification.
 * After running, it re-schedules itself for the next day.
 */
@HiltWorker
class DailyNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val wordRepository: WordRepository,
    private val sentenceRepository: SentenceRepository,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val isManual = inputData.getBoolean("is_manual", false)
            val settings = settingsDataStore.settingsFlow.first()
            Log.d("VocabWorker", "doWork: isManual=$isManual, source=${settings.sentenceSource}, geminiKeyBlank=${settings.geminiApiKey.isBlank()}, wordnikKeyBlank=${settings.wordnikApiKey.isBlank()}, notificationsEnabled=${settings.notificationsEnabled}")
            if (!isManual && !settings.notificationsEnabled) return Result.success()

            val word = wordRepository.getRandomWord() ?: return Result.success()
            Log.d("VocabWorker", "Selected word: ${word.word}")

            // Fetch sentence based on the configured source with automatic fallbacks
            val sentence = sentenceRepository.fetchSentenceWithFallback(word.word, settings.sentenceSource, settings)
            Log.d("VocabWorker", "Fetched sentence: $sentence")

            showNotification(word.word, sentence?.text, word.id)

            // Re-schedule for tomorrow at the same time if this is not a manual trigger
            if (!isManual) {
                scheduleTomorrow(context, settings.notificationHour, settings.notificationMinute)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(word: String, sentence: String?, wordId: Long) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Use HIGH importance so the notification pops up as a heads-up banner,
        // showing the sentence immediately without requiring the user to pull down the shade.
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Word of the Day",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily vocabulary word notifications"
        }
        notificationManager.createNotificationChannel(channel)

        // Intent to open the app at the word details screen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WORD_ID_EXTRA, wordId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the body: show the sentence directly, or a clear fallback
        val bodyText = if (!sentence.isNullOrBlank()) {
            "• $sentence"
        } else {
            "No API key configured — add one in Settings to get example sentences."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_v)
            .setContentTitle("Word of the Day: ${word.uppercase()}")
            // contentText is what's shown in collapsed/banner view
            .setContentText(bodyText)
            // BigTextStyle ensures the full sentence is visible when expanded
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // HIGH priority pairs with IMPORTANCE_HIGH channel to show as a banner
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_TAG = "daily_vocabulary_notification"

        /**
         * Trigger the notification immediately — no network constraint so it fires
         * right away regardless of connectivity.
         */
        fun triggerImmediately(context: Context) {
            val request = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                .setInputData(workDataOf("is_manual" to true))
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        /**
         * Schedule the worker to run once at the given hour/minute.
         */
        fun schedule(context: Context, hour: Int, minute: Int) {
            val delay = calculateDelayMillis(hour, minute)
            val request = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(WORK_TAG)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        /**
         * Cancel any scheduled notification work.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }

        private fun scheduleTomorrow(context: Context, hour: Int, minute: Int) {
            schedule(context, hour, minute)
        }

        /**
         * Calculates the delay in milliseconds until the next occurrence of [hour]:[minute].
         */
        fun calculateDelayMillis(hour: Int, minute: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}
