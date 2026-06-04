package com.pseudocs.vocabulary.data.repository

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.pseudocs.vocabulary.data.local.AppSettings
import com.pseudocs.vocabulary.data.local.SentenceSource
import com.pseudocs.vocabulary.data.model.ExampleSentence
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

// ---------- Wordnik API Models ----------

data class WordnikExample(
    @SerializedName("text") val text: String? = null,
    @SerializedName("title") val title: String? = null
)

data class WordnikExamplesResponse(
    @SerializedName("examples") val examples: List<WordnikExample>? = null
)

// ---------- Wordnik API Service ----------

interface WordnikApiService {
    @GET("word.json/{word}/examples")
    suspend fun getExamples(
        @Path("word") word: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 10,
        @Query("includeDuplicates") includeDuplicates: Boolean = false
    ): WordnikExamplesResponse
}

// ---------- Gemini API Models ----------

data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

// ---------- Gemini API Service ----------

interface GeminiApiService {
    @retrofit2.http.POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @retrofit2.http.Body request: GeminiRequest
    ): GeminiResponse
}

// ---------- Vocabulary.com API Models ----------

data class VocabularyComSentence(
    @SerializedName("sentence") val sentence: String? = null
)

data class VocabularyComResponse(
    @SerializedName("sentences") val sentences: List<VocabularyComSentence>? = null
)

// ---------- Vocabulary.com API Service ----------

interface VocabularyComApiService {
    @GET("api/1.0/examples")
    suspend fun getExamples(
        @Query("query") query: String,
        @Query("maxResults") maxResults: Int = 10,
        @Query("startOffset") startOffset: Int = 0
    ): VocabularyComResponse
}

// ---------- Dictionary API Models ----------

data class DictionaryPhonetic(
    @SerializedName("text") val text: String? = null
)

data class DictionaryDefinition(
    @SerializedName("definition") val definition: String? = null
)

data class DictionaryMeaning(
    @SerializedName("partOfSpeech") val partOfSpeech: String? = null,
    @SerializedName("definitions") val definitions: List<DictionaryDefinition>? = null
)

data class DictionaryEntry(
    @SerializedName("word") val word: String? = null,
    @SerializedName("phonetic") val phonetic: String? = null,
    @SerializedName("phonetics") val phonetics: List<DictionaryPhonetic>? = null,
    @SerializedName("meanings") val meanings: List<DictionaryMeaning>? = null
)

// ---------- Dictionary API Service ----------

interface DictionaryApiService {
    @GET("api/v2/entries/en/{word}")
    suspend fun getDefinition(@Path("word") word: String): List<DictionaryEntry>
}

/**
 * Repository responsible for fetching example sentences from Wordnik or Gemini.
 */
@Singleton
class SentenceRepository @Inject constructor() {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val wordnikService: WordnikApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.wordnik.com/v4/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WordnikApiService::class.java)
    }

    private val geminiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    private val vocabularyComService: VocabularyComApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://corpus.vocabulary.com/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VocabularyComApiService::class.java)
    }

    private val dictionaryService: DictionaryApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.dictionaryapi.dev/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DictionaryApiService::class.java)
    }

    /**
     * Fetches a random example sentence for [word] from Wordnik.
     * Returns null on failure.
     */
    suspend fun fetchWordnikSentence(word: String, apiKey: String): ExampleSentence? {
        return try {
            val response = wordnikService.getExamples(word, apiKey)
            val examples = response.examples?.filter { !it.text.isNullOrBlank() }
            if (examples.isNullOrEmpty()) return null
            // Pick a random sentence for variety
            val picked = examples.random()
            ExampleSentence(text = picked.text!!, source = "Wordnik")
        } catch (e: Exception) {
            Log.e("SentenceRepo", "Wordnik fetch failed for '$word'", e)
            null
        }
    }

    /**
     * Generates a natural example sentence for [word] using the Gemini API.
     * Returns null on failure.
     */
    suspend fun fetchGeminiSentence(word: String, apiKey: String): ExampleSentence? {
        return try {
            val prompt = """
                Generate 1 short, natural example sentence using the word:

                $word

                Requirements:
                - Modern English
                - Realistic situations
                - Different contexts
                - One sentence per line

                Return only the sentence.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                )
            )
            val response = geminiService.generateContent(apiKey, request)
            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?.lines()
                ?.firstOrNull { it.isNotBlank() }

            if (text.isNullOrBlank()) null
            else ExampleSentence(text = text, source = "Gemini")
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 429) {
                Log.w("SentenceRepo", "Gemini rate limited for '$word' — try again in a moment")
                ExampleSentence(text = "(Gemini rate limit hit — try again in a moment)", source = "Gemini")
            } else {
                Log.e("SentenceRepo", "Gemini fetch failed for '$word' (HTTP ${e.code()})", e)
                null
            }
        } catch (e: Exception) {
            Log.e("SentenceRepo", "Gemini fetch failed for '$word'", e)
            null
        }
    }

    /**
     * Fetches a random example sentence for [word] from vocabulary.com.
     * Returns null on failure.
     */
    suspend fun fetchVocabularyComSentence(word: String): ExampleSentence? {
        return try {
            val response = vocabularyComService.getExamples(word)
            val sentences = response.sentences?.filter { !it.sentence.isNullOrBlank() }
            if (sentences.isNullOrEmpty()) return null
            // Pick a random sentence for variety
            val picked = sentences.random()
            ExampleSentence(text = picked.sentence!!, source = "Vocabulary.com")
        } catch (e: Exception) {
            Log.e("SentenceRepo", "Vocabulary.com fetch failed for '$word'", e)
            null
        }
    }

    /**
     * Fetches an example sentence with automatic fallback logic.
     * Tries the preferred source, then falls back to other available sources.
     */
    suspend fun fetchSentenceWithFallback(word: String, preferredSource: String, settings: AppSettings): ExampleSentence? {
        val order = when (preferredSource) {
            SentenceSource.GEMINI.name -> listOf(SentenceSource.GEMINI, SentenceSource.WORDNIK, SentenceSource.VOCABULARY_COM)
            SentenceSource.WORDNIK.name -> listOf(SentenceSource.WORDNIK, SentenceSource.GEMINI, SentenceSource.VOCABULARY_COM)
            else -> listOf(SentenceSource.VOCABULARY_COM, SentenceSource.GEMINI, SentenceSource.WORDNIK)
        }

        var fallbackCandidate: ExampleSentence? = null

        for (source in order) {
            val result = when (source) {
                SentenceSource.GEMINI -> {
                    if (settings.geminiApiKey.isNotBlank()) {
                        fetchGeminiSentence(word, settings.geminiApiKey)
                    } else null
                }
                SentenceSource.WORDNIK -> {
                    if (settings.wordnikApiKey.isNotBlank()) {
                        fetchWordnikSentence(word, settings.wordnikApiKey)
                    } else null
                }
                SentenceSource.VOCABULARY_COM -> {
                    fetchVocabularyComSentence(word)
                }
            }

            if (result != null) {
                // If it is a real sentence (not a rate limit message), return it immediately
                if (!result.text.contains("rate limit", ignoreCase = true) && result.text.isNotBlank()) {
                    return result
                }
                // Save the rate limit message as fallback candidate in case nothing else succeeds
                if (fallbackCandidate == null) {
                    fallbackCandidate = result
                }
            }
        }

        return fallbackCandidate
    }

    /**
     * Fetches definition, part of speech, and phonetic for [word] using the Free Dictionary API.
     * Returns Triple(definition, partOfSpeech, phonetic) or null.
     */
    suspend fun fetchDefinition(word: String): Triple<String, String, String?>? {
        return try {
            val entries = dictionaryService.getDefinition(word)
            val entry = entries.firstOrNull() ?: return null
            val meaning = entry.meanings?.firstOrNull() ?: return null
            val definitionText = meaning.definitions?.firstOrNull()?.definition ?: return null
            val partOfSpeech = meaning.partOfSpeech ?: "definition"
            val phonetic = entry.phonetic ?: entry.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text
            Triple(definitionText, partOfSpeech, phonetic)
        } catch (e: Exception) {
            Log.e("SentenceRepo", "Dictionary API fetch failed for '$word'", e)
            null
        }
    }

    /**
     * Generates a definition for [word] using Gemini.
     * Returns Pair(definition, partOfSpeech) or null.
     */
    suspend fun fetchGeminiDefinition(word: String, apiKey: String): Pair<String, String>? {
        return try {
            val prompt = "Define the word '$word'. Provide a short, concise definition. First line: part of speech (noun, verb, adjective, etc.). Second line: the definition."
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                )
            )
            val response = geminiService.generateContent(apiKey, request)
            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
            if (text.isNullOrBlank()) return null
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.size >= 2) {
                Pair(lines[1], lines[0])
            } else {
                Pair(text, "definition")
            }
        } catch (e: Exception) {
            Log.e("SentenceRepo", "Gemini definition fetch failed for '$word'", e)
            null
        }
    }

    /**
     * Fetches the definition with automatic fallbacks.
     */
    suspend fun fetchDefinitionWithFallback(word: String, settings: AppSettings): Triple<String, String, String?>? {
        val freeDictResult = fetchDefinition(word)
        if (freeDictResult != null) return freeDictResult

        if (settings.geminiApiKey.isNotBlank()) {
            val geminiResult = fetchGeminiDefinition(word, settings.geminiApiKey)
            if (geminiResult != null) {
                return Triple(geminiResult.first, geminiResult.second, null)
            }
        }

        return null
    }
}
