package com.pseudocs.vocabulary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pseudocs.vocabulary.ui.theme.*

/**
 * Screen showing a word's details and its fetched example sentence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    wordId: Long,
    onBack: () -> Unit,
    viewModel: WordDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(wordId) {
        viewModel.loadWord(wordId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Word Details", color = OnBackground) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnBackground)
                }
            },
            actions = {
                IconButton(onClick = viewModel::refreshSentence) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Primary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Word hero card — shown only when word is loaded
            val currentWord = uiState.word
            if (currentWord != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Primary, Accent)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentWord.word.uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp
                        )
                        if (!uiState.phonetic.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.phonetic!!,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.85f),
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }

            // Definition Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Definition",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!uiState.partOfSpeech.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = uiState.partOfSpeech!!.lowercase(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = SurfaceVariant)

                    when {
                        uiState.isLoadingDefinition -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Primary)
                            }
                        }
                        uiState.definitionError != null -> {
                            Text(
                                text = uiState.definitionError!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                        }
                        uiState.definition != null -> {
                            Text(
                                text = uiState.definition!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = OnSurface,
                                lineHeight = 26.sp
                            )
                        }
                        else -> {
                            Text(
                                text = "Definition not found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Source badge
            if (uiState.sourceLabel.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceVariant
                ) {
                    Text(
                        text = "Source: ${uiState.sourceLabel}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary
                    )
                }
            }

            // Example sentence card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Example Sentence",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    HorizontalDivider(color = SurfaceVariant)

                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Primary)
                            }
                        }
                        uiState.error != null -> {
                            Text(
                                text = uiState.error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Error
                            )
                        }
                        uiState.sentence != null -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = uiState.sentence!!.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = OnSurface,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 26.sp
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = "Tap the refresh button to fetch an example sentence.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Refresh button
            Button(
                onClick = viewModel::refreshSentence,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    disabledContainerColor = SurfaceVariant,
                    disabledContentColor = OnSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fetch New Example")
            }
        }
    }
}
