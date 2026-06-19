package com.pseudocs.vocabulary.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.pseudocs.vocabulary.data.local.SettingsDataStore
import com.pseudocs.vocabulary.data.repository.SentenceFetchResult
import com.pseudocs.vocabulary.data.repository.SentenceRepository
import com.pseudocs.vocabulary.data.repository.WordRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

const val CHANNEL_ID = "vocabulary_channel_v2"
const val NOTIFICATION_ID = 1001
const val WORD_ID_EXTRA = "word_id"

/**
 * HiltWorker that fires to send a "Word of the Day" notification.
 * Picks a random word, fetches an example sentence, and displays a notification.
 */
@HiltWorker
class DailyNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val wordRepository: WordRepository,
    private val sentenceRepository: SentenceRepository,
    private val settingsDataStore: SettingsDataStore,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val isManual = inputData.getBoolean("is_manual", false)
            val settings = settingsDataStore.settingsFlow.first()
            Log.d("VocabWorker", "doWork: isManual=$isManual, source=${settings.sentenceSource}, geminiKeyBlank=${settings.geminiApiKey.isBlank()}, wordnikKeyBlank=${settings.wordnikApiKey.isBlank()}, notificationsEnabled=${settings.notificationsEnabled}")
            if (!isManual && !settings.notificationsEnabled) return Result.success()

            val word = wordRepository.getRandomWord()
            if (word == null) {
                Log.d("VocabWorker", "No words available in the repository.")
                return Result.success()
            }
            Log.d("VocabWorker", "Selected word: ${word.word}")

            // Fetch sentence based on the configured source with automatic fallbacks
            val result = sentenceRepository.fetchSentenceWithFallback(word.word, settings.sentenceSource, settings)
            Log.d("VocabWorker", "Fetched sentence result: $result")

            when (result) {
                is SentenceFetchResult.Success -> {
                    notificationHelper.showNotification(
                        word.word,
                        result.sentence.text,
                        word.id,
                        settings,
                        result.sentence.source
                    )
                    Result.success()
                }
                is SentenceFetchResult.Failure -> {
                    if (result.isTransient) {
                        Log.d("VocabWorker", "Transient network error, retrying worker: ${result.message}")
                        Result.retry()
                    } else {
                        // Permanent failure: show notification without example sentence and succeed
                        notificationHelper.showNotification(word.word, null, word.id, settings)
                        Result.success()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VocabWorker", "Error in doWork", e)
            Result.retry()
        }
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
    }
}
