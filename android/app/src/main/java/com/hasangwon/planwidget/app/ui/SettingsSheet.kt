package com.hasangwon.planwidget.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.hasangwon.planwidget.R
import com.hasangwon.planwidget.app.appContainer
import com.hasangwon.planwidget.app.backup.BackupService
import com.hasangwon.planwidget.app.notification.DigestScheduler
import com.hasangwon.planwidget.app.ui.theme.AppFont
import com.hasangwon.planwidget.app.ui.theme.SettingsRefresh
import com.hasangwon.planwidget.app.ui.theme.appColors
import com.hasangwon.planwidget.app.ui.theme.styleSp
import com.hasangwon.planwidget.shared.BackgroundColor
import com.hasangwon.planwidget.shared.BackgroundSettings
import com.hasangwon.planwidget.shared.ThemeColor
import com.hasangwon.planwidget.shared.ThemeSettings
import com.hasangwon.planwidget.shared.TimeTextFormatter
import com.hasangwon.planwidget.shared.model.ColorTag
import com.hasangwon.planwidget.shared.model.Recurrence
import com.hasangwon.planwidget.shared.store.CalendarLayoutSettings
import com.hasangwon.planwidget.shared.store.CalendarLayoutStyle
import com.hasangwon.planwidget.shared.store.EventColorSettings
import com.hasangwon.planwidget.shared.store.EventTextWeight
import com.hasangwon.planwidget.shared.store.EventTextWeightSettings
import com.hasangwon.planwidget.shared.store.MonthSwipeDirection
import com.hasangwon.planwidget.shared.store.MonthSwipeSettings
import com.hasangwon.planwidget.shared.store.NotificationSettings
import com.hasangwon.planwidget.shared.store.TextSizeOption
import com.hasangwon.planwidget.shared.store.TextSizeSettings
import com.hasangwon.planwidget.shared.store.WidgetTextSizeSettings
import com.hasangwon.planwidget.widget.WidgetRefresh
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

/** 설정 — 테마/배경색, 글자 크기, 달력 모양·넘기기, 일정 유형별 기본 색, 알림, 백업 (iOS SettingsView 대응) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val container = remember { context.appContainer }
    val colors = appColors
    val scope = rememberCoroutineScope()
    val toast = remember { ToastState() }
    val isTablet = com.hasangwon.planwidget.app.ui.theme.LocalIsTablet.current

    var themeColor by remember { mutableStateOf(ThemeSettings.current) }
    var backgroundColor by remember { mutableStateOf(BackgroundSettings.current) }
    var singleColor by remember { mutableStateOf(EventColorSettings.color(Recurrence.NONE)) }
    var weeklyColor by remember { mutableStateOf(EventColorSettings.color(Recurrence.WEEKLY)) }
    var monthlyColor by remember { mutableStateOf(EventColorSettings.color(Recurrence.MONTHLY)) }
    var editingRecurrence by remember { mutableStateOf<Recurrence?>(null) }
    var textSize by remember { mutableStateOf(TextSizeSettings.current(isTablet)) }
    var widgetTextSize by remember { mutableStateOf(WidgetTextSizeSettings.current) }
    var eventWeight by remember { mutableStateOf(EventTextWeightSettings.current) }
    var swipeDirection by remember { mutableStateOf(MonthSwipeSettings.current) }
    var layoutStyle by remember { mutableStateOf(CalendarLayoutSettings.current) }
    var digestTime by remember { mutableStateOf(LocalTime.of(NotificationSettings.digestHour, NotificationSettings.digestMinute)) }
    var pickingDigestTime by remember { mutableStateOf(false) }
    /** 백업/가져오기 진행 중 문구 (null 이면 대기) — 진행 중엔 화면을 덮어 상태를 보여주고 중복 실행을 막는다 */
    var backupProgress by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<ByteArray?>(null) }

    /** 설정을 바꾼 뒤 — 화면 다시 그리기 + 위젯 갱신 */
    fun applied(refreshesWidget: Boolean = true) {
        SettingsRefresh.bump()
        if (refreshesWidget) scope.launch { WidgetRefresh.requestUpdate(context) }
    }

    /** 진행 표시를 먼저 띄운 뒤 작업을 실행한다 — 사용자가 진행 중임을 알 수 있게 */
    fun runBackupTask(message: String, work: suspend () -> Unit) {
        if (backupProgress != null) return
        backupProgress = message
        scope.launch {
            delay(80)
            try {
                work()
            } finally {
                backupProgress = null
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupService.EXPORT_MIME)) { uri: Uri? ->
        val data = pendingExport
        pendingExport = null
        if (uri == null || data == null) return@rememberLauncherForActivityResult
        runBackupTask("백업 파일 저장하는 중…") {
            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(data) } != null }.getOrDefault(false)
            }
            toast.show(if (ok) "백업 파일을 저장했어요" else "백업 파일을 만들지 못했어요")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runBackupTask("일정 가져오는 중…") {
            try {
                val imported = BackupService.importFile(context, uri, container.repository)
                toast.show(
                    if (imported.skipped > 0) "${imported.added}개 가져왔어요 (${imported.skipped}개 건너뜀)"
                    else "${imported.added}개 가져왔어요",
                )
            } catch (error: BackupService.BackupException) {
                toast.show(error.message ?: "가져오지 못했어요")
            } catch (error: Exception) {
                toast.show("가져오지 못했어요")
            }
        }
    }

    AppSheet(
        title = "설정",
        onDismiss = onDismiss,
        dismissible = backupProgress == null,
        trailing = { SheetTextButton("완료", onClick = onDismiss) },
    ) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // 테마색 · 배경색
                AppCard {
                    CardTitle("테마색")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (theme in ThemeColor.entries) {
                            ColorSwatchButton(Color(theme.argb(colors.isDark)), themeColor == theme, theme.label) {
                                themeColor = theme
                                ThemeSettings.setCurrent(theme)
                                applied()
                            }
                        }
                    }
                    CardCaption("앱 전체 포인트 색과 위젯에 적용됩니다.")
                    CardDivider()
                    CardTitle("배경색")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (background in BackgroundColor.entries) {
                            BackgroundSwatch(background, backgroundColor == background) {
                                backgroundColor = background
                                BackgroundSettings.setCurrent(background)
                                applied()
                            }
                        }
                    }
                    CardCaption("잉크·블랙골드를 선택하면 앱이 어두운 화면으로 표시됩니다.")
                }

                // 글자 크기
                AppCard(spacing = 16) {
                    CardTitle("글자 크기")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CardSubtitle("앱")
                        OptionSelector(TextSizeOption.entries, textSize, { it.label }) {
                            textSize = it
                            TextSizeSettings.setCurrent(it)
                            applied(refreshesWidget = false)
                        }
                    }
                    CardDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CardSubtitle("위젯")
                        OptionSelector(TextSizeOption.entries, widgetTextSize, { it.label }) {
                            widgetTextSize = it
                            WidgetTextSizeSettings.setCurrent(it)
                            applied()
                        }
                    }
                    CardCaption("앱과 위젯의 글자 크기를 따로 설정할 수 있어요.")
                    CardDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CardSubtitle("일정 글씨 두께")
                        OptionSelector(EventTextWeight.entries, eventWeight, { it.label }) {
                            eventWeight = it
                            EventTextWeightSettings.setCurrent(it)
                            applied()
                        }
                        CardCaption("일정 이름의 두께예요. 앱 달력과 위젯에 함께 적용돼요.")
                    }
                }

                // 달력
                AppCard(spacing = 16) {
                    CardTitle("달력")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CardSubtitle("모양")
                        OptionSelector(CalendarLayoutStyle.entries, layoutStyle, { it.label }) {
                            layoutStyle = it
                            CalendarLayoutSettings.setCurrent(it)
                            applied(refreshesWidget = false)
                        }
                        CardCaption("와이드는 달력을 화면 좌우 끝까지 넓게 표시해요.")
                    }
                    CardDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CardSubtitle("넘기기")
                        OptionSelector(MonthSwipeDirection.entries, swipeDirection, { it.label }) {
                            swipeDirection = it
                            MonthSwipeSettings.setCurrent(it)
                            applied(refreshesWidget = false)
                        }
                        CardCaption("왼쪽·위로 밀면 다음 달, 오른쪽·아래로 밀면 이전 달로 넘어가요.")
                    }
                }

                // 일정별 기본 색상
                AppCard {
                    CardTitle("일정별 기본 색상")
                    fun colorOf(recurrence: Recurrence) = when (recurrence) {
                        Recurrence.NONE -> singleColor
                        Recurrence.WEEKLY -> weeklyColor
                        Recurrence.MONTHLY -> monthlyColor
                    }
                    // 유형 3개 가로 배치 — 각자 현재 색 표시, 탭하면 아래 공유 팔레트로 편집
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (option in Recurrence.entries) {
                            val isEditing = editingRecurrence == option
                            val color = colorOf(option).composeColor
                            Row(
                                Modifier
                                    .weight(1f)
                                    .background(if (isEditing) color.copy(alpha = 0.14f) else colors.primaryFill(0.04f), RoundedCornerShape(12.dp))
                                    .border(1.5.dp, if (isEditing) color else Color.Transparent, RoundedCornerShape(12.dp))
                                    .plainClickable { editingRecurrence = if (isEditing) null else option }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(12.dp).background(color, CircleShape))
                                HorizontalSpace(6)
                                Text("${option.label} 일정", fontSize = styleSp(AppFont.CAPTION), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            }
                        }
                    }
                    AnimatedVisibility(editingRecurrence != null) {
                        val editing = editingRecurrence ?: Recurrence.NONE
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            CardDivider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                for (tag in ColorTag.entries) {
                                    ColorSwatchButton(tag.composeColor, colorOf(editing) == tag, tag.label) {
                                        when (editing) {
                                            Recurrence.NONE -> singleColor = tag
                                            Recurrence.WEEKLY -> weeklyColor = tag
                                            Recurrence.MONTHLY -> monthlyColor = tag
                                        }
                                        EventColorSettings.setColor(tag, editing)
                                        applied()
                                    }
                                }
                            }
                        }
                    }
                    CardCaption("각 일정의 기본 색으로 적용됩니다. 일정 작성 화면에서 개별 색으로 바꿀 수도 있습니다.")
                }

                // 알림
                AppCard {
                    CardTitle("알림")
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("하루 알림 시간", fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Spacer(Modifier.weight(1f))
                        Text(
                            TimeTextFormatter.format(digestTime),
                            Modifier
                                .background(colors.primaryFill(0.06f), RoundedCornerShape(8.dp))
                                .plainClickable { pickingDigestTime = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = styleSp(AppFont.BODY),
                            color = colors.textPrimary,
                        )
                    }
                    CardCaption("알림을 켠 일정이 있는 날, 이 시간에 하루 일정을 모아 한 번만 알려드려요.")
                }

                // 백업
                AppCard {
                    CardTitle("백업")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BackupButton("내보내기", painterResource(R.drawable.ic_upload), Modifier.weight(1f), enabled = backupProgress == null) {
                            runBackupTask("백업 파일 만드는 중…") {
                                runCatching { BackupService.exportData(container.repository) }
                                    .onSuccess { data ->
                                        pendingExport = data
                                        exportLauncher.launch(BackupService.defaultExportName)
                                    }
                                    .onFailure { toast.show("백업 파일을 만들지 못했어요") }
                            }
                        }
                        BackupButton("가져오기", painterResource(R.drawable.ic_download), Modifier.weight(1f), enabled = backupProgress == null) {
                            importLauncher.launch(arrayOf("*/*"))
                        }
                    }
                    CardCaption("내보내기 타입 1(.hscal), 타입 2(.redcalendar) 파일을 가져올 수 있어요. 이미 있는 일정은 건너뛰어요.")
                }
            }

            ToastOverlay(toast, bottomPadding = 24)
        }
    }

    if (pickingDigestTime) {
        AppTimePickerDialog(initial = digestTime, onDismiss = { pickingDigestTime = false }) { picked ->
            digestTime = picked
            pickingDigestTime = false
            NotificationSettings.setDigestTime(picked.hour, picked.minute)
            DigestScheduler.refresh(context)
        }
    }

    backupProgress?.let { message ->
        Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Column(
                Modifier.background(colors.surface, RoundedCornerShape(18.dp)).padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(color = colors.primary)
                Text(message, fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            }
        }
    }
}

@Composable
private fun BackgroundSwatch(background: BackgroundColor, isSelected: Boolean, onClick: () -> Unit) {
    val colors = appColors
    // 블랙골드는 잉크와 색이 거의 같아 골드 테두리로 구분
    val isBlackGold = background == BackgroundColor.BLACK_GOLD
    Box(
        Modifier
            .size(28.dp)
            .background(Color(background.swatch), CircleShape)
            .border(
                if (isBlackGold) 2.5.dp else 1.dp,
                if (isBlackGold) Color(ThemeColor.GOLD.argb(false)) else colors.primaryFill(0.12f),
                CircleShape,
            )
            .plainClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = background.label,
                tint = if (background.forcesDark) Color.White else Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun BackupButton(title: String, icon: Painter, modifier: Modifier = Modifier, enabled: Boolean, onClick: () -> Unit) {
    val colors = appColors
    Row(
        modifier
            .background(colors.primaryFill(0.06f), RoundedCornerShape(11.dp))
            .plainClickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
        HorizontalSpace(6)
        Text(title, fontSize = styleSp(AppFont.CAPTION), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
}
