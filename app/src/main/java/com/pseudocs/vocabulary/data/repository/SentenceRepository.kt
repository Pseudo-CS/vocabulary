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
    @retrofit2.http.POST("v1beta/models/gemini-flash-latest:generateContent")
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

sealed interface SentenceFetchResult {
    data class Success(val sentence: ExampleSentence) : SentenceFetchResult
    data class Failure(
        val isTransient: Boolean,
        val message: String,
        val exception: Throwable? = null
    ) : SentenceFetchResult
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
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
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

    private fun isTransient(throwable: Throwable): Boolean {
        return when (throwable) {
            is java.io.IOException -> true
            is retrofit2.HttpException -> {
                val code = throwable.code()
                code == 429 || code >= 500
            }
            else -> false
        }
    }

    /**
     * Fetches a random example sentence for [word] from Wordnik.
     */
    suspend fun fetchWordnikSentence(word: String, apiKey: String): SentenceFetchResult {
        return try {
            val response = wordnikService.getExamples(word, apiKey)
            val examples = response.examples?.filter { !it.text.isNullOrBlank() }
            if (examples.isNullOrEmpty()) {
                SentenceFetchResult.Failure(isTransient = false, message = "No examples found on Wordnik.")
            } else {
                val picked = examples.random()
                SentenceFetchResult.Success(ExampleSentence(text = picked.text!!, source = "Wordnik"))
            }
        } catch (e: Exception) {
            Log.e("SentenceRepo", "Wordnik fetch failed for '$word'", e)
            SentenceFetchResult.Failure(isTransient = isTransient(e), message = "Wordnik error: ${e.message}", exception = e)
        }
    }

    /**
     * Generates a natural example sentence for [word] using the Gemini API.
     */
    suspend fun fetchGeminiSentence(word: String, apiKey: String): SentenceFetchResult {
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

            if (text.isNullOrBlank()) {
                SentenceFetchResult.Failure(isTransient = false, message = "Gemini returned empty content.")
            } else {
                SentenceFetchResult.Success(ExampleSentence(text = text, source = "Gemini"))
            }
        } catch (e: retrofit2.HttpException) {
            Log.e("SentenceRepo", "Gemini fetch failed for '$word' (HTTP ${e.code()})", e)
            SentenceFetchResult.Failure(isTransient = isTransient(e), message = "Gemini HTTP error ${e.code()}", exception = e)
        } catch (e: Exception) {
            Log.e("SentenceRepo", "Gemini fetch failed for '$word'", e)
            SentenceFetchResult.Failure(isTransient = isTransient(e), message = "Gemini error: ${e.message}", exception = e)
        }
    }

    /**
     * Generates a riddle for [word] using the Gemini API.
     */
    suspend fun fetchGeminiRiddle(word: String, apiKey: String): SentenceFetchResult {
        return try {
            val prompt = """
                Create a short, intriguing riddle about or incorporating the word:
                
                $word
                
                Requirements:
                - Keep it engaging, simple, and concise (1-3 sentences).
                - Make it clear that the answer/concept relates to '$word'.
                - Return only the riddle itself. Do not include introductory text, labels, or the answer.
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

            if (text.isNullOrBlank()) {
                SentenceFetchResult.Failure(isTransient = false, message = "Gemini returned empty riddle.")
            } else {
                SentenceFetchResult.Success(ExampleSentence(text = text, source = "Gemini Riddle"))
            }
        } catch (e: retrofit2.HttpException) {
            Log.e("SentenceRepo", "Gemini riddle fetch failed for '$word' (HTTP ${e.code()})", e)
            SentenceFetchResult.Failure(isTransient = isTransient(e), message = "Gemini HTTP error ${e.code()}", exception = e)
        } catch (e: Exception) {
            Log.e("SentenceRepo", "Gemini riddle fetch failed for '$word'", e)
            SentenceFetchResult.Failure(isTransient = isTransient(e), message = "Gemini error: ${e.message}", exception = e)
        }
    }

    /**
     * Generates a psychology fact for [word] using the Gemini API.
     */
    suspend fun fetchGeminiPsychologyFact(word: String, apiKey: String): SentenceFetchResult {
        return try {
            val prompt = """
                Create a fascinating, educational psychology fact that incorporates or explains the word:
                
                $word
                
                Requirements:
                - Keep it short, interesting, and concise (1-2 sentences).
                - Use the word naturally or explain a psychological concept related to it.
                - Return only the fact itself. Do not include introductory text or labels.
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

            if (text.isNullOrBlank()) {
                SentenceFetchResult.Failure(isTransient = false, message = "Gemini returned empty psychology fact.")
            } else {
                SentenceFetchResult.Success(ExampleSentence(text = text, source = "Gemini Psychology Fact"))
            }
        } catch (e: retrofit2.HttpException) {
            Log.e("SentenceRepo", "Gemini psychology fact fetch failed for '$word' (HTTP ${e.code()})", e)
            SentenceFetchResult.Failure(isTransient = isTransient(e), message = "Gemini HTTP error ${e.code()}", exception = e)
        } catch (e: Exception) {
            Log.e("SentenceRepo", "Gemini psychology fact fetch failed for '$word'", e)
            SentenceFetchResult.Failure(isTransient = isTransient(e), message = "Gemini error: ${e.message}", exception = e)
        }
    }

    /**
     * Generates a joke for [word] using the Gemini API.
     */
    suspend fun fetchGeminiJoke(word: String, apiKey: String): SentenceFetchResult {
        return try {
            val prompt = """
                Create a funny, clean joke that incorporates or uses the word:
                
                $word
                
                Requirements:
                - Keep it short, witty, and concise.
                - Use the word naturally.
                - Return only the joke itself. Do not include introductory text, labels, or explanations.
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

            if (text.isNullOrBlank()) {
                SentenceFetchResult.Failure(isTransient = false, message = "Gemini returned empty joke.")
            } else {
                SentenceFetchResult.Success(ExampleSentence(text = text, source = "Gemini Joke"))
            }
        } catch (e: retrofit2.HttpException) {
            Log.e("SentenceRepo", "Gemini joke fetch failed for '$word' (HTTP ${e.code()})", e)
            SentenceFetchResult.Failure(isTransient = isTransient(e), message = "Gemini HTTP error ${e.code()}", exception = e)
        } catch (e: Exception) {
            Log.e("SentenceRepo", "Gemini joke fetch failed for '$word'", e)
            SentenceFetchResult.Failure(isTransient = isTransient(e), message = "Gemini error: ${e.message}", exception = e)
        }
    }

    /**
     * Generates a dark humor joke for [word] using the Gemini API.
     */
    suspend fun fetchGeminiDarkJoke(word: String, apiKey: String): SentenceFetchResult {
        return try {
            val prompt = """
                Create a witty, dark humor joke that incorporates or uses the word:
                
                $word
                
                Requirements:
                - Keep it short and concise.
                - Use dark humor / gallows humor safely and creatively.
                - Use the word naturally.
                - Return only the joke itself. Do not include introductory text, labels, or explanations.
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

            if (text.isNullOrBlank()) {
                SentenceFetchResult.Failure(isTransient = false, message = "Gemini returned empty dark joke.")
            } else {
                SentenceFetchResult.Success(ExampleSentence(text = text, source = "Gemini Dark Joke"))
            }
        } catch (e: retrofit2.HttpException) {
            Log.e("SentenceRepo", "Gemini dark joke fetch failed for '$word' (HTTP ${e.code()})", e)
            SentenceFetchResult.Failure(isTransient = isTransient(e), message = "Gemini HTTP error ${e.code()}", exception = e)
        } catch (e: Exception) {
            Log.e("SentenceRepo", "Gemini dark joke fetch failed for '$word'", e)
            SentenceFetchResult.Failure(isTransient = isTransient(e), message = "Gemini error: ${e.message}", exception = e)
        }
    }

    /**
     * Fetches a random example sentence for [word] from vocabulary.com.
     */
    suspend fun fetchVocabularyComSentence(word: String): SentenceFetchResult {
        return try {
            val response = vocabularyComService.getExamples(word)
            val sentences = response.sentences?.filter { !it.sentence.isNullOrBlank() }
            if (sentences.isNullOrEmpty()) {
                SentenceFetchResult.Failure(isTransient = false, message = "No examples found on Vocabulary.com.")
            } else {
                val picked = sentences.random()
                SentenceFetchResult.Success(ExampleSentence(text = picked.sentence!!, source = "Vocabulary.com"))
            }
        } catch (e: Exception) {
            Log.e("SentenceRepo", "Vocabulary.com fetch failed for '$word'", e)
            SentenceFetchResult.Failure(isTransient = isTransient(e), message = "Vocabulary.com error: ${e.message}", exception = e)
        }
    }

    /**
     * Fetches an example sentence with automatic fallback logic.
     * Tries the preferred source, then falls back to other available sources.
     */
    suspend fun fetchSentenceWithFallback(word: String, preferredSource: String, settings: AppSettings): SentenceFetchResult {
        // Special AI Modes handling
        val activeModes = mutableListOf<String>()
        if (settings.riddleMode) activeModes.add("riddle")
        if (settings.psychologyFactsMode) activeModes.add("psychology")
        if (settings.jokeMode) activeModes.add("joke")
        if (settings.darkJokeMode) activeModes.add("dark_joke")

        if (activeModes.isNotEmpty() && settings.geminiApiKey.isNotBlank()) {
            val chosenMode = activeModes.random()
            val specialResult = when (chosenMode) {
                "riddle" -> fetchGeminiRiddle(word, settings.geminiApiKey)
                "psychology" -> fetchGeminiPsychologyFact(word, settings.geminiApiKey)
                "joke" -> fetchGeminiJoke(word, settings.geminiApiKey)
                "dark_joke" -> fetchGeminiDarkJoke(word, settings.geminiApiKey)
                else -> null
            }

            if (specialResult is SentenceFetchResult.Success) {
                return specialResult
            }
            Log.d("SentenceRepo", "Special mode ($chosenMode) fetch failed, falling back to standard sentence fetch: ${(specialResult as? SentenceFetchResult.Failure)?.message}")
        }

        val order = when (preferredSource) {
            SentenceSource.GEMINI.name -> listOf(SentenceSource.GEMINI, SentenceSource.WORDNIK, SentenceSource.VOCABULARY_COM)
            SentenceSource.WORDNIK.name -> listOf(SentenceSource.WORDNIK, SentenceSource.GEMINI, SentenceSource.VOCABULARY_COM)
            else -> listOf(SentenceSource.VOCABULARY_COM, SentenceSource.GEMINI, SentenceSource.WORDNIK)
        }

        val failures = mutableListOf<SentenceFetchResult.Failure>()

        for (source in order) {
            val result = when (source) {
                SentenceSource.GEMINI -> {
                    if (settings.geminiApiKey.isNotBlank()) {
                        fetchGeminiSentence(word, settings.geminiApiKey)
                    } else {
                        SentenceFetchResult.Failure(isTransient = false, message = "Gemini API key is blank.")
                    }
                }
                SentenceSource.WORDNIK -> {
                    if (settings.wordnikApiKey.isNotBlank()) {
                        fetchWordnikSentence(word, settings.wordnikApiKey)
                    } else {
                        SentenceFetchResult.Failure(isTransient = false, message = "Wordnik API key is blank.")
                    }
                }
                SentenceSource.VOCABULARY_COM -> {
                    fetchVocabularyComSentence(word)
                }
            }

            when (result) {
                is SentenceFetchResult.Success -> return result
                is SentenceFetchResult.Failure -> failures.add(result)
            }
        }

        val hasTransientFailure = failures.any { it.isTransient }
        val primaryErrorMessage = failures.firstOrNull()?.message ?: "Unknown error"
        return SentenceFetchResult.Failure(isTransient = hasTransientFailure, message = primaryErrorMessage)
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
