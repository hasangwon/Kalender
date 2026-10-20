package com.hasangwon.planwidget.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hasangwon.planwidget.app.appContainer
import com.hasangwon.planwidget.app.notification.DigestScheduler
import com.hasangwon.planwidget.app.ui.theme.AppFont
import com.hasangwon.planwidget.app.ui.theme.appColors
import com.hasangwon.planwidget.app.ui.theme.styleSp
import com.hasangwon.planwidget.shared.KOREAN
import com.hasangwon.planwidget.shared.TimeTextFormatter
import com.hasangwon.planwidget.shared.model.ColorTag
import com.hasangwon.planwidget.shared.model.Recurrence
import com.hasangwon.planwidget.shared.model.Schedule
import com.hasangwon.planwidget.shared.store.EventColorSettings
import com.hasangwon.planwidget.shared.toEpochMillis
import com.hasangwon.planwidget.shared.toLocalDate
import com.hasangwon.planwidget.shared.toLocalDateTime
import com.hasangwon.planwidget.shared.weekdayWide
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", KOREAN)

/** 폼 입력 상태 (iOS ScheduleFormView 의 @State 들). 화면은 이 값을 그리기만 한다 */
private class ScheduleFormState(val editing: Schedule?, defaultDate: LocalDate, zone: ZoneId) {
    var title by mutableStateOf(editing?.title ?: "")
    var memo by mutableStateOf(editing?.memo ?: "")
    var date by mutableStateOf(editing?.startDate?.toLocalDate(zone) ?: defaultDate)
    var hasTime by mutableStateOf(editing?.hasTime ?: false)
    var time by mutableStateOf(
        if (editing != null && editing.hasTime) editing.startDate.toLocalDateTime(zone).toLocalTime() else LocalTime.of(9, 0),
    )
    var recurrence by mutableStateOf(editing?.recurrence ?: Recurrence.NONE)
    var monthlyOnLastDay by mutableStateOf(editing?.monthlyOnLastDay ?: false)
    var notifies by mutableStateOf(editing?.notifies ?: false)
    var hasEndDate by mutableStateOf(editing?.endDate != null)
    var endDate by mutableStateOf(editing?.endDate?.toLocalDate(zone) ?: (editing?.startDate?.toLocalDate(zone) ?: defaultDate))
    /** null 이면 유형 기본 색을 따름 */
    var pickedColor by mutableStateOf<ColorTag?>(if (editing != null && editing.hasCustomColor) editing.color else null)
    var isColorExpanded by mutableStateOf(false)

    val isEditing: Boolean get() = editing != null
    val trimmedTitle: String get() = title.trim()
    val typeDefaultColor: ColorTag get() = EventColorSettings.color(recurrence)
    val effectiveColor: ColorTag get() = pickedColor ?: typeDefaultColor

    val recurrenceFooter: String
        get() = when (recurrence) {
            Recurrence.NONE -> "선택한 날짜에 한 번만 표시됩니다."
            Recurrence.WEEKLY -> "선택한 날짜부터 ${date.weekdayWide}마다 반복됩니다."
            Recurrence.MONTHLY ->
                if (monthlyOnLastDay) "선택한 날짜가 속한 달부터 매달 말일(그 달의 마지막 날)에 반복됩니다."
                else "선택한 날짜부터 매달 ${date.dayOfMonth}일마다 반복됩니다."
        }

    fun onDateChanged(newDate: LocalDate) {
        date = newDate
        // 종료일은 시작일보다 앞설 수 없음
        if (endDate < newDate) endDate = newDate
    }

    fun onRecurrenceChanged(value: Recurrence) {
        recurrence = value
        if (value == Recurrence.NONE) hasEndDate = false
        if (value != Recurrence.MONTHLY) monthlyOnLastDay = false
    }

    /** 날짜 + (선택 시) 시간을 합쳐 저장할 startDate 계산 */
    fun resolvedStartDate(zone: ZoneId): Long =
        if (hasTime) date.atTime(time.hour, time.minute).atZone(zone).toInstant().toEpochMilli()
        else date.toEpochMillis(zone)

    fun build(zone: ZoneId): Schedule {
        val resolvedEnd = if (recurrence != Recurrence.NONE && hasEndDate) maxOf(endDate, date).toEpochMillis(zone) else null
        val resolvedMonthlyOnLastDay = recurrence == Recurrence.MONTHLY && monthlyOnLastDay
        val base = editing ?: Schedule()
        return base.copy(
            title = trimmedTitle,
            memo = memo.trim(),
            startDate = resolvedStartDate(zone),
            hasTime = hasTime,
            recurrenceRaw = recurrence.raw,
            colorRaw = effectiveColor.raw,
            endDate = resolvedEnd,
            hasCustomColor = pickedColor != null,
            notifies = notifies,
            monthlyOnLastDay = resolvedMonthlyOnLastDay,
        )
    }
}

/**
 * 일정 추가/수정 폼 (schedule 이 있으면 수정 모드). iOS ScheduleFormView 대응.
 * onDismiss(saved) — 저장으로 닫혔는지 알려 준다 (리뷰 요청 판단용)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFormSheet(
    defaultDate: LocalDate = LocalDate.now(),
    schedule: Schedule? = null,
    onDismiss: (saved: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { context.appContainer.repository }
    val colors = appColors
    val zone = remember { ZoneId.systemDefault() }
    val state = remember(schedule) { ScheduleFormState(schedule, defaultDate, zone) }
    val scope = rememberCoroutineScope()

    var pickingDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun save() {
        scope.launch {
            repository.saveSchedule(state.build(zone))
            onDismiss(true)
        }
    }

    fun delete() {
        val target = state.editing ?: return
        scope.launch {
            repository.deleteSchedule(target)
            onDismiss(false)
        }
    }

    AppSheet(
        title = if (state.isEditing) "일정 수정" else "새 일정",
        onDismiss = { onDismiss(false) },
        leading = { SheetTextButton("취소") { onDismiss(false) } },
        trailing = { SheetTextButton("저장", enabled = state.trimmedTitle.isNotEmpty()) { save() } },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 제목·메모
            FormSection {
                FormTextField(value = state.title, onValueChange = { state.title = it }, placeholder = "제목")
                FormRowDivider()
                FormTextField(value = state.memo, onValueChange = { state.memo = it }, placeholder = "메모 (선택)", maxLines = 4)
            }

            // 날짜·시간·알림
            FormSection(footer = if (state.notifies) "이 일정이 있는 날, 설정한 시간에 하루 일정을 모아 알려드려요." else null) {
                FormValueRow("날짜", state.date.format(fullDateFormatter)) { pickingDate = true }
                FormRowDivider()
                FormToggleRow("시간 설정", state.hasTime) { state.hasTime = it }
                AnimatedVisibility(state.hasTime) {
                    Column {
                        FormRowDivider()
                        FormValueRow("시간", TimeTextFormatter.format(state.time)) { pickingTime = true }
                    }
                }
                FormRowDivider()
                FormToggleRow("알림 받기", state.notifies) { isOn ->
                    state.notifies = isOn
                    if (isOn) {
                        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        DigestScheduler.requestAuthorizationIfNeeded(context)
                    }
                }
            }

            // 반복
            FormSection(header = "반복", footer = state.recurrenceFooter) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Recurrence.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = state.recurrence == option,
                            onClick = { state.onRecurrenceChanged(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, Recurrence.entries.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = colors.primary,
                                activeContentColor = Color.White,
                                activeBorderColor = colors.primary,
                                inactiveContainerColor = colors.surface,
                                inactiveContentColor = colors.textPrimary,
                                inactiveBorderColor = colors.textPrimary.copy(alpha = 0.15f),
                            ),
                            icon = {},
                        ) {
                            Text(option.label, fontSize = styleSp(AppFont.FOOTNOTE), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                // 매달 반복은 "말일 기준" 으로 반복할 수 있음 (30/31일 없는 달 대응)
                AnimatedVisibility(state.recurrence == Recurrence.MONTHLY) {
                    Column {
                        FormRowDivider()
                        FormToggleRow("매달 말일에 반복", state.monthlyOnLastDay) { state.monthlyOnLastDay = it }
                    }
                }
                // 매주/매달 반복에서만 종료일 지정 가능
                AnimatedVisibility(state.recurrence != Recurrence.NONE) {
                    Column {
                        FormRowDivider()
                        FormToggleRow("종료 날짜 지정", state.hasEndDate) { state.hasEndDate = it }
                        AnimatedVisibility(state.hasEndDate) {
                            Column {
                                FormRowDivider()
                                FormValueRow("종료 날짜", state.endDate.format(fullDateFormatter)) { pickingEndDate = true }
                            }
                        }
                    }
                }
            }

            // 표시 색
            FormSection(footer = if (state.pickedColor == null) "일정 유형의 기본 색을 따르고 있습니다." else "이 일정만 개별 색을 사용합니다.") {
                Row(
                    Modifier.fillMaxWidth().plainClickable { state.isColorExpanded = !state.isColorExpanded }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("표시 색", fontSize = styleSp(AppFont.BODY), color = colors.textPrimary)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(16.dp).background(state.effectiveColor.composeColor, CircleShape))
                    Icon(
                        Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.padding(start = 8.dp).size(16.dp).rotate(if (state.isColorExpanded) 180f else 0f),
                    )
                }
                AnimatedVisibility(state.isColorExpanded) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        for (tag in ColorTag.entries) {
                            ColorSwatchButton(tag.composeColor, state.effectiveColor == tag, tag.label) {
                                // 유형 기본 색을 고르면 "기본 따름" 으로 되돌림
                                state.pickedColor = if (tag == state.typeDefaultColor) null else tag
                            }
                        }
                    }
                }
            }

            if (state.isEditing) {
                FormSection {
                    Box(
                        Modifier.fillMaxWidth().plainClickable { confirmingDelete = true }.padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("일정 삭제", fontSize = styleSp(AppFont.BODY), color = Color(0xFFE53935))
                    }
                }
            }
        }
    }

    if (pickingDate) {
        AppDatePickerDialog(initial = state.date, onDismiss = { pickingDate = false }) {
            state.onDateChanged(it)
            pickingDate = false
        }
    }
    if (pickingEndDate) {
        AppDatePickerDialog(initial = state.endDate, minDate = state.date, onDismiss = { pickingEndDate = false }) {
            state.endDate = it
            pickingEndDate = false
        }
    }
    if (pickingTime) {
        AppTimePickerDialog(initial = state.time, onDismiss = { pickingTime = false }) {
            state.time = it
            pickingTime = false
        }
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = {
                Text(if (state.editing?.recurrence == Recurrence.NONE) "이 일정을 삭제할까요?" else "반복 일정 전체가 삭제됩니다. 삭제할까요?")
            },
            confirmButton = {
                TextButton(onClick = { confirmingDelete = false; delete() }) { Text("삭제", color = Color(0xFFE53935)) }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("취소") } },
            containerColor = colors.surface,
        )
    }
}

// MARK: - 폼 조각 (iOS Form 섹션 모양)

@Composable
internal fun FormSection(
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = appColors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        header?.let {
            Text(it, Modifier.padding(start = 14.dp), fontSize = styleSp(AppFont.FOOTNOTE), color = colors.textSecondary)
        }
        Column(Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(12.dp)), content = content)
        footer?.let {
            Text(it, Modifier.padding(start = 14.dp), fontSize = styleSp(AppFont.FOOTNOTE), color = colors.textSecondary)
        }
    }
}

@Composable
internal fun FormRowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 14.dp).heightIn(min = 0.5.dp).background(appColors.textPrimary.copy(alpha = 0.1f)))
}

@Composable
internal fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLines: Int = 1,
) {
    val colors = appColors
    val style = TextStyle(fontSize = styleSp(AppFont.BODY), color = colors.textPrimary)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
        textStyle = style,
        maxLines = maxLines,
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = colors.textTertiary))
                inner()
            }
        },
    )
}

@Composable
internal fun FormValueRow(label: String, value: String, onClick: () -> Unit) {
    val colors = appColors
    Row(
        Modifier.fillMaxWidth().plainClickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = styleSp(AppFont.BODY), color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            Modifier.background(colors.primaryFill(0.06f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = styleSp(AppFont.BODY),
            color = colors.textPrimary,
        )
    }
}

@Composable
internal fun FormToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = appColors
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = styleSp(AppFont.BODY), color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        AppSwitch(checked, onCheckedChange = onCheckedChange)
    }
}

// MARK: - 날짜·시간 피커 다이얼로그

/** Material DatePicker 는 UTC 0시 millis 로 날짜를 다룬다 */
private fun LocalDate.toUtcMillis(): Long = toEpochDay() * 86_400_000L
private fun Long.fromUtcMillis(): LocalDate = LocalDate.ofEpochDay(Math.floorDiv(this, 86_400_000L))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppDatePickerDialog(
    initial: LocalDate,
    minDate: LocalDate? = null,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val colors = appColors
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toUtcMillis(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                minDate == null || utcTimeMillis.fromUtcMillis() >= minDate
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(it.fromUtcMillis()) } ?: onDismiss() }) { Text("확인", color = colors.primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = colors.textSecondary) } },
        colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = colors.surface),
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onPick: (LocalTime) -> Unit,
) {
    val colors = appColors
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) { Text("확인", color = colors.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = colors.textSecondary) } },
        text = { TimePicker(state = state) },
        containerColor = colors.surface,
    )
}
