package com.cprassist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cprassist.data.models.ArrestState
import com.cprassist.data.models.EventType
import com.cprassist.ui.components.ArrestStatusBar
import com.cprassist.ui.components.EmergencyButton
import com.cprassist.ui.components.EventButton
import com.cprassist.ui.components.EventButtonCategory
import com.cprassist.ui.components.LastEventIndicator
import com.cprassist.ui.components.PulsatingButton
import com.cprassist.ui.components.ROSCIndicator
import com.cprassist.ui.theme.EmergencyRed
import com.cprassist.ui.theme.ProfessionalBlue
import com.cprassist.ui.theme.ROSCGreen
import com.cprassist.ui.theme.WarningAmber
import com.cprassist.ui.viewmodel.ProfessionalModeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalModeScreen(
    onBack: () -> Unit,
    onViewSummary: () -> Unit,
    onViewHistory: () -> Unit,
    viewModel: ProfessionalModeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val arrestState by viewModel.arrestState.collectAsState()
    val events by viewModel.events.collectAsState()
    val lastEvent by viewModel.lastEvent.collectAsState()

    var showEndConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (arrestState !is ArrestState.Idle) {
                TopAppBar(
                    title = {
                        Text(
                            text = "CARDIAC ARREST",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ProfessionalBlue,
                        titleContentColor = Color.White
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (arrestState) {
                is ArrestState.Idle -> {
                    ConfirmArrestContent(
                        onConfirm = { viewModel.confirmCardiacArrest() },
                        onBack = onBack,
                        onViewHistory = onViewHistory
                    )
                }

                is ArrestState.Active -> {
                    ActiveArrestContent(
                        state = arrestState as ArrestState.Active,
                        totalTime = uiState.formattedElapsedTime,
                        episodeTime = uiState.formattedEpisodeTime,
                        cycleTime = uiState.formattedCycleTime,
                        episodeNumber = uiState.currentEpisode,
                        lastEvent = lastEvent,
                        eventCount = uiState.eventCount,
                        isPaused = (arrestState as ArrestState.Active).isPaused,
                        onLogEvent = { viewModel.logEvent(it) },
                        onPause = { viewModel.pause() },
                        onResume = { viewModel.resume() },
                        onViewLog = onViewSummary,
                        onEnd = { showEndConfirmDialog = true }
                    )
                }

                is ArrestState.ROSC -> {
                    ROSCContent(
                        roscTime = uiState.formattedRoscTime,
                        arrestTime = uiState.formattedElapsedTime,
                        onReArrest = { viewModel.handleReArrest() },
                        onEnd = { showEndConfirmDialog = true },
                        onViewLog = onViewSummary
                    )
                }

                is ArrestState.Completed -> {
                    // Reset handled by dialog - this shouldn't display
                }
            }
        }
    }

    // End confirmation dialog
    if (showEndConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEndConfirmDialog = false },
            title = { Text("End & Reset?") },
            text = { Text("This will end the current event and reset all timers. Event data will be cleared.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndConfirmDialog = false
                        viewModel.reset()
                    }
                ) {
                    Text("Reset", color = EmergencyRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ConfirmArrestContent(
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onViewHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CARDIAC ARREST TIMER",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        PulsatingButton(
            text = "CONFIRM\nCARDIAC ARREST",
            onClick = onConfirm,
            backgroundColor = EmergencyRed,
            isPulsating = false // No animation - professional UI
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "For trained medical professionals only",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        EmergencyButton(
            text = "View History",
            onClick = onViewHistory,
            backgroundColor = ProfessionalBlue,
            minHeight = 56.dp,
            fontSize = 16.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveArrestContent(
    state: ArrestState.Active,
    totalTime: String,
    episodeTime: String,
    cycleTime: String,
    episodeNumber: Int,
    lastEvent: com.cprassist.data.models.TimestampedEvent?,
    eventCount: Int,
    isPaused: Boolean,
    onLogEvent: (EventType) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onViewLog: () -> Unit,
    onEnd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Status bar with both timers
        ArrestStatusBar(
            totalArrestTime = totalTime,
            episodeTime = episodeTime,
            cycleNumber = state.currentCycle,
            episodeNumber = episodeNumber
        )

        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarningAmber)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PAUSED",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            // Airway/Access section
            SectionHeader(title = "AIRWAY / ACCESS")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventButton(
                    text = "iGel",
                    onClick = { onLogEvent(EventType.IGEL) },
                    category = EventButtonCategory.AIRWAY,
                    modifier = Modifier.weight(1f)
                )
                EventButton(
                    text = "Tube",
                    onClick = { onLogEvent(EventType.TUBE) },
                    category = EventButtonCategory.AIRWAY,
                    modifier = Modifier.weight(1f)
                )
                EventButton(
                    text = "Cannula",
                    onClick = { onLogEvent(EventType.CANNULA) },
                    category = EventButtonCategory.AIRWAY,
                    modifier = Modifier.weight(1f)
                )
                EventButton(
                    text = "IO",
                    onClick = { onLogEvent(EventType.IO) },
                    category = EventButtonCategory.AIRWAY,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rhythms section
            SectionHeader(title = "RHYTHMS")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventButton(
                    text = "VF",
                    onClick = { onLogEvent(EventType.VF) },
                    category = EventButtonCategory.RHYTHM,
                    modifier = Modifier.weight(1f)
                )
                EventButton(
                    text = "VT",
                    onClick = { onLogEvent(EventType.VT) },
                    category = EventButtonCategory.RHYTHM,
                    modifier = Modifier.weight(1f)
                )
                EventButton(
                    text = "Asystole",
                    onClick = { onLogEvent(EventType.ASYSTOLE) },
                    category = EventButtonCategory.RHYTHM,
                    modifier = Modifier.weight(1f)
                )
                EventButton(
                    text = "PEA",
                    onClick = { onLogEvent(EventType.PEA) },
                    category = EventButtonCategory.RHYTHM,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interventions section
            SectionHeader(title = "INTERVENTIONS")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventButton(
                    text = "Shock",
                    onClick = { onLogEvent(EventType.SHOCK_GIVEN) },
                    category = EventButtonCategory.INTERVENTION,
                    modifier = Modifier.weight(1f)
                )
                EventButton(
                    text = "Adrenaline",
                    onClick = { onLogEvent(EventType.ADRENALINE) },
                    category = EventButtonCategory.INTERVENTION,
                    modifier = Modifier.weight(1f)
                )
                EventButton(
                    text = "Amiodarone",
                    onClick = { onLogEvent(EventType.AMIODARONE) },
                    category = EventButtonCategory.INTERVENTION,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventButton(
                    text = "Narcan",
                    onClick = { onLogEvent(EventType.NARCAN) },
                    category = EventButtonCategory.INTERVENTION,
                    modifier = Modifier.weight(1f)
                )
                EventButton(
                    text = "Fluids",
                    onClick = { onLogEvent(EventType.FLUIDS) },
                    category = EventButtonCategory.INTERVENTION,
                    modifier = Modifier.weight(1f)
                )
                EventButton(
                    text = "TXA",
                    onClick = { onLogEvent(EventType.TXA) },
                    category = EventButtonCategory.INTERVENTION,
                    modifier = Modifier.weight(1f)
                )
                EventButton(
                    text = "Atropine",
                    onClick = { onLogEvent(EventType.ATROPINE) },
                    category = EventButtonCategory.INTERVENTION,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ROSC button
            EmergencyButton(
                text = "ROSC",
                onClick = { onLogEvent(EventType.ROSC) },
                backgroundColor = ROSCGreen,
                minHeight = 72.dp,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()

            // Last event indicator
            lastEvent?.let { event ->
                LastEventIndicator(
                    eventName = event.type.displayName,
                    eventTime = event.formatElapsedTime(),
                    totalEvents = eventCount
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EmergencyButton(
                    text = if (isPaused) "Resume" else "Pause",
                    onClick = if (isPaused) onResume else onPause,
                    backgroundColor = if (isPaused) ProfessionalBlue else Color.Gray,
                    modifier = Modifier.weight(1f),
                    minHeight = 56.dp,
                    fontSize = 16.sp
                )
                EmergencyButton(
                    text = "View Log",
                    onClick = onViewLog,
                    backgroundColor = Color.DarkGray,
                    modifier = Modifier.weight(1f),
                    minHeight = 56.dp,
                    fontSize = 16.sp
                )
                EmergencyButton(
                    text = "End",
                    onClick = onEnd,
                    backgroundColor = EmergencyRed.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                    minHeight = 56.dp,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ROSCContent(
    roscTime: String,
    arrestTime: String,
    onReArrest: () -> Unit,
    onEnd: () -> Unit,
    onViewLog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ROSCIndicator(
            roscTime = roscTime
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Show total arrest time for reference
        Text(
            text = "Total arrest time: $arrestTime",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Monitor patient",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // Re-arrest button
        EmergencyButton(
            text = "RE-ARREST",
            onClick = onReArrest,
            backgroundColor = EmergencyRed,
            minHeight = 80.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmergencyButton(
                text = "View Log",
                onClick = onViewLog,
                backgroundColor = Color.DarkGray,
                modifier = Modifier.weight(1f),
                minHeight = 56.dp,
                fontSize = 16.sp
            )
            EmergencyButton(
                text = "End Event",
                onClick = onEnd,
                backgroundColor = ProfessionalBlue,
                modifier = Modifier.weight(1f),
                minHeight = 56.dp,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
