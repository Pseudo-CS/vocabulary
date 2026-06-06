package com.pseudocs.vocabulary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import com.pseudocs.vocabulary.ui.VocabularyApp
import com.pseudocs.vocabulary.ui.theme.VocabularyTheme
import com.pseudocs.vocabulary.notification.WORD_ID_EXTRA

/**
 * Main entry activity. Launches the Compose UI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var wordIdToOpen by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            VocabularyTheme {
                VocabularyApp(
                    wordIdToOpen = wordIdToOpen,
                    onWordIdOpened = {
                        wordIdToOpen = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(WORD_ID_EXTRA, -1L)
        if (id != null && id != -1L) {
            wordIdToOpen = id
            intent.removeExtra(WORD_ID_EXTRA)
        }
    }
}
