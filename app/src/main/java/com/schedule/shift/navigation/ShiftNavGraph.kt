package com.schedule.shift.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.schedule.shift.ui.registration.RegistrationScreen
import com.schedule.shift.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val REGISTRATION = "registration"
    const val CONFIRMATION = "confirmation"
    const val SETTINGS = "settings"
}

@Composable
fun ShiftNavGraph(
    navController: NavHostController = rememberNavController(),
    openRegistration: Boolean = false,
) {
    LaunchedEffect(Unit) {
        if (openRegistration) navController.navigate(Routes.REGISTRATION)
    }
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { backStackEntry ->
            val flowHolder: RegistrationFlowStateHolder = hiltViewModel(backStackEntry)
            val homeRefreshNeeded by flowHolder.homeRefreshNeeded.collectAsStateWithLifecycle()
            HomeScreen(
                onAddSchedule = { navController.navigate(Routes.REGISTRATION) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                refreshTrigger = homeRefreshNeeded,
                onRefreshConsumed = flowHolder::clearHomeRefresh,
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
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
    val pendingAction by flowHolder.pendingAction.collectAsStateWithLifecycle()
    LaunchedEffect(pendingAction) {
        if (pendingAction == FlowPendingAction.GoToConfirmation) {
            flowHolder.resetAction()
            navController.navigate(Routes.CONFIRMATION)
        }
    }
    RegistrationScreen(
        onParsed = { weeks, imageUri -> flowHolder.handleParsed(weeks, imageUri) },
        onBack = { navController.popBackStack() },
        skipImageHandler = if (skipConfirm) flowHolder::startSkipSave else null,
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
            factory.create(weeks, imageUri)
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
            navController.navigate(Routes.REGISTRATION) {
                popUpTo(Routes.REGISTRATION) { inclusive = true }
            }
        },
    )
}
