package com.pseudocs.vocabulary.notification

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.pseudocs.vocabulary.data.local.AppSettings
import com.pseudocs.vocabulary.data.local.SettingsDataStore
import com.pseudocs.vocabulary.data.model.ExampleSentence
import com.pseudocs.vocabulary.data.model.Word
import com.pseudocs.vocabulary.data.repository.SentenceFetchResult
import com.pseudocs.vocabulary.data.repository.SentenceRepository
import com.pseudocs.vocabulary.data.repository.WordRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DailyNotificationWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val wordRepository = mockk<WordRepository>()
    private val sentenceRepository = mockk<SentenceRepository>()
    private val settingsDataStore = mockk<SettingsDataStore>()
    private val notificationHelper = mockk<NotificationHelper>(relaxed = true)

    private lateinit var worker: DailyNotificationWorker

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0

        worker = DailyNotificationWorker(
            context = context,
            workerParams = workerParams,
            wordRepository = wordRepository,
            sentenceRepository = sentenceRepository,
            settingsDataStore = settingsDataStore,
            notificationHelper = notificationHelper
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun doWork_onSuccess_showsNotification() = runBlocking {
        // Arrange
        val settings = AppSettings(
            notificationsEnabled = true,
            notificationHour = 9,
            notificationMinute = 0
        )
        every { settingsDataStore.settingsFlow } returns flowOf(settings)

        val word = Word(id = 1, word = "test")
        coEvery { wordRepository.getRandomWord() } returns word

        val sentence = ExampleSentence("This is a test sentence.", "Gemini")
        coEvery {
            sentenceRepository.fetchSentenceWithFallback(word.word, any(), any())
        } returns SentenceFetchResult.Success(sentence)

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
        // Verify notification helper was called
        verify(exactly = 1) {
            notificationHelper.showNotification(word.word, sentence.text, word.id, settings, sentence.source)
        }
    }

    @Test
    fun doWork_onTransientFailure_retriesWorker() = runBlocking {
        // Arrange
        val settings = AppSettings(
            notificationsEnabled = true,
            notificationHour = 9,
            notificationMinute = 0
        )
        every { settingsDataStore.settingsFlow } returns flowOf(settings)

        val word = Word(id = 1, word = "test")
        coEvery { wordRepository.getRandomWord() } returns word

        coEvery {
            sentenceRepository.fetchSentenceWithFallback(word.word, any(), any())
        } returns SentenceFetchResult.Failure(isTransient = true, message = "Network error")

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.retry(), result)
        // Verify notification helper was NOT called
        verify(exactly = 0) {
            notificationHelper.showNotification(any(), any(), any(), any())
        }
    }

    @Test
    fun doWork_onPermanentFailure_showsFallbackNotification() = runBlocking {
        // Arrange
        val settings = AppSettings(
            notificationsEnabled = true,
            notificationHour = 9,
            notificationMinute = 0
        )
        every { settingsDataStore.settingsFlow } returns flowOf(settings)

        val word = Word(id = 1, word = "test")
        coEvery { wordRepository.getRandomWord() } returns word

        coEvery {
            sentenceRepository.fetchSentenceWithFallback(word.word, any(), any())
        } returns SentenceFetchResult.Failure(isTransient = false, message = "Word not found")

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
        // Verify fallback notification helper was called (sentence text is null)
        verify(exactly = 1) {
            notificationHelper.showNotification(word.word, null, word.id, settings)
        }
    }
}
