package com.cprassist.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cprassist.data.models.CPRGuidanceState
import com.cprassist.ui.components.EmergencyButton
import com.cprassist.ui.components.MedicalDisclaimer
import com.cprassist.ui.components.MetronomeIndicator
import com.cprassist.ui.components.SwitchRescuerAlert
import com.cprassist.ui.components.TimerDisplay
import com.cprassist.ui.theme.EmergencyRed
import com.cprassist.ui.theme.ProfessionalBlue
import com.cprassist.ui.theme.WarningAmber
import com.cprassist.ui.viewmodel.CPRGuidanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CPRGuidanceScreen(
    onBack: () -> Unit,
    viewModel: CPRGuidanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CPR GUIDANCE",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stop()
                        onBack()
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Exit",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmergencyRed,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (state) {
                is CPRGuidanceState.Idle -> {
                    IdleContent(
                        onStart = { viewModel.startGuidance() }
                    )
                }

                is CPRGuidanceState.Active -> {
                    ActiveGuidanceContent(
                        instruction = uiState.currentInstruction,
                        elapsedTime = uiState.formattedElapsedTime,
                        compressionCount = uiState.compressionCount,
                        bpm = uiState.metronomeConfig.bpm,
                        isMetronomeRunning = uiState.isMetronomeRunning,
                        showSwitchAlert = uiState.showSwitchAlert,
                        onBpmChange = { viewModel.updateBPM(it) },
                        onAcknowledgeSwitch = { viewModel.acknowledgeSwitch() },
                        onStop = { viewModel.stop() }
                    )
                }

                is CPRGuidanceState.Paused -> {
                    PausedContent(
                        onResume = { viewModel.resume() },
                        onStop = { viewModel.stop() }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CALL 911 FIRST",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = EmergencyRed,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "If someone is unresponsive and not breathing normally, start CPR immediately.",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        EmergencyButton(
            text = "START CPR GUIDANCE",
            onClick = onStart,
            fontSize = 24.sp,
            minHeight = 100.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        MedicalDisclaimer()
    }
}

@Composable
private fun ActiveGuidanceContent(
    instruction: String,
    elapsedTime: String,
    compressionCount: Int,
    bpm: Int,
    isMetronomeRunning: Boolean,
    showSwitchAlert: Boolean,
    onBpmChange: (Int) -> Unit,
    onAcknowledgeSwitch: () -> Unit,
    onStop: () -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(bpm.toFloat()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main instruction
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EmergencyRed, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = instruction,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Metronome visual indicator
            MetronomeIndicator(
                isActive = isMetronomeRunning,
                bpm = bpm
            )

            Spacer(modifier = Modifier.height(24.dp))

            // BPM Slider
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Compression Rate: ${sliderPosition.toInt()} BPM",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = { onBpmChange(sliderPosition.toInt()) },
                    valueRange = 100f..120f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = EmergencyRed,
                        activeTrackColor = EmergencyRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("100", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("120", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimerDisplay(
                    time = elapsedTime,
                    label = "TIME"
                )

                TimerDisplay(
                    time = compressionCount.toString(),
                    label = "COMPRESSIONS"
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Stop button
            EmergencyButton(
                text = "STOP",
                onClick = onStop,
                backgroundColor = Color.Gray,
                minHeight = 64.dp,
                fontSize = 20.sp
            )
        }

        // Switch rescuer alert overlay
        AnimatedVisibility(
            visible = showSwitchAlert,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp)
        ) {
            SwitchRescuerAlert(onAcknowledge = onAcknowledgeSwitch)
        }
    }
}

@Composable
private fun PausedContent(
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PAUSED",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = WarningAmber
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Continue CPR as soon as possible",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        EmergencyButton(
            text = "RESUME CPR",
            onClick = onResume,
            backgroundColor = ProfessionalBlue,
            minHeight = 80.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        EmergencyButton(
            text = "END SESSION",
            onClick = onStop,
            backgroundColor = Color.Gray,
            minHeight = 64.dp,
            fontSize = 18.sp
        )
    }
}
