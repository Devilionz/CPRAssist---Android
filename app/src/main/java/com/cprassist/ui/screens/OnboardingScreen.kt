package com.cprassist.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cprassist.ui.theme.EmergencyRed
import com.cprassist.ui.theme.ProfessionalBlue
import com.cprassist.ui.theme.ROSCGreen
import com.cprassist.ui.theme.WarningAmber

/**
 * Onboarding flow for first-time users
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 4

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentPage) {
            0 -> WelcomePage(
                onNext = { currentPage = 1 }
            )
            1 -> FeaturesPage(
                onNext = { currentPage = 2 },
                onBack = { currentPage = 0 }
            )
            2 -> HowToUsePage(
                onNext = { currentPage = 3 },
                onBack = { currentPage = 1 }
            )
            3 -> DisclaimerPage(
                onAccept = onComplete,
                onBack = { currentPage = 2 }
            )
        }

        // Page indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(totalPages) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (index == currentPage) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentPage) ProfessionalBlue
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = EmergencyRed
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "CPR ASSIST",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cardiac Arrest Management",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "This app helps you perform effective CPR and manage cardiac arrest emergencies.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ProfessionalBlue
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeaturesPage(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Two Modes",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureCard(
            icon = Icons.Default.Favorite,
            title = "CPR Guidance",
            description = "Step-by-step voice guidance for bystanders with no training. Includes metronome and spoken instructions.",
            color = EmergencyRed
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            icon = Icons.Default.Timer,
            title = "Professional Mode",
            description = "For paramedics and trained responders. Event logging, arrest timers, rhythm tracking, and exportable reports.",
            color = ProfessionalBlue
        )

        Spacer(modifier = Modifier.weight(1f))

        NavigationButtons(
            onBack = onBack,
            onNext = onNext,
            backText = "Back",
            nextText = "Next"
        )

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
private fun HowToUsePage(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Key Features",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureItem(
            icon = Icons.Default.Timer,
            title = "Metronome",
            description = "100-120 BPM rhythm guide for proper compression rate"
        )

        FeatureItem(
            icon = Icons.Default.Favorite,
            title = "2-Minute Alerts",
            description = "Automatic reminders to switch rescuers and check rhythm"
        )

        FeatureItem(
            icon = Icons.Default.Check,
            title = "Event Logging",
            description = "One-tap logging of interventions, rhythms, and medications"
        )

        FeatureItem(
            icon = Icons.Default.Favorite,
            title = "ROSC Tracking",
            description = "Track return of spontaneous circulation with re-arrest support"
        )

        Spacer(modifier = Modifier.weight(1f))

        NavigationButtons(
            onBack = onBack,
            onNext = onNext,
            backText = "Back",
            nextText = "Next"
        )

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
private fun DisclaimerPage(
    onAccept: () -> Unit,
    onBack: () -> Unit
) {
    var accepted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = WarningAmber
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Medical Disclaimer",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DisclaimerText(
                    "This application is designed as a supplementary aid and is NOT a substitute for professional medical training, certification, or emergency medical services."
                )

                Spacer(modifier = Modifier.height(12.dp))

                DisclaimerText(
                    "ALWAYS call emergency services (911 or local equivalent) immediately in a cardiac arrest situation."
                )

                Spacer(modifier = Modifier.height(12.dp))

                DisclaimerText(
                    "The developers and publishers of this application assume no liability for outcomes resulting from its use. CPR quality depends on proper training and technique."
                )

                Spacer(modifier = Modifier.height(12.dp))

                DisclaimerText(
                    "This app follows current AHA (American Heart Association) guidelines but guidelines may change. Always follow the most current protocols from your certifying organization."
                )

                Spacer(modifier = Modifier.height(12.dp))

                DisclaimerText(
                    "Professional Mode is intended for trained medical personnel only. Untrained users should use CPR Guidance Mode."
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = accepted,
                onCheckedChange = { accepted = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = ROSCGreen
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "I understand and accept these terms",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        NavigationButtons(
            onBack = onBack,
            onNext = onAccept,
            backText = "Back",
            nextText = "Accept & Continue",
            nextEnabled = accepted
        )

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = ProfessionalBlue
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DisclaimerText(text: String) {
    Text(
        text = "• $text",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 20.sp
    )
}

@Composable
private fun NavigationButtons(
    onBack: () -> Unit,
    onNext: () -> Unit,
    backText: String,
    nextText: String,
    nextEnabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(backText, fontSize = 16.sp)
        }

        Button(
            onClick = onNext,
            enabled = nextEnabled,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ProfessionalBlue
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(nextText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
