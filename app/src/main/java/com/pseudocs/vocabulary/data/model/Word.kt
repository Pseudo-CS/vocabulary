package com.pseudocs.vocabulary.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a vocabulary word stored locally in the Room database.
 */
@Entity(
    tableName = "words",
    indices = [Index(value = ["word"], unique = true)]
)
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val addedAt: Long = System.currentTimeMillis()
)
