package com.hasangwon.planwidget.app.ui

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasangwon.planwidget.R
import com.hasangwon.planwidget.app.appContainer
import com.hasangwon.planwidget.app.ui.theme.AppFont
import com.hasangwon.planwidget.app.ui.theme.LocalIsTablet
import com.hasangwon.planwidget.app.ui.theme.LocalTextSize
import com.hasangwon.planwidget.app.ui.theme.SettingsRefresh
import com.hasangwon.planwidget.app.ui.theme.appColors
import com.hasangwon.planwidget.app.ui.theme.fixedSp
import com.hasangwon.planwidget.app.ui.theme.styleSp
import com.hasangwon.planwidget.app.ui.theme.textScale
import com.hasangwon.planwidget.shared.KOREAN
import com.hasangwon.planwidget.shared.Lunar
import com.hasangwon.planwidget.shared.WEEKDAY_SYMBOLS
import com.hasangwon.planwidget.shared.model.ColorTag
import com.hasangwon.planwidget.shared.model.Schedule
import com.hasangwon.planwidget.shared.startOfMonth
import com.hasangwon.planwidget.shared.store.CalendarLayoutSettings
import com.hasangwon.planwidget.shared.store.CalendarLayoutStyle
import com.hasangwon.planwidget.shared.store.DayEvent
import com.hasangwon.planwidget.shared.store.DayEventResolver
import com.hasangwon.planwidget.shared.store.EventTextWeightSettings
import com.hasangwon.planwidget.shared.store.LabSettings
import com.hasangwon.planwidget.shared.store.MonthSwipeDirection
import com.hasangwon.planwidget.shared.store.MonthSwipeSettings
import com.hasangwon.planwidget.shared.store.ScheduleStore
import com.hasangwon.planwidget.shared.store.TextSizeOption
import com.hasangwon.planwidget.shared.weekdaySundayFirst
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 달력 화면이 띄우는 시트 */
private sealed interface CalendarSheet {
    data class AddSchedule(val date: LocalDate) : CalendarSheet
    data class EditSchedule(val schedule: Schedule) : CalendarSheet
    data object Anniversaries : CalendarSheet
    data object Sync : CalendarSheet
    data object Settings : CalendarSheet
    data object Info : CalendarSheet
    data object Search : CalendarSheet
    data object MonthPicker : CalendarSheet
}

private enum class MenuItem(val title: String) {
    ANNIVERSARIES("매년 기념일 등록"),
    SYNC("동기화"),
    SETTINGS("설정"),
    INFO("정보"),
}

private val dayHeaderFormatter = DateTimeFormatter.ofPattern("M월 d일 EEEE", KOREAN)
private val monthTitleFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", KOREAN)

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val container = remember { context.appContainer }
    val viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.factory(container))

    val settingsVersion = SettingsRefresh.version
    val colors = appColors
    val isTablet = LocalIsTablet.current
    val textSize = LocalTextSize.current
    val scale = textScale
    val zone = remember { ZoneId.systemDefault() }

    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val anniversaries by viewModel.anniversaries.collectAsStateWithLifecycle()
    val deviceEvents by viewModel.deviceEvents.collectAsStateWithLifecycle()

    val swipeDirection = remember(settingsVersion) { MonthSwipeSettings.current }
    val layoutStyle = remember(settingsVersion) { CalendarLayoutSettings.current }
    val eventWeight = remember(settingsVersion) { FontWeight(EventTextWeightSettings.current.fontWeightValue) }
    val showsTodayLabel = remember(settingsVersion) { LabSettings.showsTodayLabel }
    val isWide = layoutStyle == CalendarLayoutStyle.WIDE

    val pagerState = remember { MonthPagerState(LocalDate.now().startOfMonth()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var sheet by remember { mutableStateOf<CalendarSheet?>(null) }
    val toast = remember { ToastState() }
    val scope = rememberCoroutineScope()
    val gapPx = with(LocalDensity.current) { 40.dp.toPx() }
    val pagingAxis = if (swipeDirection == MonthSwipeDirection.VERTICAL) PagerAxis.VERTICAL else PagerAxis.HORIZONTAL

    LaunchedEffect(Unit) {
        viewModel.refreshDeviceCalendar()
        viewModel.loadDeviceEvents(pagerState.displayedMonth)
    }

    /** 표시 월 변경의 단일 경로 — 이동 방향으로 미끄러지며 넘어간다 */
    fun setMonth(target: LocalDate, alsoSelect: LocalDate? = null) {
        if (pagerState.isBusy) return
        val normalized = target.startOfMonth()
        if (normalized == pagerState.displayedMonth && alsoSelect == null) return
        alsoSelect?.let { selectedDate = it }
        if (normalized != pagerState.displayedMonth) {
            scope.launch {
                pagerState.animateTo(normalized, pagingAxis, gapPx)
                viewModel.loadDeviceEvents(normalized)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            TopBar(
                isTablet = isTablet,
                onMenu = { item ->
                    sheet = when (item) {
                        MenuItem.ANNIVERSARIES -> CalendarSheet.Anniversaries
                        MenuItem.SYNC -> CalendarSheet.Sync
                        MenuItem.SETTINGS -> CalendarSheet.Settings
                        MenuItem.INFO -> CalendarSheet.Info
                    }
                },
                onToday = { setMonth(LocalDate.now(), alsoSelect = LocalDate.now()) },
                onSearch = { sheet = CalendarSheet.Search },
            )

            // 달력 카드 — 남는 높이를 전부 차지
            val cardRadius = if (isWide) 0.dp else 24.dp
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = if (isWide) 0.dp else 16.dp)
                    .padding(top = 4.dp)
                    .shadow(if (isWide || colors.isDark) 0.dp else 4.dp, RoundedCornerShape(cardRadius), spotColor = Color.Black.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(cardRadius))
                    .background(colors.surface)
                    .padding(vertical = 14.dp)
                    .padding(horizontal = if (isWide) 0.dp else 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MonthHeader(
                    displayedMonth = pagerState.displayedMonth,
                    isTablet = isTablet,
                    isVertical = swipeDirection == MonthSwipeDirection.VERTICAL,
                    onTitle = { sheet = CalendarSheet.MonthPicker },
                    onPrevious = { setMonth(pagerState.displayedMonth.minusMonths(1)) },
                    onNext = { setMonth(pagerState.displayedMonth.plusMonths(1)) },
                )

                val gridInset = if (isWide) 2.dp else 6.dp
                WeekdayHeader(Modifier.padding(horizontal = gridInset))

                // 3장의 날짜를 모아 한 번에 계산 (겹치는 앞뒤 달 날짜는 한 번만)
                val pagedMonths = (-1..1).map { pagerState.monthAt(it) }
                val weekRowsByMonth = remember(pagedMonths) { pagedMonths.associateWith { makeWeekRows(it) } }
                val eventsByDay = remember(weekRowsByMonth, schedules, anniversaries, deviceEvents, settingsVersion) {
                    DayEventResolver.eventsByDay(
                        schedules = schedules,
                        anniversaries = anniversaries,
                        deviceEvents = deviceEvents,
                        days = weekRowsByMonth.values.flatten().flatten(),
                        zone = zone,
                    )
                }

                MonthSwipePager(
                    state = pagerState,
                    swipeDirection = swipeDirection,
                    gapPx = gapPx,
                    onPagingFinished = { viewModel.loadDeviceEvents(it) },
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = gridInset),
                ) { month ->
                    MonthPage(
                        month = month,
                        weeks = weekRowsByMonth[month] ?: makeWeekRows(month),
                        eventsByDay = eventsByDay,
                        selectedDate = selectedDate,
                        isWide = isWide,
                        eventWeight = eventWeight,
                        showsTodayLabel = showsTodayLabel,
                        onSelect = { day, isCurrentMonth ->
                            // 인접 달 날짜를 누르면 슬라이드로 그 달에 이동
                            if (isCurrentMonth) selectedDate = day else setMonth(day, alsoSelect = day)
                        },
                    )
                }
            }

            DaySection(
                selectedDate = selectedDate,
                schedules = schedules,
                eventsForDay = { day ->
                    val daySchedules = ScheduleStore.occurrences(schedules, day, zone)
                    daySchedules to DayEventResolver.events(daySchedules, anniversaries, deviceEvents, day, zone)
                },
                onAdd = { sheet = CalendarSheet.AddSchedule(selectedDate) },
                onEdit = { sheet = CalendarSheet.EditSchedule(it) },
                onDelete = { viewModel.deleteSchedule(it) },
                onDeviceEventTap = { toast.show("이 일정은 안드로이드 달력 앱에서 수정할 수 있어요") },
                modifier = Modifier.height((220 * if (isTablet) 1.2f else 1f).dp),
            )
        }

        ToastOverlay(toast)
    }

    // MARK: - 시트
    val activity = context as? Activity
    when (val current = sheet) {
        null -> Unit
        is CalendarSheet.AddSchedule -> ScheduleFormSheet(
            defaultDate = current.date,
            onDismiss = { saved ->
                sheet = null
                if (saved && activity != null) viewModel.requestReviewIfEarned(activity)
            },
        )
        is CalendarSheet.EditSchedule -> ScheduleFormSheet(schedule = current.schedule, onDismiss = { sheet = null })
        CalendarSheet.Anniversaries -> AnniversaryListSheet(onDismiss = { sheet = null })
        CalendarSheet.Sync -> SyncSheet(onDismiss = { sheet = null; viewModel.refreshDeviceCalendar(); viewModel.loadDeviceEvents(pagerState.displayedMonth) })
        CalendarSheet.Settings -> SettingsSheet(onDismiss = { sheet = null })
        CalendarSheet.Info -> InfoSheet(onDismiss = { sheet = null })
        CalendarSheet.Search -> SearchSheet(
            onDismiss = { sheet = null },
            onSelect = { target ->
                sheet = null
                setMonth(target, alsoSelect = target)
            },
        )
        CalendarSheet.MonthPicker -> MonthYearPickerSheet(
            displayedMonth = pagerState.displayedMonth,
            onDismiss = { sheet = null },
            onSelect = { picked ->
                sheet = null
                setMonth(picked)
            },
        )
    }
}

// MARK: - 커스텀 상단바

@Composable
private fun TopBar(
    isTablet: Boolean,
    onMenu: (MenuItem) -> Unit,
    onToday: () -> Unit,
    onSearch: () -> Unit,
) {
    val colors = appColors
    var isMenuOpen by remember { mutableStateOf(false) }
    val iconSize = if (isTablet) 27.dp else 18.dp
    val titleSize = if (isTablet) 30.sp else 20.sp
    val barHeight = if (isTablet) 76.dp else 50.dp

    Box(Modifier.fillMaxWidth().height(barHeight).padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            // 메뉴 버튼 — SF Symbol 대신 직접 그린 세 막대 (iOS 와 동일). 메뉴는 버튼 아래에 뜬다
            Box {
                Box(
                    Modifier
                        .size(44.dp)
                        .plainClickable { isMenuOpen = true }
                        .semantics { contentDescription = "메뉴" },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(if (isTablet) 5.dp else 4.dp)) {
                        repeat(3) {
                            Box(
                                Modifier
                                    .width(if (isTablet) 28.dp else 20.dp)
                                    .height(if (isTablet) 3.dp else 2.dp)
                                    .background(colors.primary, CircleShape),
                            )
                        }
                    }
                }
                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false },
                    containerColor = colors.surface,
                ) {
                    for (item in MenuItem.entries) {
                        if (item != MenuItem.entries.first()) CardDivider()
                        DropdownMenuItem(
                            text = {
                                Text(item.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            },
                            leadingIcon = {
                                Icon(menuIcon(item), contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                            },
                            onClick = {
                                isMenuOpen = false
                                onMenu(item)
                            },
                            modifier = Modifier.width(230.dp).height(50.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                "오늘",
                Modifier
                    .background(colors.primaryFill(0.06f), CircleShape)
                    .plainClickable(onClick = onToday)
                    .padding(horizontal = if (isTablet) 14.dp else 10.dp, vertical = if (isTablet) 7.dp else 5.dp),
                fontSize = if (isTablet) 19.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
            )

            Box(
                Modifier
                    .size(44.dp)
                    .plainClickable(onClick = onSearch)
                    .semantics { contentDescription = "검색" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.primary, modifier = Modifier.size(iconSize + 4.dp))
            }
        }

        // 오른쭉 버튼이 더 많아도 제목은 화면 가운데에 고정
        Text(
            "달력",
            Modifier.align(Alignment.Center),
            fontSize = titleSize,
            fontWeight = FontWeight.Black,
            color = colors.primary,
        )
    }
}

@Composable
private fun menuIcon(item: MenuItem): Painter = when (item) {
    MenuItem.ANNIVERSARIES -> painterResource(R.drawable.ic_gift)
    MenuItem.SYNC -> painterResource(R.drawable.ic_sync)
    MenuItem.SETTINGS -> rememberVectorPainter(Icons.Outlined.Settings)
    MenuItem.INFO -> rememberVectorPainter(Icons.Outlined.Info)
}

// MARK: - 월 헤더 / 요일 헤더

@Composable
private fun MonthHeader(
    displayedMonth: LocalDate,
    isTablet: Boolean,
    isVertical: Boolean,
    onTitle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = appColors
    val textSize = LocalTextSize.current
    val scale = textScale
    // 월 제목 배율 — 글자 크기 단계에 거의 따라 커지지 않게 중간 단계 대비 변화량의 20%만 반영
    val base = TextSizeOption.MEDIUM.scale(isTablet)
    val monthTitleScale = base + (scale - base) * 0.2f
    val verticalPadding = if (isTablet) 22.dp else 0.dp

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = verticalPadding, bottom = verticalPadding + 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.plainClickable(onClick = onTitle), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                displayedMonth.format(monthTitleFormatter),
                fontSize = (22 * monthTitleScale).sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
            )
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "연·월 이동", tint = colors.textSecondary, modifier = Modifier.size((16 * monthTitleScale).dp))
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MonthNavButton(if (isVertical) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowLeft, "이전 달", onPrevious)
            MonthNavButton(if (isVertical) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight, "다음 달", onNext)
        }
    }
}

@Composable
private fun MonthNavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = appColors
    val scale = textScale
    val side = (30 * minOf(scale, 1.4f)).dp
    Box(
        Modifier
            .size(side)
            .background(colors.primaryFill(0.05f), CircleShape)
            .plainClickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size((18 * scale).dp))
    }
}

@Composable
private fun WeekdayHeader(modifier: Modifier = Modifier) {
    val colors = appColors
    Row(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
        WEEKDAY_SYMBOLS.forEachIndexed { index, symbol ->
            Text(
                symbol,
                Modifier.weight(1f),
                fontSize = fixedSp(12f),
                fontWeight = FontWeight.Bold,
                color = if (index == 0 || index == 6) colors.holidayRed else colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// MARK: - 달력 그리드

/** 월 그리드 날짜 — 앞쪽은 전달 말일, 뒤쪽은 다음 달 초반으로 7칸을 채운다 */
internal fun makeWeekRows(month: LocalDate): List<List<LocalDate>> {
    val first = month.startOfMonth()
    val leading = first.weekdaySundayFirst - 1
    val slots = mutableListOf<LocalDate>()
    for (offset in leading downTo 1) slots += first.minusDays(offset.toLong())
    for (day in 0 until first.lengthOfMonth()) slots += first.plusDays(day.toLong())
    while (slots.size % 7 != 0) slots += slots.last().plusDays(1)
    return slots.chunked(7)
}

@Composable
private fun MonthPage(
    month: LocalDate,
    weeks: List<List<LocalDate>>,
    eventsByDay: Map<LocalDate, List<DayEvent>>,
    selectedDate: LocalDate,
    isWide: Boolean,
    eventWeight: FontWeight,
    showsTodayLabel: Boolean,
    onSelect: (LocalDate, Boolean) -> Unit,
) {
    val today = LocalDate.now()
    // 남은 높이를 주 수로 나눠 각 주에 정확한 높이 부여 — 어떤 글자 크기/기기에서도 주 높이가 균일하다
    Column(Modifier.fillMaxSize()) {
        for (week in weeks) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (day in week) {
                    DayCell(
                        day = day,
                        events = eventsByDay[day] ?: emptyList(),
                        isCurrentMonth = day.year == month.year && day.month == month.month,
                        isSelected = day == selectedDate,
                        isToday = day == today,
                        isWide = isWide,
                        eventWeight = eventWeight,
                        showsTodayLabel = showsTodayLabel,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    events: List<DayEvent>,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    isWide: Boolean,
    eventWeight: FontWeight,
    showsTodayLabel: Boolean,
    onSelect: (LocalDate, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors
    val scale = textScale
    val weekday = day.weekdaySundayFirst
    // 공휴일 판정은 이미 이벤트 목록에 들어 있다
    val isHoliday = events.any { it.kind == DayEvent.Kind.HOLIDAY }
    val numberColor = when {
        isSelected -> Color.White
        isHoliday || weekday == 1 || weekday == 7 -> colors.holidayRed
        isToday -> colors.primary
        else -> colors.textPrimary
    }

    Column(
        modifier
            .alpha(if (isCurrentMonth) 1f else 0.32f)
            .plainClickable { onSelect(day, isCurrentMonth) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // 실험실 "오늘 표시": 숫자 오른쪽에 작게 "오늘". 왼쪽에 같은 폭의 투명 라벨을 둬 숫자는 칸 가운데에 남는다
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (isToday && showsTodayLabel) TodayLabel(Modifier.alpha(0f))
            Box(
                Modifier
                    .size((24 * scale).dp)
                    .background(
                        when {
                            isSelected -> colors.primary
                            isToday -> colors.primary.copy(alpha = 0.12f)
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${day.dayOfMonth}",
                    fontSize = fixedSp(13f),
                    fontWeight = if (isToday || isSelected) FontWeight.Black else FontWeight.SemiBold,
                    color = numberColor,
                    maxLines = 1,
                )
            }
            if (isToday && showsTodayLabel) TodayLabel()
        }

        // 위젯처럼 날짜 아래 일정 라벨 (최대 2개, 길면 … 없이 글자 단위로 끊음). 고정 높이 영역에 담아 주 높이에 영향을 주지 않는다
        Column(
            Modifier.fillMaxWidth().heightIn(max = (36 * scale).dp).clipToBounds(),
            verticalArrangement = Arrangement.spacedBy(1.5.dp),
        ) {
            for (event in events.take(2)) {
                DayEventChip(event, isWide, eventWeight)
            }
            if (events.size > 2) {
                Text(
                    "+${events.size - 2}",
                    Modifier.fillMaxWidth(),
                    fontSize = fixedSp(7.5f),
                    lineHeight = fixedSp(9f),
                    fontWeight = FontWeight.Black,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TodayLabel(modifier: Modifier = Modifier) {
    Text("오늘", modifier, fontSize = fixedSp(8f), fontWeight = FontWeight.SemiBold, color = appColors.textSecondary, maxLines = 1)
}

@Composable
private fun DayEventChip(event: DayEvent, isWide: Boolean, eventWeight: FontWeight) {
    val colors = appColors
    val tint = eventTint(event, colors.holidayRed, colors.textSecondary)
    CharClippedText(
        text = event.title,
        // 와이드는 칸이 넓은 만큼 1pt 크게
        fontSize = fixedSp(if (isWide) 9f else 8f),
        fontWeight = eventWeight,
        color = tint.darkened(0.12f),
        modifier = Modifier
            .fillMaxWidth()
            // 여백 없이 글자 뒤에만 칠한다 (형광펜처럼) — 칸 폭을 전부 글자에 쓴다
            .background(tint.copy(alpha = 0.1f), RoundedCornerShape(2.dp)),
    )
}

private fun eventTint(event: DayEvent, holidayRed: Color, fallback: Color): Color = when {
    event.kind == DayEvent.Kind.HOLIDAY -> holidayRed
    event.colorTag != null -> Color(event.colorTag.argb)
    else -> fallback
}

/** `.brightness(-amount)` 와 같은 결과를 필터 없이 색으로 만든다 */
internal fun Color.darkened(amount: Float): Color =
    Color((red - amount).coerceAtLeast(0f), (green - amount).coerceAtLeast(0f), (blue - amount).coerceAtLeast(0f), alpha)

val ColorTag.composeColor: Color get() = Color(argb)

// MARK: - 선택한 날짜 일정

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DaySection(
    selectedDate: LocalDate,
    schedules: List<Schedule>,
    eventsForDay: (LocalDate) -> Pair<List<Schedule>, List<DayEvent>>,
    onAdd: () -> Unit,
    onEdit: (Schedule) -> Unit,
    onDelete: (Schedule) -> Unit,
    onDeviceEventTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors
    val (daySchedules, dayEvents) = remember(selectedDate, schedules, eventsForDay) { eventsForDay(selectedDate) }
    // 공휴일/기념일/안드로이드 달력 = 수정 불가 카드로 표시
    val staticEvents = dayEvents.filter { it.kind != DayEvent.Kind.SCHEDULE }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(selectedDate.format(dayHeaderFormatter), fontSize = fixedSp(17f), fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(Lunar.text(selectedDate), fontSize = fixedSp(11f), color = colors.textTertiary, modifier = Modifier.padding(bottom = 1.dp))
            Spacer(Modifier.weight(1f))
            Text("${dayEvents.size}개", fontSize = fixedSp(13f), fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        }

        if (dayEvents.isEmpty()) {
            EmptyDayButton(onAdd)
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(staticEvents, key = { it.id }) { event ->
                    StaticEventCard(event, onTap = { if (event.kind == DayEvent.Kind.DEVICE_CALENDAR) onDeviceEventTap() })
                }
                items(daySchedules, key = { it.id }) { schedule ->
                    ScheduleCard(schedule, onTap = { onEdit(schedule) }, onDelete = { onDelete(schedule) })
                }
                item {
                    AddScheduleButton(onAdd)
                }
            }
        }
    }
}

@Composable
private fun EmptyDayButton(onAdd: () -> Unit) {
    val colors = appColors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .cardSurface()
            .plainClickable(onClick = onAdd)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(34.dp).background(colors.primary.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("일정이 없어요", fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text("탭해서 일정을 추가해 보세요", fontSize = styleSp(AppFont.CAPTION), color = colors.textSecondary)
        }
    }
}

/** 일정 카드 배경 (iOS cardBackground) */
@Composable
private fun Modifier.cardSurface(): Modifier {
    val colors = appColors
    return this
        .shadow(if (colors.isDark) 0.dp else 2.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.3f))
        .background(colors.surface, RoundedCornerShape(16.dp))
}

@Composable
private fun StaticEventCard(event: DayEvent, onTap: () -> Unit) {
    val colors = appColors
    val tint = when (event.kind) {
        DayEvent.Kind.HOLIDAY -> colors.primary
        DayEvent.Kind.DEVICE_CALENDAR -> Color.Gray
        else -> event.colorTag?.composeColor ?: colors.textSecondary
    }
    val badge = when (event.kind) {
        DayEvent.Kind.HOLIDAY -> "공휴일"
        DayEvent.Kind.DEVICE_CALENDAR -> "안드로이드 달력"
        else -> "기념일"
    }

    Row(
        Modifier.fillMaxWidth().cardSurface().plainClickable(onClick = onTap).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(4.dp).height(34.dp).background(tint, RoundedCornerShape(2.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(event.title, fontSize = fixedSp(15f), fontWeight = FontWeight.Bold, color = colors.textPrimary)
            event.timeText?.let { Text(it, fontSize = fixedSp(12f), color = colors.textSecondary) }
        }
        Badge(badge, tint)
    }
}

@Composable
private fun Badge(text: String, tint: Color) {
    Text(
        text,
        Modifier.background(tint.copy(alpha = 0.13f), CircleShape).padding(horizontal = 8.dp, vertical = 3.dp),
        fontSize = styleSp(AppFont.CAPTION2),
        fontWeight = FontWeight.Bold,
        color = tint,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleCard(schedule: Schedule, onTap: () -> Unit, onDelete: () -> Unit) {
    val colors = appColors
    val typeColor = schedule.displayColor.composeColor
    var isMenuOpen by remember { mutableStateOf(false) }

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .cardSurface()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap,
                    onLongClick = { isMenuOpen = true },
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.width(4.dp).height(34.dp).background(typeColor, RoundedCornerShape(2.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(schedule.title, fontSize = fixedSp(15f), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                if (schedule.memo.isNotEmpty()) {
                    Text(schedule.memo, fontSize = fixedSp(12f), color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                schedule.recurrenceBadgeText?.let { Badge(it, typeColor) }
                Text(schedule.timeText() ?: "종일", fontSize = fixedSp(12f), color = colors.textSecondary)
            }
        }

        // 길게 누르면 삭제 메뉴 (iOS contextMenu 대응)
        DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }, containerColor = colors.surface) {
            DropdownMenuItem(
                text = { Text("삭제", color = Color(0xFFE53935)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFE53935)) },
                onClick = {
                    isMenuOpen = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun AddScheduleButton(onAdd: () -> Unit) {
    val colors = appColors
    val stroke = colors.primary.copy(alpha = 0.35f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .drawBehind {
                val dash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
                drawRoundRect(
                    color = stroke,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = dash),
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .plainClickable(onClick = onAdd)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("일정 추가", fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.Bold, color = colors.primary)
    }
}
