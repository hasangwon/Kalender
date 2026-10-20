package com.hasangwon.planwidget.shared

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/** 사용자 노출 날짜 포맷용 로케일 (iOS Locale.korean 대응) */
val KOREAN: Locale = Locale.KOREAN

/** 저장된 epoch millis → 기기 시간대의 날짜 */
fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun Long.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDateTime()

/** 그날 0시 (iOS calendar.startOfDay 대응) */
fun LocalDate.toEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun LocalDateTime.toEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atZone(zone).toInstant().toEpochMilli()

/** 그 달 1일 (iOS Calendar.startOfMonth 대응) */
fun LocalDate.startOfMonth(): LocalDate = withDayOfMonth(1)

/** iOS Calendar.weekday 와 같은 번호: 일=1 … 토=7 */
val LocalDate.weekdaySundayFirst: Int
    get() = dayOfWeek.value % 7 + 1

val LocalDate.isWeekend: Boolean
    get() = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

val LocalDate.isLastDayOfMonth: Boolean
    get() = dayOfMonth == lengthOfMonth()

/** 요일 한 글자 (일~토). 달력 헤더·위젯 공용 */
val WEEKDAY_SYMBOLS: List<String> = listOf("일", "월", "화", "수", "목", "금", "토")

/** 요일 전체 이름 (iOS weekday(.wide) 대응) */
val LocalDate.weekdayWide: String
    get() = WEEKDAY_SYMBOLS[weekdaySundayFirst - 1] + "요일"
