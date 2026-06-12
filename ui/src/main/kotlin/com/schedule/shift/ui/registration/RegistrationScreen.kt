package com.schedule.shift.ui.registration

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.shift.domain.model.ScheduleWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onParsed: (List<ScheduleWeek>) -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            val bitmap = loadBitmapFromUri(context, uri)
            bitmap?.let { viewModel.onImageSelected(it) }
        }
    }

    if (uiState is RegistrationUiState.ParseSuccess) {
        val weeks = (uiState as RegistrationUiState.ParseSuccess).weeks
        onParsed(weeks)
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("스케쥴 추가") }) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            RegistrationContent(
                uiState = uiState,
                onPickImage = { imagePicker.launch("image/*") },
                onRetry = viewModel::reset,
            )
        }
    }
}

@Composable
internal fun RegistrationContent(
    uiState: RegistrationUiState,
    onPickImage: () -> Unit,
    onRetry: () -> Unit,
) {
    when (uiState) {
        is RegistrationUiState.Idle -> IdleContent(onPickImage = onPickImage)
        is RegistrationUiState.Processing -> ProcessingContent()
        is RegistrationUiState.ParseSuccess -> Unit
        is RegistrationUiState.NotASchedule -> NotAScheduleContent(onRetry = onRetry)
        is RegistrationUiState.ParseError -> ParseErrorContent(onRetry = onRetry)
    }
}

@Composable
private fun IdleContent(onPickImage: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "스케쥴 이미지를 선택해주세요",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("이미지 선택")
        }
    }
}

@Composable
private fun ProcessingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("스케쥴을 인식하는 중...")
        }
    }
}

@Composable
private fun NotAScheduleContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "스케쥴 이미지를 인식하지 못했습니다",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "스케쥴표 이미지인지 확인 후 다시 시도해주세요",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("다시 시도")
        }
    }
}

@Composable
private fun ParseErrorContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "이미지 처리 중 오류가 발생했습니다",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("다시 시도")
        }
    }
}

private fun loadBitmapFromUri(
    context: android.content.Context,
    uri: android.net.Uri,
): Bitmap? = runCatching {
    val inputStream = context.contentResolver.openInputStream(uri)
    android.graphics.BitmapFactory.decodeStream(inputStream)
}.getOrNull()
