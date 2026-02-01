package com.cprassist.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cprassist.data.models.EventCategory
import com.cprassist.data.models.EventType
import com.cprassist.data.models.TimestampedEvent
import com.cprassist.data.repository.EventRepository
import com.cprassist.ui.components.EmergencyButton
import com.cprassist.ui.theme.ButtonAirway
import com.cprassist.ui.theme.ButtonIntervention
import com.cprassist.ui.theme.ButtonOutcome
import com.cprassist.ui.theme.ButtonRhythm
import com.cprassist.ui.theme.ProfessionalBlue
import com.cprassist.ui.viewmodel.ProfessionalModeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSummaryScreen(
    onBack: () -> Unit,
    onNewEvent: () -> Unit,
    viewModel: ProfessionalModeViewModel = viewModel()
) {
    val context = LocalContext.current
    val events by viewModel.events.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Timeline", "Summary")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Log", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val intent = viewModel.exportAndShare(EventRepository.ExportFormat.TEXT)
                            context.startActivity(Intent.createChooser(intent, "Share Event Log"))
                        }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
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
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Content area - takes available space but leaves room for buttons
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> TimelineContent(events = events)
                    1 -> SummaryContent(
                        summary = viewModel.getEventSummary(),
                        events = events
                    )
                }
            }

            // Export buttons - always visible at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EmergencyButton(
                        text = "Export Text",
                        onClick = {
                            val intent = viewModel.exportAndShare(EventRepository.ExportFormat.TEXT)
                            context.startActivity(Intent.createChooser(intent, "Share as Text"))
                        },
                        backgroundColor = ProfessionalBlue,
                        modifier = Modifier.weight(1f),
                        minHeight = 56.dp,
                        fontSize = 14.sp
                    )
                    EmergencyButton(
                        text = "Export JSON",
                        onClick = {
                            val intent = viewModel.exportAndShare(EventRepository.ExportFormat.JSON)
                            context.startActivity(Intent.createChooser(intent, "Share as JSON"))
                        },
                        backgroundColor = Color.DarkGray,
                        modifier = Modifier.weight(1f),
                        minHeight = 56.dp,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                EmergencyButton(
                    text = "New Event",
                    onClick = {
                        viewModel.reset()
                        onNewEvent()
                    },
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    minHeight = 56.dp,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun TimelineContent(events: List<TimestampedEvent>) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No events recorded",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            items(events) { event ->
                EventTimelineItem(event = event)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun EventTimelineItem(event: TimestampedEvent) {
    val categoryColor = when (event.type.category) {
        EventCategory.AIRWAY_ACCESS -> ButtonAirway
        EventCategory.RHYTHM -> ButtonRhythm
        EventCategory.INTERVENTION -> ButtonIntervention
        EventCategory.OUTCOME -> ButtonOutcome
        EventCategory.SYSTEM -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time column
        Column(
            modifier = Modifier.width(70.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = event.formatElapsedTime(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = event.formatTimestamp(),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Category indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(categoryColor, RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Event details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.type.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Cycle ${event.cycleNumber}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryContent(
    summary: String,
    events: List<TimestampedEvent>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Quick stats cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Shocks",
                value = events.count { it.type == EventType.SHOCK_GIVEN }.toString(),
                color = ButtonIntervention
            )
            StatCard(
                title = "Adrenaline",
                value = events.count { it.type == EventType.ADRENALINE }.toString(),
                color = ButtonIntervention
            )
            StatCard(
                title = "Amiodarone",
                value = events.count { it.type == EventType.AMIODARONE }.toString(),
                color = ButtonIntervention
            )
            StatCard(
                title = "Total Events",
                value = events.size.toString(),
                color = ProfessionalBlue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Full text summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = summary,
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier.width(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
