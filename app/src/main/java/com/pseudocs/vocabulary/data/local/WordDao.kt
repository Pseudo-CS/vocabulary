package com.pseudocs.vocabulary.data.local

import androidx.room.*
import com.pseudocs.vocabulary.data.model.Word
import kotlinx.coroutines.flow.Flow

/**
 * DAO for all vocabulary word operations.
 */
@Dao
interface WordDao {

    @Query("SELECT * FROM words ORDER BY addedAt DESC")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE word LIKE '%' || :query || '%' ORDER BY addedAt DESC")
    fun searchWords(query: String): Flow<List<Word>>

    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomWord(): Word?

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getWordById(id: Long): Word?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWord(word: Word): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWords(words: List<Word>)

    @Update
    suspend fun updateWord(word: Word)

    @Delete
    suspend fun deleteWord(word: Word)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int
}
