package com.schedule.shift.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.schedule.shift.ui.confirmation.ConfirmationScreen
import com.schedule.shift.ui.confirmation.ConfirmationViewModel
import com.schedule.shift.ui.home.HomeScreen
import com.schedule.shift.ui.registration.RegistrationScreen

private object Routes {
    const val HOME = "home"
    const val REGISTRATION = "registration"
    const val CONFIRMATION = "confirmation"
}

@Composable
fun ShiftNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onAddSchedule = { navController.navigate(Routes.REGISTRATION) })
        }
        composable(Routes.REGISTRATION) { backStackEntry ->
            val homeEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.HOME) }
            val flowHolder: RegistrationFlowStateHolder = hiltViewModel(homeEntry)
            RegistrationScreen(
                onParsed = { weeks ->
                    flowHolder.setPendingWeeks(weeks)
                    navController.navigate(Routes.CONFIRMATION)
                },
            )
        }
        composable(Routes.CONFIRMATION) { backStackEntry ->
            ConfirmationDestination(navController = navController, backStackEntry = backStackEntry)
        }
    }
}

@Composable
private fun ConfirmationDestination(
    navController: NavHostController,
    backStackEntry: androidx.navigation.NavBackStackEntry,
) {
    val homeEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.HOME) }
    val flowHolder: RegistrationFlowStateHolder = hiltViewModel(homeEntry)
    val weeks = flowHolder.pendingWeeks

    if (weeks.isEmpty()) {
        navController.popBackStack()
        return
    }

    val viewModel: ConfirmationViewModel = hiltViewModel(
        creationCallback = { factory: ConfirmationViewModel.Factory -> factory.create(weeks) },
    )
    ConfirmationScreen(
        viewModel = viewModel,
        onSaved = {
            navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
        },
        onCancelled = { navController.popBackStack() },
    )
}
