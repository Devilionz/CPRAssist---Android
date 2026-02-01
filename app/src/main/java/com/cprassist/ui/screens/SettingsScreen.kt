package com.cprassist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cprassist.data.preferences.AppSettings
import com.cprassist.ui.theme.EmergencyRed
import com.cprassist.ui.theme.ProfessionalBlue
import com.cprassist.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset to defaults",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProfessionalBlue,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Metronome Settings
            SettingsSection(title = "Metronome") {
                SliderSetting(
                    label = "Default BPM",
                    value = settings.defaultBpm.toFloat(),
                    valueRange = 100f..120f,
                    steps = 19,
                    valueLabel = "${settings.defaultBpm} BPM",
                    onValueChange = { viewModel.setDefaultBpm(it.toInt()) }
                )

                SliderSetting(
                    label = "Volume",
                    value = settings.metronomeVolume.toFloat(),
                    valueRange = 0f..100f,
                    valueLabel = "${settings.metronomeVolume}%",
                    onValueChange = { viewModel.setMetronomeVolume(it.toInt()) }
                )

                SwitchSetting(
                    label = "Sound",
                    description = "Play audio click for each beat",
                    checked = settings.useMetronomeSound,
                    onCheckedChange = { viewModel.setUseMetronomeSound(it) }
                )

                SwitchSetting(
                    label = "Vibration",
                    description = "Vibrate for each beat",
                    checked = settings.useMetronomeVibration,
                    onCheckedChange = { viewModel.setUseMetronomeVibration(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voice Guidance Settings
            SettingsSection(title = "Voice Guidance (TTS)") {
                SwitchSetting(
                    label = "Enable Voice Guidance",
                    description = "Spoken instructions during CPR",
                    checked = settings.ttsEnabled,
                    onCheckedChange = { viewModel.setTtsEnabled(it) }
                )

                SliderSetting(
                    label = "Speech Rate",
                    value = settings.ttsSpeechRate,
                    valueRange = 0.5f..1.5f,
                    valueLabel = when {
                        settings.ttsSpeechRate < 0.8f -> "Slow"
                        settings.ttsSpeechRate > 1.1f -> "Fast"
                        else -> "Normal"
                    },
                    onValueChange = { viewModel.setTtsSpeechRate(it) },
                    enabled = settings.ttsEnabled
                )

                SliderSetting(
                    label = "Voice Volume",
                    value = settings.ttsVolume.toFloat(),
                    valueRange = 0f..100f,
                    valueLabel = "${settings.ttsVolume}%",
                    onValueChange = { viewModel.setTtsVolume(it.toInt()) },
                    enabled = settings.ttsEnabled
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cycle Alert Settings
            SettingsSection(title = "2-Minute Cycle Alert") {
                SwitchSetting(
                    label = "Enable Cycle Alert",
                    description = "Beep every 2 minutes to switch rescuers",
                    checked = settings.cycleAlertEnabled,
                    onCheckedChange = { viewModel.setCycleAlertEnabled(it) }
                )

                SliderSetting(
                    label = "Alert Volume",
                    value = settings.cycleAlertVolume.toFloat(),
                    valueRange = 0f..100f,
                    valueLabel = "${settings.cycleAlertVolume}%",
                    onValueChange = { viewModel.setCycleAlertVolume(it.toInt()) },
                    enabled = settings.cycleAlertEnabled
                )

                SliderSetting(
                    label = "Cycle Interval",
                    value = settings.cycleIntervalMinutes.toFloat(),
                    valueRange = 1f..5f,
                    steps = 3,
                    valueLabel = "${settings.cycleIntervalMinutes} min",
                    onValueChange = { viewModel.setCycleIntervalMinutes(it.toInt()) },
                    enabled = settings.cycleAlertEnabled
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Settings
            SettingsSection(title = "Display") {
                SwitchSetting(
                    label = "Keep Screen On",
                    description = "Prevent screen from turning off during use",
                    checked = settings.keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) }
                )

                SwitchSetting(
                    label = "Dark Theme",
                    description = "High contrast dark mode for outdoor use",
                    checked = settings.useDarkTheme,
                    onCheckedChange = { viewModel.setUseDarkTheme(it) }
                )

                SwitchSetting(
                    label = "Show Compression Count",
                    description = "Display number of compressions",
                    checked = settings.showCompressionCount,
                    onCheckedChange = { viewModel.setShowCompressionCount(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Professional Mode Settings
            SettingsSection(title = "Professional Mode") {
                SwitchSetting(
                    label = "Auto-start Metronome",
                    description = "Start metronome when confirming arrest",
                    checked = settings.autoStartMetronome,
                    onCheckedChange = { viewModel.setAutoStartMetronome(it) }
                )

                SwitchSetting(
                    label = "Confirm Event Logging",
                    description = "Show confirmation before logging events",
                    checked = settings.confirmEventLogging,
                    onCheckedChange = { viewModel.setConfirmEventLogging(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Section
            SettingsSection(title = "About") {
                ClickableSetting(
                    label = "Privacy Policy",
                    onClick = onNavigateToPrivacyPolicy
                )
                ClickableSetting(
                    label = "About CPR Assist",
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings?") },
            text = { Text("This will restore all settings to their default values.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = EmergencyRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ProfessionalBlue
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SwitchSetting(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ProfessionalBlue,
                checkedTrackColor = ProfessionalBlue.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = valueLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) ProfessionalBlue
                else ProfessionalBlue.copy(alpha = 0.5f)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = ProfessionalBlue,
                activeTrackColor = ProfessionalBlue
            )
        )
    }
}

@Composable
private fun ClickableSetting(
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}
