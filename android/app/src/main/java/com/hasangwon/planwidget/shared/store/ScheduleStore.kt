package com.hasangwon.planwidget.shared.store

import com.hasangwon.planwidget.shared.isLastDayOfMonth
import com.hasangwon.planwidget.shared.model.Recurrence
import com.hasangwon.planwidget.shared.model.Schedule
import com.hasangwon.planwidget.shared.toLocalDate
import com.hasangwon.planwidget.shared.toLocalDateTime
import java.time.LocalDate
import java.time.ZoneId

/** 일정 필터·정렬 로직 (iOS ScheduleStore 대응). 뷰에 흩뿌리지 않고 여기 모은다 */
object ScheduleStore {
    /** 특정 날짜에 발생하는 일정 (종일 → 시간순 → 제목순) */
    fun occurrences(
        schedules: List<Schedule>,
        day: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Schedule> {
        fun minutes(schedule: Schedule): Int {
            val time = schedule.startDate.toLocalDateTime(zone)
            return time.hour * 60 + time.minute
        }

        return schedules
            .filter { it.occurs(day, zone) }
            .sortedWith(
                compareBy<Schedule> { it.hasTime }
                    .thenBy { if (it.hasTime) minutes(it) else 0 }
                    .thenBy { it.title },
            )
    }

    /**
     * 여러 날짜에 나올 수 있는 일정 후보를 날짜별로.
     * 날짜마다 전체 일정을 훑으면 날짜 수 × 일정 수만큼 판정이 돌기 때문에, 맞을 수 있는 날에만 넣는다.
     * 최종 발생 판정은 호출하는 쪽에서 [occurrences] → [Schedule.occurs] 로 다시 한다 ("후보 줄이기"일 뿐).
     */
    fun candidatesByDay(
        schedules: List<Schedule>,
        days: Collection<LocalDate>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Map<LocalDate, List<Schedule>> {
        val sortedDays = days.toSortedSet()
        if (sortedDays.isEmpty()) return emptyMap()

        val rangeStart = sortedDays.first()
        val rangeEnd = sortedDays.last()

        val candidates = HashMap<LocalDate, MutableList<Schedule>>()
        for (schedule in schedules) {
            val start = schedule.startDate.toLocalDate(zone)
            when (schedule.recurrence) {
                Recurrence.NONE -> {
                    if (start in sortedDays) candidates.getOrPut(start) { mutableListOf() } += schedule
                }
                Recurrence.WEEKLY, Recurrence.MONTHLY -> {
                    // 표시 기간과 겹치지 않는 반복(이미 끝났거나 아직 시작 전)은 건너뛴다
                    val end = schedule.endDate?.toLocalDate(zone)
                    if (start > rangeEnd) continue
                    if (end != null && end < rangeStart) continue

                    for (day in sortedDays) {
                        if (day < start) continue
                        if (end != null && day > end) break

                        val matches = when (schedule.recurrence) {
                            Recurrence.WEEKLY -> day.dayOfWeek == start.dayOfWeek
                            Recurrence.MONTHLY ->
                                if (schedule.monthlyOnLastDay) day.isLastDayOfMonth
                                else day.dayOfMonth == start.dayOfMonth
                            Recurrence.NONE -> false
                        }
                        if (matches) candidates.getOrPut(day) { mutableListOf() } += schedule
                    }
                }
            }
        }
        return candidates
    }
}
