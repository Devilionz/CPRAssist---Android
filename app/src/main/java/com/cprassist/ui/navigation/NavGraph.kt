package com.cprassist.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cprassist.ui.screens.EventSummaryScreen
import com.cprassist.ui.screens.ProfessionalModeScreen
import com.cprassist.ui.screens.SessionHistoryScreen

/**
 * Navigation routes for the Professional Cardiac Arrest Timer app.
 *
 * This app is for trained medical professionals only.
 * All consumer/bystander CPR guidance features have been removed.
 */
sealed class Screen(val route: String) {
    object ProfessionalMode : Screen("professional_mode")
    object EventSummary : Screen("event_summary")
    object SessionHistory : Screen("session_history")
}

@Composable
fun CPRAssistNavGraph(
    navController: NavHostController = rememberNavController()
) {
    // Launch directly into Professional Mode - no mode selection, no onboarding
    NavHost(
        navController = navController,
        startDestination = Screen.ProfessionalMode.route
    ) {
        // Professional Mode Screen - Main entry point
        composable(Screen.ProfessionalMode.route) {
            ProfessionalModeScreen(
                onBack = {
                    // No back navigation - this is the start screen
                    // Could show a confirmation dialog if needed
                },
                onViewSummary = {
                    navController.navigate(Screen.EventSummary.route)
                },
                onViewHistory = {
                    navController.navigate(Screen.SessionHistory.route)
                }
            )
        }

        // Event Summary Screen
        composable(Screen.EventSummary.route) {
            EventSummaryScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNewEvent = {
                    // Return to Professional Mode for a new event
                    navController.navigate(Screen.ProfessionalMode.route) {
                        popUpTo(Screen.ProfessionalMode.route) { inclusive = true }
                    }
                }
            )
        }

        // Session History Screen (for reviewing past sessions)
        composable(Screen.SessionHistory.route) {
            SessionHistoryScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
