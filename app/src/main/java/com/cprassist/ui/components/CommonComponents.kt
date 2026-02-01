package com.cprassist.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cprassist.ui.theme.ButtonAirway
import com.cprassist.ui.theme.ButtonIntervention
import com.cprassist.ui.theme.ButtonOutcome
import com.cprassist.ui.theme.ButtonRhythm
import com.cprassist.ui.theme.EmergencyRed
import com.cprassist.ui.theme.ROSCGreen
import com.cprassist.ui.theme.WarningAmber

/**
 * Large, glove-friendly button for critical actions
 */
@Composable
fun EmergencyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = EmergencyRed,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    fontSize: TextUnit = 24.sp,
    minHeight: Dp = 80.dp
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(minHeight),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.4f),
            disabledContentColor = textColor.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Pulsating button for urgent actions
 */
@Composable
fun PulsatingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = EmergencyRed,
    isPulsating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsating) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Event logging button - compact but still glove-friendly
 */
@Composable
fun EventButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    category: EventButtonCategory = EventButtonCategory.INTERVENTION,
    isCompact: Boolean = false
) {
    val backgroundColor = when (category) {
        EventButtonCategory.AIRWAY -> ButtonAirway
        EventButtonCategory.RHYTHM -> ButtonRhythm
        EventButtonCategory.INTERVENTION -> ButtonIntervention
        EventButtonCategory.OUTCOME -> ButtonOutcome
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .height(if (isCompact) 56.dp else 64.dp)
            .padding(2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = if (isCompact) 14.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

enum class EventButtonCategory {
    AIRWAY,
    RHYTHM,
    INTERVENTION,
    OUTCOME
}

/**
 * Timer display with large, readable numbers
 */
@Composable
fun TimerDisplay(
    time: String,
    label: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 48.sp,
    isHighlighted: Boolean = false
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) WarningAmber.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = tween(300),
        label = "timerBg"
    )

    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = time,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

/**
 * Visual metronome indicator
 */
@Composable
fun MetronomeIndicator(
    isActive: Boolean,
    bpm: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "metronome")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = if (isActive) 1.2f else 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isActive) (60000 / bpm) else 500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beat"
    )

    val color by animateColorAsState(
        targetValue = if (isActive) EmergencyRed else Color.Gray,
        animationSpec = tween(100),
        label = "color"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(scale)
                .background(color, CircleShape)
                .border(2.dp, Color.White, CircleShape)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$bpm BPM",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * ROSC indicator - shows when patient has pulse
 */
@Composable
fun ROSCIndicator(
    roscTime: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ROSCGreen, RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ROSC",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = roscTime,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

/**
 * Status bar showing arrest time and cycle
 */
@Composable
fun ArrestStatusBar(
    totalArrestTime: String,
    episodeTime: String,
    cycleNumber: Int,
    episodeNumber: Int = 1,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Top row: Total time and Cycle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TOTAL:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmergencyRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = totalArrestTime,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CYCLE:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = cycleNumber.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Bottom row: Episode time (only show if episode > 1 or always show episode time)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (episodeNumber > 1) "EPISODE $episodeNumber:" else "EPISODE:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = episodeTime,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Last event indicator
 */
@Composable
fun LastEventIndicator(
    eventName: String,
    eventTime: String,
    totalEvents: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Last: $eventName @ $eventTime",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "Events: $totalEvents",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Switch rescuer alert overlay
 */
@Composable
fun SwitchRescuerAlert(
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(WarningAmber, RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SWITCH RESCUER",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "2 minutes - swap if possible",
                fontSize = 16.sp,
                color = Color.Black.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAcknowledge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.2f)
                )
            ) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Medical disclaimer text
 */
@Composable
fun MedicalDisclaimer(
    modifier: Modifier = Modifier
) {
    Text(
        text = "This app is not a replacement for professional medical training. " +
                "Always call emergency services (911) immediately.",
        modifier = modifier.padding(16.dp),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}
