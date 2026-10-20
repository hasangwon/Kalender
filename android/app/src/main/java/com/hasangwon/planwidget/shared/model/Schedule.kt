package com.hasangwon.planwidget.shared.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hasangwon.planwidget.shared.TimeTextFormatter
import com.hasangwon.planwidget.shared.isLastDayOfMonth
import com.hasangwon.planwidget.shared.store.EventColorSettings
import com.hasangwon.planwidget.shared.toLocalDate
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * 일정 (iOS Schedule 대응). 필드 이름·원시 문자열은 백업 파일 형식(common/spec/backup-format.md)과 같다.
 * 날짜는 epoch millis 로 저장한다.
 */
@Entity(tableName = "schedule")
data class Schedule(
    @PrimaryKey val id: String = newId(),
    val title: String = "",
    val memo: String = "",
    /** 기준 날짜. 반복 일정이면 반복의 시작일이자 요일/일자의 기준. 종일이면 그날 0시 */
    val startDate: Long = System.currentTimeMillis(),
    /** true 면 startDate 의 시각 사용, false 면 종일 */
    val hasTime: Boolean = false,
    val recurrenceRaw: String = Recurrence.NONE.raw,
    val colorRaw: String = ColorTag.BLUE.raw,
    /** 반복 종료일(포함) 0시. null 이면 무제한. 단일 일정은 항상 null */
    val endDate: Long? = null,
    /** true 면 colorRaw 의 개별 색, false 면 유형 기본 색 */
    val hasCustomColor: Boolean = false,
    /** 하루 일괄 알림 대상 */
    val notifies: Boolean = false,
    /** 매달 반복일 때만 유효. true 면 기준일의 일(day)이 아니라 매달 말일에 반복 */
    val monthlyOnLastDay: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val recurrence: Recurrence get() = Recurrence.from(recurrenceRaw)

    val color: ColorTag get() = ColorTag.from(colorRaw)

    /** 실제 표시 색 — 개별 지정이 없으면 유형 기본 색 */
    val displayColor: ColorTag
        get() = if (hasCustomColor) color else EventColorSettings.color(recurrence)

    /** 반복 배지 — 매달 말일 반복이면 "매달 말일" */
    val recurrenceBadgeText: String?
        get() = if (recurrence == Recurrence.MONTHLY && monthlyOnLastDay) "매달 말일" else recurrence.badgeText

    /**
     * 해당 날짜에 이 일정이 발생하는지. 발생 판정은 이 한 곳에만 둔다 (iOS Schedule.occurs 와 같은 규칙).
     * - none: 시작일 == 대상일
     * - weekly: 시작일 이후 && 요일 일치
     * - monthly: 시작일 이후 && 일 일치 (29~31일 시작은 해당 일자가 없는 달에 미발생), 말일 옵션이면 그 달 마지막 날
     * - 종료일(포함) 이후에는 발생하지 않음
     */
    fun occurs(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val base = startDate.toLocalDate(zone)
        if (day < base) return false

        val end = endDate?.toLocalDate(zone)
        if (end != null && day > end) return false

        return when (recurrence) {
            Recurrence.NONE -> day == base
            Recurrence.WEEKLY -> day.dayOfWeek == base.dayOfWeek
            Recurrence.MONTHLY ->
                if (monthlyOnLastDay) day.isLastDayOfMonth else day.dayOfMonth == base.dayOfMonth
        }
    }

    /** 시간 표시 텍스트 (종일이면 null) */
    fun timeText(zone: ZoneId = ZoneId.systemDefault()): String? =
        if (hasTime) TimeTextFormatter.format(startDate, zone) else null

    companion object {
        /** iOS 와 같은 대문자 UUID 문자열 — 백업 파일에서 서로 읽을 수 있게 */
        fun newId(): String = UUID.randomUUID().toString().uppercase()
    }
}
