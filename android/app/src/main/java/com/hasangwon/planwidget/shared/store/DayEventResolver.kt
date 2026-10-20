package com.hasangwon.planwidget.shared.store

import com.hasangwon.planwidget.shared.KoreanHolidays
import com.hasangwon.planwidget.shared.TimeTextFormatter
import com.hasangwon.planwidget.shared.model.AnniversaryEntry
import com.hasangwon.planwidget.shared.model.ColorTag
import com.hasangwon.planwidget.shared.model.Schedule
import com.hasangwon.planwidget.shared.toLocalDate
import java.time.LocalDate
import java.time.ZoneId

/** 하루치 표시 이벤트 (공휴일 + 기념일 + 일정 + 안드로이드 달력 통합, 표시용 값 타입) */
data class DayEvent(
    val id: String,
    val kind: Kind,
    val title: String,
    val colorTag: ColorTag?,
    val timeText: String?,
) {
    enum class Kind { HOLIDAY, ANNIVERSARY, SCHEDULE, DEVICE_CALENDAR }
}

/** 안드로이드 기본 달력에서 읽어온 일정 (읽기 전용). iOS AppleCalendarEvent 대응 */
data class DeviceCalendarEvent(
    val id: String,
    val title: String,
    /** 종일 일정은 기기 시간대의 그날 0시로 맞춰서 넣는다 (CalendarContract 는 UTC 0시로 주기 때문) */
    val startMillis: Long,
    val isAllDay: Boolean,
) {
    fun occurs(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        startMillis.toLocalDate(zone) == day

    fun timeText(zone: ZoneId = ZoneId.systemDefault()): String? =
        if (isAllDay) null else TimeTextFormatter.format(startMillis, zone)
}

object DayEventResolver {
    /** 해당 날짜의 전체 표시 이벤트 — 공휴일 → 기념일 → 일정(종일→시간순) → 안드로이드 달력 순서 */
    fun events(
        schedules: List<Schedule>,
        anniversaries: List<AnniversaryEntry>,
        deviceEvents: List<DeviceCalendarEvent> = emptyList(),
        day: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DayEvent> {
        val result = mutableListOf<DayEvent>()

        KoreanHolidays.holidayName(day)?.let { name ->
            result += DayEvent("holiday-$name", DayEvent.Kind.HOLIDAY, name, null, null)
        }

        for (anniversary in anniversaries) {
            if (!anniversary.occurs(day)) continue
            result += DayEvent(
                id = "anniversary-${anniversary.id}",
                kind = DayEvent.Kind.ANNIVERSARY,
                title = anniversary.name,
                colorTag = EventColorSettings.anniversaryColor,
                timeText = if (anniversary.isLunar) anniversary.dateText else null,
            )
        }

        for (schedule in ScheduleStore.occurrences(schedules, day, zone)) {
            result += DayEvent(
                id = "schedule-${schedule.id}",
                kind = DayEvent.Kind.SCHEDULE,
                title = schedule.title,
                colorTag = schedule.displayColor,
                timeText = schedule.timeText(zone),
            )
        }

        for (event in deviceEvents) {
            if (!event.occurs(day, zone)) continue
            result += DayEvent("device-${event.id}", DayEvent.Kind.DEVICE_CALENDAR, event.title, null, event.timeText(zone))
        }

        return result
    }

    /**
     * 여러 날짜의 이벤트를 한 번에 — 달력 그리드용.
     * 날짜마다 전체 일정을 훑으면 칸 수 × 일정 수만큼 판정이 돌아 화면이 멈춘다.
     * 일정을 먼저 날짜별 후보로 나눠 두고, 발생 판정·정렬은 기존 [events] 경로를 그대로 쓴다.
     */
    fun eventsByDay(
        schedules: List<Schedule>,
        anniversaries: List<AnniversaryEntry>,
        deviceEvents: List<DeviceCalendarEvent> = emptyList(),
        days: Collection<LocalDate>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Map<LocalDate, List<DayEvent>> {
        val sortedDays = days.toSortedSet()
        val candidates = ScheduleStore.candidatesByDay(schedules, sortedDays, zone)
        val deviceByDay = deviceEvents.groupBy { it.startMillis.toLocalDate(zone) }

        val result = HashMap<LocalDate, List<DayEvent>>(sortedDays.size)
        for (day in sortedDays) {
            result[day] = events(
                schedules = candidates[day] ?: emptyList(),
                anniversaries = anniversaries,
                deviceEvents = deviceByDay[day] ?: emptyList(),
                day = day,
                zone = zone,
            )
        }
        return result
    }
}
