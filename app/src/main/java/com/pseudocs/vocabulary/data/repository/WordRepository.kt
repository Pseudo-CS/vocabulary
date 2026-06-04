package com.pseudocs.vocabulary.data.repository

import com.pseudocs.vocabulary.data.local.WordDao
import com.pseudocs.vocabulary.data.model.Word
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository abstracting all local word operations.
 * Acts as the single source of truth for vocabulary data.
 */
@Singleton
class WordRepository @Inject constructor(
    private val wordDao: WordDao
) {
    fun getAllWords(): Flow<List<Word>> = wordDao.getAllWords()

    fun searchWords(query: String): Flow<List<Word>> = wordDao.searchWords(query)

    suspend fun getRandomWord(): Word? = wordDao.getRandomWord()

    suspend fun getWordById(id: Long): Word? = wordDao.getWordById(id)

    suspend fun addWord(word: String): Boolean {
        val trimmed = word.trim()
        if (trimmed.isBlank()) return false
        val result = wordDao.insertWord(Word(word = trimmed))
        return result != -1L
    }

    suspend fun addWords(words: List<String>) {
        val wordEntities = words
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { Word(word = it) }
        wordDao.insertWords(wordEntities)
    }

    suspend fun updateWord(id: Long, newText: String): Boolean {
        val trimmed = newText.trim()
        if (trimmed.isBlank()) return false
        val word = wordDao.getWordById(id) ?: return false
        wordDao.updateWord(word.copy(word = trimmed))
        return true
    }

    suspend fun deleteWord(word: Word) = wordDao.deleteWord(word)

    suspend fun getWordCount(): Int = wordDao.getWordCount()
}
