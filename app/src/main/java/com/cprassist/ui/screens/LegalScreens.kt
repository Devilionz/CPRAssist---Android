package com.cprassist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cprassist.ui.theme.ProfessionalBlue

/**
 * Privacy Policy Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
            Text(
                text = "CPR Assist Privacy Policy",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Last updated: January 2025",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PolicySection(
                title = "1. Information Collection",
                content = """
                    CPR Assist is designed with privacy as a priority. We collect minimal data:

                    • Session Data: Event logs, timestamps, and intervention records are stored locally on your device only.

                    • No Personal Information: We do not collect names, email addresses, phone numbers, or any personally identifiable information.

                    • No Location Data: We do not access or store your location.

                    • No Network Transmission: All data remains on your device. Nothing is sent to external servers.
                """.trimIndent()
            )

            PolicySection(
                title = "2. Data Storage",
                content = """
                    • Local Storage Only: All session history and settings are stored locally on your device using Android's secure storage mechanisms.

                    • Export Control: You have full control over exporting data. Exported files can be shared via your device's share functionality.

                    • Data Deletion: Uninstalling the app removes all stored data. You can also clear data manually from Settings.
                """.trimIndent()
            )

            PolicySection(
                title = "3. Permissions",
                content = """
                    CPR Assist requires the following permissions:

                    • VIBRATE: For metronome haptic feedback
                    • WAKE_LOCK: To keep the screen on during emergencies
                    • FOREGROUND_SERVICE: To run the metronome reliably
                    • POST_NOTIFICATIONS: To show service status

                    No permissions are used to collect or transmit personal data.
                """.trimIndent()
            )

            PolicySection(
                title = "4. Third-Party Services",
                content = """
                    CPR Assist does not integrate with any third-party analytics, advertising, or tracking services.

                    The app uses Android's built-in Text-to-Speech engine, which operates locally on your device.
                """.trimIndent()
            )

            PolicySection(
                title = "5. Children's Privacy",
                content = """
                    This app is not directed at children under 13. However, as we collect no personal information, there are no specific risks to children's privacy.
                """.trimIndent()
            )

            PolicySection(
                title = "6. Changes to This Policy",
                content = """
                    We may update this privacy policy from time to time. Any changes will be reflected in the "Last updated" date above.
                """.trimIndent()
            )

            PolicySection(
                title = "7. Contact",
                content = """
                    If you have questions about this privacy policy, please contact us through the app's support channel.
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * About Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
            Text(
                text = "CPR Assist",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Version 1.0.0",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            AboutSection(
                title = "Purpose",
                content = """
                    CPR Assist is designed to help save lives during cardiac arrest emergencies. It provides:

                    • Metronome-guided CPR for proper compression rate (100-120 BPM)
                    • Voice guidance for untrained bystanders
                    • Professional event logging for trained responders
                    • Exportable reports for medical documentation
                """.trimIndent()
            )

            AboutSection(
                title = "Guidelines",
                content = """
                    This app follows current American Heart Association (AHA) guidelines for CPR and cardiac arrest management.

                    Key recommendations implemented:
                    • Compression rate: 100-120 per minute
                    • Compression depth: At least 2 inches (5 cm)
                    • Allow complete chest recoil
                    • Minimize interruptions
                    • Switch rescuers every 2 minutes
                """.trimIndent()
            )

            AboutSection(
                title = "Disclaimer",
                content = """
                    This app is a supplementary tool and is NOT a substitute for:

                    • Professional CPR training and certification
                    • Emergency medical services (call 911 first!)
                    • Medical professional judgment
                    • Hands-on practice with manikins

                    Always follow the protocols of your certifying organization.
                """.trimIndent()
            )

            AboutSection(
                title = "Acknowledgments",
                content = """
                    • American Heart Association for CPR guidelines
                    • Emergency medical professionals who provided feedback
                    • All first responders and bystanders who help save lives
                """.trimIndent()
            )

            AboutSection(
                title = "Open Source",
                content = """
                    CPR Assist is built with modern Android technologies:

                    • Kotlin & Jetpack Compose
                    • Material Design 3
                    • Android Architecture Components
                    • Room Database
                    • DataStore Preferences
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Made with ❤️ for first responders and bystanders everywhere",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: String
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ProfessionalBlue
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp
        )
    }
}
