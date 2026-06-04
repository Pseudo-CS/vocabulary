package com.pseudocs.vocabulary.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pseudocs.vocabulary.data.local.SentenceSource
import com.pseudocs.vocabulary.data.local.SettingsDataStore
import com.pseudocs.vocabulary.data.model.ExampleSentence
import com.pseudocs.vocabulary.data.model.Word
import com.pseudocs.vocabulary.data.repository.SentenceRepository
import com.pseudocs.vocabulary.data.repository.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordDetailUiState(
    val word: Word? = null,
    val definition: String? = null,
    val partOfSpeech: String? = null,
    val phonetic: String? = null,
    val sentence: ExampleSentence? = null,
    val isLoading: Boolean = false,
    val isLoadingDefinition: Boolean = false,
    val error: String? = null,
    val definitionError: String? = null,
    val sourceLabel: String = ""
)

/**
 * ViewModel for Word Detail screen.
 * Loads the word and fetches an example sentence from the configured source.
 */
@HiltViewModel
class WordDetailViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val sentenceRepository: SentenceRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordDetailUiState())
    val uiState: StateFlow<WordDetailUiState> = _uiState.asStateFlow()

    fun loadWord(wordId: Long) {
        viewModelScope.launch {
            val word = wordRepository.getWordById(wordId)
            _uiState.update { it.copy(word = word) }
            if (word != null) {
                launch { fetchDefinition(word.word) }
                launch { fetchSentence(word.word) }
            }
        }
    }

    fun refreshSentence() {
        val word = _uiState.value.word ?: return
        viewModelScope.launch {
            launch { fetchSentence(word.word) }
            if (_uiState.value.definition == null) {
                launch { fetchDefinition(word.word) }
            }
        }
    }

    private suspend fun fetchDefinition(wordText: String) {
        _uiState.update { it.copy(isLoadingDefinition = true, definitionError = null) }
        val settings = settingsDataStore.settingsFlow.first()

        val definitionResult = sentenceRepository.fetchDefinitionWithFallback(wordText, settings)

        _uiState.update {
            it.copy(
                definition = definitionResult?.first,
                partOfSpeech = definitionResult?.second,
                phonetic = definitionResult?.third,
                isLoadingDefinition = false,
                definitionError = if (definitionResult == null) "Definition not found." else null
            )
        }
    }

    private suspend fun fetchSentence(wordText: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        val settings = settingsDataStore.settingsFlow.first()

        val sentence = sentenceRepository.fetchSentenceWithFallback(wordText, settings.sentenceSource, settings)

        _uiState.update {
            it.copy(
                sentence = sentence,
                isLoading = false,
                error = if (sentence == null) "Could not fetch examples. Try again." else null,
                sourceLabel = sentence?.source ?: ""
            )
        }
    }
}
