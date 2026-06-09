package com.pseudocs.vocabulary.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pseudocs.vocabulary.data.model.Word
import com.pseudocs.vocabulary.notification.DailyNotificationWorker
import com.pseudocs.vocabulary.ui.theme.*
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Main vocabulary list screen with search, add, edit, delete and TXT import.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    onWordClick: (Long) -> Unit,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<Word?>(null) }
    var deletingWord by remember { mutableStateOf<Word?>(null) }

    // File picker for TXT import
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val text = BufferedReader(InputStreamReader(inputStream)).readText()
                viewModel.importFromText(text)
            } catch (e: Exception) {
                // handled in VM
            }
        }
    }

    // File picker for TXT export (backup)
    val exportFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportToUri(context.contentResolver, it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Surface, Background)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "My Vocabulary",
                                style = MaterialTheme.typography.headlineLarge,
                                color = OnBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.words.size} word${if (uiState.words.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                        }
                        // Action buttons (Notification & Import)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Manual Notification button
                            IconButton(
                                onClick = { DailyNotificationWorker.triggerImmediately(context) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SurfaceVariant)
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Trigger Notification",
                                    tint = Primary
                                )
                            }

                            // Import button
                            IconButton(
                                onClick = { filePicker.launch("text/*") },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SurfaceVariant)
                            ) {
                                Icon(
                                    Icons.Default.FileUpload,
                                    contentDescription = "Import TXT",
                                    tint = Primary
                                )
                            }

                            // Export button
                            IconButton(
                                onClick = { exportFilePicker.launch("vocabulary_backup.txt") },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SurfaceVariant)
                            ) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = "Export TXT",
                                    tint = Primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::updateSearch,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Search words...", color = OnSurfaceVariant)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = OnSurfaceVariant)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearch("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = OnSurfaceVariant)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = SurfaceVariant,
                            focusedContainerColor = SurfaceVariant,
                            unfocusedContainerColor = SurfaceVariant,
                            focusedTextColor = OnBackground,
                            unfocusedTextColor = OnBackground
                        )
                    )
                }
            }

            // Word list
            if (uiState.words.isEmpty()) {
                EmptyState(hasQuery = uiState.searchQuery.isNotEmpty())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.words, key = { it.id }) { word ->
                        WordListItem(
                            word = word,
                            onClick = { onWordClick(word.id) },
                            onEdit = { editingWord = word },
                            onDelete = { deletingWord = word }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Snackbar for messages
        if (uiState.message != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = SurfaceVariant,
                contentColor = OnSurface
            ) {
                Text(uiState.message!!)
            }
        }

        // FAB - Add word
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Primary,
            contentColor = OnPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Word")
        }
    }

    // Add Word Dialog
    if (showAddDialog) {
        WordInputDialog(
            title = "Add Word",
            initialValue = "",
            onConfirm = { text ->
                viewModel.addWord(text)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Edit Word Dialog
    editingWord?.let { word ->
        WordInputDialog(
            title = "Edit Word",
            initialValue = word.word,
            onConfirm = { text ->
                viewModel.updateWord(word.id, text)
                editingWord = null
            },
            onDismiss = { editingWord = null }
        )
    }

    // Delete Confirmation Dialog
    deletingWord?.let { word ->
        AlertDialog(
            onDismissRequest = { deletingWord = null },
            containerColor = Surface,
            title = {
                Text("Delete Word", color = OnSurface)
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${word.word}\"?",
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWord(word)
                    deletingWord = null
                }) {
                    Text("Delete", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingWord = null }) {
                    Text("Cancel", color = Primary)
                }
            }
        )
    }
}

@Composable
private fun WordListItem(
    word: Word,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Letter avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Primary, Accent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = word.word.first().uppercaseChar().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = word.word,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Action buttons
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = OnSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Error)
            }
        }
    }
}

@Composable
private fun EmptyState(hasQuery: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (hasQuery) Icons.Default.SearchOff else Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (hasQuery) "No words match your search" else "No words yet",
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurfaceVariant
            )
            if (!hasQuery) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to add your first word",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WordInputDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text(title, color = OnSurface) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Word") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                    focusedLabelColor = Primary,
                    unfocusedLabelColor = OnSurfaceVariant
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Save", color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnSurfaceVariant)
            }
        }
    )
}
