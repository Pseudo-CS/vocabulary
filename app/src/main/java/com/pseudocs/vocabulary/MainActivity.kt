package com.pseudocs.vocabulary

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import com.pseudocs.vocabulary.ui.VocabularyApp
import com.pseudocs.vocabulary.ui.theme.VocabularyTheme
import com.pseudocs.vocabulary.notification.WORD_ID_EXTRA

/**
 * Main entry activity. Requests notification permission on Android 13+ and
 * launches the Compose UI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var wordIdToOpen by mutableStateOf<Long?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

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
