package com.pseudocs.vocabulary.data.model

/**
 * Represents a fetched example sentence with its source information.
 */
data class ExampleSentence(
    val text: String,
    val source: String = "" // e.g. "Wordnik", "Gemini"
)
