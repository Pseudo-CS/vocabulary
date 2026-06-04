package com.pseudocs.vocabulary.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pseudocs.vocabulary.data.model.Word
import com.pseudocs.vocabulary.data.repository.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VocabularyUiState(
    val words: List<Word> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val message: String? = null
)

/**
 * ViewModel for the Vocabulary screen.
 * Handles word CRUD, search, and bulk TXT import.
 */
@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val wordRepository: WordRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<VocabularyUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) wordRepository.getAllWords()
            else wordRepository.searchWords(query)
        },
        _searchQuery,
        _message
    ) { words, query, message ->
        VocabularyUiState(words = words, searchQuery = query, message = message)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VocabularyUiState(isLoading = true)
    )

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun addWord(word: String) {
        viewModelScope.launch {
            val success = wordRepository.addWord(word)
            _message.value = if (success) "Word added!" else "Word already exists or is empty."
            clearMessageAfterDelay()
        }
    }

    fun updateWord(id: Long, newText: String) {
        viewModelScope.launch {
            val success = wordRepository.updateWord(id, newText)
            _message.value = if (success) "Word updated!" else "Could not update word."
            clearMessageAfterDelay()
        }
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch {
            wordRepository.deleteWord(word)
            _message.value = "\"${word.word}\" deleted."
            clearMessageAfterDelay()
        }
    }

    /**
     * Import words from plain text content (one word per line or comma-separated).
     */
    fun importFromText(text: String) {
        viewModelScope.launch {
            val rawList = if (text.lines().size <= 1 && text.contains(",")) {
                text.split(",")
            } else {
                text.lines()
            }
            val words = rawList.map { it.trim() }.filter { it.isNotBlank() }
            if (words.isEmpty()) {
                _message.value = "No valid words found."
            } else {
                wordRepository.addWords(words)
                _message.value = "Imported ${words.size} word(s)."
            }
            clearMessageAfterDelay()
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun clearMessageAfterDelay() {
        kotlinx.coroutines.delay(3000)
        _message.value = null
    }
}
