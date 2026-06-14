package com.schedule.shift.navigation

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.schedule.shift.ui.confirmation.ConfirmationScreen
import com.schedule.shift.ui.confirmation.ConfirmationViewModel
import com.schedule.shift.ui.home.HomeScreen
import com.schedule.shift.ui.settings.SettingsScreen
import com.schedule.shift.ui.util.loadBitmapFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val CONFIRMATION = "confirmation"
    const val SETTINGS = "settings"
}

@Composable
fun ShiftNavGraph(
    navController: NavHostController = rememberNavController(),
    openGallery: Boolean = false,
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { backStackEntry ->
            HomeDestination(navController, backStackEntry, openGallery)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CONFIRMATION) { backStackEntry ->
            ConfirmationDestination(navController = navController, backStackEntry = backStackEntry)
        }
    }
}

@Composable
private fun HomeDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    openGallery: Boolean,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val flowHolder: RegistrationFlowStateHolder = hiltViewModel(backStackEntry)
    val homeRefreshNeeded by flowHolder.homeRefreshNeeded.collectAsStateWithLifecycle()
    val pendingAction by flowHolder.pendingAction.collectAsStateWithLifecycle()
    val isProcessingImage by flowHolder.isProcessingImage.collectAsStateWithLifecycle()
    val imageErrorMessage by flowHolder.imageErrorMessage.collectAsStateWithLifecycle()

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            coroutineScope.launch(Dispatchers.IO) {
                val bmp = loadBitmapFromUri(context, uri) ?: return@launch
                flowHolder.handleImageSelected(bmp, uri.toString())
            }
        }
    }
    val launchGallery = remember(galleryLauncher) {
        { galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) }
    }

    LaunchedEffect(Unit) { if (openGallery) launchGallery() }

    LaunchedEffect(pendingAction) {
        if (pendingAction == FlowPendingAction.GoToConfirmation) {
            flowHolder.resetAction()
            navController.navigate(Routes.CONFIRMATION)
        }
    }

    HomeScreen(
        onAddSchedule = launchGallery,
        onSettings = { navController.navigate(Routes.SETTINGS) },
        refreshTrigger = homeRefreshNeeded,
        onRefreshConsumed = flowHolder::clearHomeRefresh,
        isProcessingImage = isProcessingImage,
        imageErrorMessage = imageErrorMessage,
        onImageErrorDismiss = flowHolder::clearImageError,
    )
}

@Composable
private fun ConfirmationDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
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
            navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
        },
    )
}
