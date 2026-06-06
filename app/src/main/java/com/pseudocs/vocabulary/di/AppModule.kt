package com.pseudocs.vocabulary.di

import android.content.Context
import androidx.room.Room
import com.pseudocs.vocabulary.data.local.SettingsDataStore
import com.pseudocs.vocabulary.data.local.VocabularyDatabase
import com.pseudocs.vocabulary.data.local.WordDao
import com.pseudocs.vocabulary.data.repository.SentenceRepository
import com.pseudocs.vocabulary.data.repository.WordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing all application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VocabularyDatabase =
        Room.databaseBuilder(
            context,
            VocabularyDatabase::class.java,
            "vocabulary_database"
        )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideWordDao(database: VocabularyDatabase): WordDao = database.wordDao()

    @Provides
    @Singleton
    fun provideWordRepository(wordDao: WordDao): WordRepository = WordRepository(wordDao)

    @Provides
    @Singleton
    fun provideSentenceRepository(): SentenceRepository = SentenceRepository()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)
}
