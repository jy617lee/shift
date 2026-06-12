package com.schedule.shift.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.schedule.shift.ui.confirmation.ConfirmationScreen
import com.schedule.shift.ui.confirmation.ConfirmationViewModel
import com.schedule.shift.ui.home.HomeScreen
import com.schedule.shift.ui.privacy.PrivacyPolicyScreen
import com.schedule.shift.ui.registration.RegistrationScreen
import com.schedule.shift.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val REGISTRATION = "registration"
    const val CONFIRMATION = "confirmation"
    const val SETTINGS = "settings"
    const val PRIVACY_POLICY = "privacy_policy"
}

@Composable
fun ShiftNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddSchedule = { navController.navigate(Routes.REGISTRATION) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onPrivacyPolicy = { navController.navigate(Routes.PRIVACY_POLICY) },
            )
        }
        composable(Routes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REGISTRATION) { backStackEntry ->
            RegistrationDestination(navController = navController, backStackEntry = backStackEntry)
        }
        composable(Routes.CONFIRMATION) { backStackEntry ->
            ConfirmationDestination(navController = navController, backStackEntry = backStackEntry)
        }
    }
}

@Composable
private fun RegistrationDestination(
    navController: NavHostController,
    backStackEntry: androidx.navigation.NavBackStackEntry,
) {
    val homeEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.HOME) }
    val flowHolder: RegistrationFlowStateHolder = hiltViewModel(homeEntry)
    val skipConfirm by flowHolder.skipConfirm.collectAsStateWithLifecycle()
    RegistrationScreen(
        onParsed = { weeks, imageUri, sessionId, sessionStartMs ->
            flowHolder.setPendingWeeks(weeks)
            flowHolder.setPendingImageUri(imageUri)
            flowHolder.setSession(sessionId, sessionStartMs)
            flowHolder.setReplace(false)
            if (skipConfirm) {
                flowHolder.autoSave(weeks)
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
            } else {
                navController.navigate(Routes.CONFIRMATION)
            }
        },
        onBack = { navController.popBackStack() },
    )
}

@Composable
private fun ConfirmationDestination(
    navController: NavHostController,
    backStackEntry: androidx.navigation.NavBackStackEntry,
) {
    val homeEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.HOME) }
    val flowHolder: RegistrationFlowStateHolder = hiltViewModel(homeEntry)
    val weeks = flowHolder.pendingWeeks
    val imageUri = flowHolder.pendingImageUri

    if (weeks.isEmpty()) {
        navController.popBackStack()
        return
    }

    val viewModel: ConfirmationViewModel = hiltViewModel(
        creationCallback = { factory: ConfirmationViewModel.Factory ->
            factory.create(
                weeks = weeks,
                imageUri = imageUri,
                sessionId = flowHolder.pendingSessionId,
                sessionStartMs = flowHolder.pendingSessionStartMs,
                replace = flowHolder.pendingReplace,
            )
        },
    )
    ConfirmationScreen(
        viewModel = viewModel,
        onSaved = {
            flowHolder.clear()
            navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
        },
        onCancelled = {
            flowHolder.clear()
            navController.popBackStack()
        },
    )
}
