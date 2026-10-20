package com.hasangwon.planwidget.shared

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 일정 시간 표시("오후 3:30")용 공용 포매터.
 * 포매터 생성은 비싸서 달력 그리드처럼 수백 번 그릴 때 매번 만들지 않는다.
 */
object TimeTextFormatter {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", KOREAN)

    fun format(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        formatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun format(time: LocalTime): String = formatter.format(time)
}
