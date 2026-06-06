package com.pseudocs.vocabulary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pseudocs.vocabulary.data.local.SentenceSource
import com.pseudocs.vocabulary.ui.theme.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Settings screen for configuring notification time, sentence source, and API keys.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleNotifications(true)
        } else {
            android.widget.Toast.makeText(
                context,
                "Notification permission is required to receive daily words.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(colors = listOf(Surface, Background))
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = OnBackground,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Notifications Section ──
            SettingsSectionHeader("Notifications")

            SettingsCard {
                // Enable/Disable toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Daily Word Notification",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurface
                        )
                        Text(
                            "Get a word of the day notification",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    
                                    if (hasPermission) {
                                        viewModel.toggleNotifications(true)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    viewModel.toggleNotifications(true)
                                }
                            } else {
                                viewModel.toggleNotifications(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnPrimary,
                            checkedTrackColor = Primary
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = SurfaceVariant
                )

                // Time picker row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showTimePicker = true
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Notification Time",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurface
                        )
                        Text(
                            "Tap to change time",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceVariant
                    ) {
                        Text(
                            text = "%02d:%02d".format(settings.notificationHour, settings.notificationMinute),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Sentence Source Section ──
            SettingsSectionHeader("Sentence Source")

            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Choose where to fetch example sentences",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SentenceSource.entries.forEach { source ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateSentenceSource(source.name) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.sentenceSource == source.name,
                                onClick = { viewModel.updateSentenceSource(source.name) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Primary,
                                    unselectedColor = OnSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = when (source) {
                                        SentenceSource.WORDNIK -> "Wordnik"
                                        SentenceSource.GEMINI -> "Gemini"
                                        SentenceSource.VOCABULARY_COM -> "Vocabulary.com"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurface
                                )
                                Text(
                                    text = when (source) {
                                        SentenceSource.WORDNIK -> "Real-world example sentences"
                                        SentenceSource.GEMINI -> "AI-generated natural sentences"
                                        SentenceSource.VOCABULARY_COM -> "Web-scraped example sentences (no API key required)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── API Keys Section ──
            SettingsSectionHeader("API Keys")

            SettingsCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ApiKeyField(
                        label = "Wordnik API Key",
                        value = settings.wordnikApiKey,
                        onValueChange = viewModel::updateWordnikApiKey,
                        placeholder = "Enter Wordnik API key"
                    )

                    ApiKeyField(
                        label = "Gemini API Key",
                        value = settings.geminiApiKey,
                        onValueChange = viewModel::updateGeminiApiKey,
                        placeholder = "Enter Gemini API key"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = settings.notificationHour,
            initialMinute = settings.notificationMinute,
            onConfirm = { hour, minute ->
                viewModel.updateNotificationTime(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = Primary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = OnSurfaceVariant) },
        visualTransformation = if (passwordVisible) VisualTransformation.None
        else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Hide key" else "Show key",
                    tint = OnSurfaceVariant
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = SurfaceVariant,
            focusedContainerColor = SurfaceVariant,
            unfocusedContainerColor = SurfaceVariant,
            focusedTextColor = OnBackground,
            unfocusedTextColor = OnBackground,
            focusedLabelColor = Primary,
            unfocusedLabelColor = OnSurfaceVariant
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = {
            Text(
                "Set Notification Time",
                color = OnSurface,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = SurfaceVariant,
                        clockDialSelectedContentColor = OnPrimary,
                        clockDialUnselectedContentColor = OnSurface,
                        selectorColor = Primary,
                        timeSelectorSelectedContainerColor = Primary,
                        timeSelectorUnselectedContainerColor = SurfaceVariant,
                        timeSelectorSelectedContentColor = OnPrimary,
                        timeSelectorUnselectedContentColor = OnSurface
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour, timePickerState.minute)
            }) {
                Text("Set", color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnSurfaceVariant)
            }
        }
    )
}
