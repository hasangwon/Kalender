package com.hasangwon.planwidget.widget

import android.content.Context
import com.hasangwon.planwidget.app.appContainer
import com.hasangwon.planwidget.shared.KOREAN
import com.hasangwon.planwidget.shared.isWeekend
import com.hasangwon.planwidget.shared.startOfMonth
import com.hasangwon.planwidget.shared.store.DayEvent
import com.hasangwon.planwidget.shared.store.DayEventResolver
import com.hasangwon.planwidget.shared.toEpochMillis
import com.hasangwon.planwidget.shared.weekdaySundayFirst
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 위젯 크기 3단계 (iOS systemSmall / systemMedium / systemLarge 대응) */
enum class WidgetFamily { SMALL, MEDIUM, LARGE }

/** 위젯에 그릴 이벤트 하나 (값 타입 — 엔티티를 위젯에 직접 담지 않는다) */
data class WidgetEvent(
    val title: String,
    val colorArgb: Long?,
    /** 공휴일은 색 태그가 없어 따로 빨간색으로 칠한다 (앱과 동일) */
    val isHoliday: Boolean,
    val timeText: String?,
)

/** Large 달력 위젯의 셀 하나 */
data class MonthCell(
    val day: Int,
    val isToday: Boolean,
    val isHoliday: Boolean,
    /** 칸에 표시할 일정 (최대 2개, 앱 달력과 같은 순서) */
    val chips: List<WidgetEvent>,
    /** 표시하지 못한 나머지 일정 수 (+N) */
    val extraCount: Int,
)

data class WidgetDayGroup(val date: LocalDate, val items: List<WidgetEvent>)

data class ScheduleEntry(
    val date: LocalDate,
    val today: List<WidgetEvent>,
    val upcoming: List<WidgetDayGroup>,
    val monthTitle: String,
    /** 요일 정렬용 선행 빈 칸 수 */
    val leadingBlanks: Int,
    val monthCells: List<MonthCell>,
)

/** 위젯 데이터 계산 (iOS ScheduleProvider 대응). 크기별로 필요한 날짜만 계산한다 */
object ScheduleProvider {
    private val monthTitleFormatter = DateTimeFormatter.ofPattern("M월", KOREAN)

    suspend fun loadEntry(context: Context, family: WidgetFamily, now: LocalDate = LocalDate.now()): ScheduleEntry {
        val zone = ZoneId.systemDefault()

        // 내일부터 7일 (Medium 에서만 표시)
        val upcomingDays = if (family == WidgetFamily.MEDIUM) (1..7).map { now.plusDays(it.toLong()) } else emptyList()

        // 이번 달 그리드 (Large 에서만 표시, 월 제목·빈 칸 수는 가벼워서 항상 계산)
        val monthStart = now.startOfMonth()
        val leadingBlanks = monthStart.weekdaySundayFirst - 1
        val monthDays = if (family == WidgetFamily.LARGE) (0 until monthStart.lengthOfMonth()).map { monthStart.plusDays(it.toLong()) } else emptyList()

        val days = listOf(now) + upcomingDays + monthDays
        val rangeStart = days.min()
        val rangeEnd = days.max().plusDays(1)

        // 표시 기간에 나올 수 있는 일정만 읽는다 — 단일 일정은 기간 안의 것만, 반복 일정은 판정에 맡긴다
        val database = context.appContainer.database
        val schedules = database.scheduleDao().getCandidates(rangeStart.toEpochMillis(zone), rangeEnd.toEpochMillis(zone))
        val anniversaries = database.anniversaryDao().getAll()

        // 필요한 날짜를 모아 한 번에 계산 — 날짜마다 전체 일정을 훑지 않는다
        val eventsByDay = DayEventResolver.eventsByDay(schedules, anniversaries, emptyList(), days, zone)
        fun events(day: LocalDate): List<DayEvent> = eventsByDay[day] ?: emptyList()

        val upcoming = upcomingDays.mapNotNull { day ->
            val items = events(day)
            if (items.isEmpty()) null else WidgetDayGroup(day, items.map(::toWidgetEvent))
        }

        val monthCells = monthDays.map { day ->
            val dayEvents = events(day)
            val chips = dayEvents.take(2).map(::toWidgetEvent)
            MonthCell(
                day = day.dayOfMonth,
                isToday = day == now,
                isHoliday = dayEvents.any { it.kind == DayEvent.Kind.HOLIDAY } || day.isWeekend,
                chips = chips,
                extraCount = dayEvents.size - chips.size,
            )
        }

        return ScheduleEntry(
            date = now,
            today = events(now).map(::toWidgetEvent),
            upcoming = upcoming,
            monthTitle = monthStart.format(monthTitleFormatter),
            leadingBlanks = leadingBlanks,
            monthCells = monthCells,
        )
    }

    private fun toWidgetEvent(event: DayEvent) = WidgetEvent(
        title = event.title,
        colorArgb = event.colorTag?.argb,
        isHoliday = event.kind == DayEvent.Kind.HOLIDAY,
        timeText = event.timeText,
    )

    /** 미리보기용 (위젯 선택 화면) */
    fun placeholder(now: LocalDate = LocalDate.now()): ScheduleEntry = ScheduleEntry(
        date = now,
        today = listOf(
            WidgetEvent("팀 회의", 0xFF5587CE, false, "오전 10:00"),
            WidgetEvent("운동", 0xFF73A965, false, null),
        ),
        upcoming = emptyList(),
        monthTitle = now.startOfMonth().format(monthTitleFormatter),
        leadingBlanks = now.startOfMonth().weekdaySundayFirst - 1,
        monthCells = (1..now.lengthOfMonth()).map { MonthCell(it, it == now.dayOfMonth, false, emptyList(), 0) },
    )
}
