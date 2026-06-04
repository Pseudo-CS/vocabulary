package com.pseudocs.vocabulary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pseudocs.vocabulary.data.model.Word

/**
 * Room database for the Vocabulary app.
 */
@Database(entities = [Word::class], version = 1, exportSchema = false)
abstract class VocabularyDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
}
