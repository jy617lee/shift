@file:Suppress("MaxLineLength")

package com.schedule.shift.ui.confirmation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.ui.COLOR_SATURDAY
import com.schedule.shift.ui.COLOR_SUNDAY
import com.schedule.shift.ui.TODAY_BAR_ALPHA
import com.schedule.shift.ui.TODAY_BAR_HEIGHT
import com.schedule.shift.ui.TODAY_BAR_WIDTH
import com.schedule.shift.ui.WEEK_HEADER_ALPHA
import com.schedule.shift.ui.WEEK_LAST_DAY_OFFSET
import com.schedule.shift.ui.home.ShiftBadgeType
import com.schedule.shift.ui.home.ShiftTypeBadge
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val IMAGE_PREVIEW_HEIGHT = 260
private const val IMAGE_MAX_SCALE = 5f
private const val TIME_LENGTH = 5

private val DAY_LABEL_FMT = DateTimeFormatter.ofPattern("M/d(E)")
private val MONTH_DAY_FMT = DateTimeFormatter.ofPattern("M/d")
private val WEEK_RANGE_FMT = DateTimeFormatter.ofPattern("M월 d일")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationScreen(
    viewModel: ConfirmationViewModel,
    onSaved: () -> Unit,
    onCancelled: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        when (uiState) {
            is ConfirmationUiState.Saved -> onSaved()
            is ConfirmationUiState.Cancelled -> onCancelled()
            is ConfirmationUiState.Reviewing -> Unit
        }
    }

    val reviewing = uiState as? ConfirmationUiState.Reviewing ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ConfirmationScaffold(viewModel = viewModel, reviewing = reviewing)

    if (reviewing.conflictCount > 0) {
        ReplaceConfirmDialog(
            conflictCount = reviewing.conflictCount,
            onConfirm = viewModel::proceedWithReplace,
            onDismiss = viewModel::dismissConflict,
        )
    }

    if (reviewing.editing != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissEdit,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            DayEditSheet(
                editing = reviewing.editing,
                onDraftChange = viewModel::updateDraft,
                onCommit = viewModel::commitEdit,
                onDismiss = viewModel::dismissEdit,
            )
        }
    }
}

@Composable
private fun ConfirmationScaffold(viewModel: ConfirmationViewModel, reviewing: ConfirmationUiState.Reviewing) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ConfirmationTopBar(onBack = viewModel::cancel) },
        bottomBar = { ConfirmationBottomBar(onCancel = viewModel::cancel, onConfirm = viewModel::confirm) },
    ) { padding ->
        ConfirmationBody(
            reviewing = reviewing,
            padding = padding,
            onEditDay = { weekIndex, dayIndex -> viewModel.startEdit(weekIndex, dayIndex) },
        )
    }
}

@Composable
private fun ReplaceConfirmDialog(conflictCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val weekLabel = if (conflictCount == 1) "1개 주" else "${conflictCount}개 주"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("스케쥴 교체") },
        text = { Text("${weekLabel} 스케쥴이 이미 등록되어 있습니다.\n기존 스케쥴을 새 스케쥴로 교체하시겠습니까?") },
        confirmButton = {
            Button(onClick = onConfirm) { Text("교체하기") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@Composable
private fun ConfirmationTopBar(onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "취소", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(text = "OCR 결과 확인", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ConfirmationBody(
    reviewing: ConfirmationUiState.Reviewing,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onEditDay: (Int, Int) -> Unit,
) {
    val today = LocalDate.now()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState()),
    ) {
        if (reviewing.imageUri != null) ImagePreview(uri = reviewing.imageUri)
        Spacer(modifier = Modifier.height(12.dp))
        reviewing.weeks.forEachIndexed { weekIndex, week ->
            WeekReviewCard(week = week, today = today, onEditDay = { dayIndex -> onEditDay(weekIndex, dayIndex) })
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ImagePreview(uri: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, IMAGE_MAX_SCALE)
        offset += panChange * scale
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IMAGE_PREVIEW_HEIGHT.dp)
            .clip(RoundedCornerShape(0.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .transformable(state = transformableState),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = Uri.parse(uri),
            contentDescription = "스케쥴 이미지",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun WeekReviewCard(week: ScheduleWeek, today: LocalDate, onEditDay: (Int) -> Unit) {
    val start = week.weekStartDate
    val end = start.plusDays(WEEK_LAST_DAY_OFFSET)
    val header = if (start.month == end.month) {
        "${start.year}년 ${start.format(WEEK_RANGE_FMT)}–${end.dayOfMonth}일"
    } else {
        "${start.year}년 ${start.format(WEEK_RANGE_FMT)} – ${end.format(WEEK_RANGE_FMT)}"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        WeekReviewHeader(header = header)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        week.days.forEachIndexed { dayIndex, day ->
            ReviewDayRow(day = day, isToday = day.date == today, onClick = { onEditDay(dayIndex) })
            if (dayIndex < week.days.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun WeekReviewHeader(header: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = WEEK_HEADER_ALPHA))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = header, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Suppress("LongMethod")
@Composable
private fun ReviewDayRow(day: ScheduleDay, isToday: Boolean, onClick: () -> Unit) {
    val dayOfWeek = day.date.dayOfWeek
    val dayColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        dayOfWeek == DayOfWeek.SUNDAY -> COLOR_SUNDAY
        dayOfWeek == DayOfWeek.SATURDAY -> COLOR_SATURDAY
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = TODAY_BAR_ALPHA)) else Modifier)
            .clickable { onClick() },
    ) {
        if (isToday) {
            Box(
                modifier = Modifier
                    .width(TODAY_BAR_WIDTH.dp)
                    .height(TODAY_BAR_HEIGHT.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        } else {
            Spacer(modifier = Modifier.width(TODAY_BAR_WIDTH.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.date.format(DAY_LABEL_FMT).substringAfter("(").removeSuffix(")"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = dayColor,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = day.date.format(MONTH_DAY_FMT),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                )
            }
            ReviewDayShiftContent(day = day, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Edit, contentDescription = "수정", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReviewDayShiftContent(day: ScheduleDay, modifier: Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (day.type) {
            DayType.WORK -> {
                ShiftTypeBadge(label = "근무", type = ShiftBadgeType.WORK)
                Text(text = "${day.startTime}–${day.endTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
            DayType.OFF -> ShiftTypeBadge(label = day.codeLabel.ifEmpty { "휴무" }, type = ShiftBadgeType.OFF)
            DayType.OTHER -> ShiftTypeBadge(label = day.codeLabel.ifEmpty { "기타" }, type = ShiftBadgeType.OFF)
            DayType.UNREGISTERED -> Text(text = "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConfirmationBottomBar(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 12.dp)) {
        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp), color = MaterialTheme.colorScheme.outline)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("취소", color = MaterialTheme.colorScheme.onSurface)
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("저장하기", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
private fun DayEditSheet(
    editing: EditingState,
    onDraftChange: (ScheduleDay) -> Unit,
    onCommit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val draft = editing.draft
    var startTimeText by remember(editing.weekIndex, editing.dayIndex) { mutableStateOf(draft.startTime?.toString() ?: "") }
    var endTimeText by remember(editing.weekIndex, editing.dayIndex) { mutableStateOf(draft.endTime?.toString() ?: "") }
    var codeLabelText by remember(editing.weekIndex, editing.dayIndex) { mutableStateOf(draft.codeLabel) }
    var startTimeError by remember { mutableStateOf(false) }
    var endTimeError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
        Text(text = "${draft.date.format(DAY_LABEL_FMT)} 수정", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(20.dp))
        DayTypeSelector(draft = draft, onDraftChange = onDraftChange)
        if (draft.type == DayType.WORK) {
            Spacer(modifier = Modifier.height(16.dp))
            TimeInputRow(
                startTimeText = startTimeText,
                endTimeText = endTimeText,
                startTimeError = startTimeError,
                endTimeError = endTimeError,
                onStartChange = { startTimeText = it; startTimeError = false },
                onEndChange = { endTimeText = it; endTimeError = false },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        CodeLabelField(value = codeLabelText, onValueChange = { codeLabelText = it })
        Spacer(modifier = Modifier.height(24.dp))
        EditSheetActions(
            onDismiss = onDismiss,
            onSave = {
                if (draft.type == DayType.WORK) {
                    val start = parseTime(startTimeText)
                    val end = parseTime(endTimeText)
                    startTimeError = start == null
                    endTimeError = end == null
                    if (start == null || end == null) return@EditSheetActions
                    onDraftChange(draft.copy(startTime = start, endTime = end, codeLabel = codeLabelText))
                } else {
                    onDraftChange(draft.copy(codeLabel = codeLabelText))
                }
                onCommit()
            },
        )
    }
}

@Suppress("CognitiveComplexMethod")
@Composable
private fun DayTypeSelector(draft: ScheduleDay, onDraftChange: (ScheduleDay) -> Unit) {
    Text(text = "유형", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(DayType.WORK to "근무", DayType.OFF to "휴무", DayType.OTHER to "기타").forEach { (type, label) ->
            val selected = draft.type == type
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (selected) 0.dp else 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable {
                        onDraftChange(when (type) {
                            DayType.WORK -> draft.copy(type = DayType.WORK)
                            DayType.OFF -> draft.copy(type = DayType.OFF, startTime = null, endTime = null)
                            else -> draft.copy(type = DayType.OTHER, startTime = null, endTime = null)
                        })
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TimeInputRow(
    startTimeText: String,
    endTimeText: String,
    startTimeError: Boolean,
    endTimeError: Boolean,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "출근시간", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = startTimeText,
                onValueChange = onStartChange,
                placeholder = { Text("HH:mm", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = startTimeError,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "퇴근시간", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = endTimeText,
                onValueChange = onEndChange,
                placeholder = { Text("HH:mm", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = endTimeError,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CodeLabelField(value: String, onValueChange: (String) -> Unit) {
    Text(text = "근태코드", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(value = value, onValueChange = onValueChange, singleLine = true, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun EditSheetActions(onDismiss: () -> Unit, onSave: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("취소") }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("저장", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun parseTime(text: String): LocalTime? =
    runCatching {
        val normalized = text.trim().takeIf { it.length == TIME_LENGTH } ?: return@runCatching null
        LocalTime.parse(normalized)
    }.getOrNull()
