package com.hasangwon.planwidget.shared

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap

/**
 * 한국 공휴일. iOS KoreanHolidays 대응.
 * - 확정 공고분(2025~2027, 임시공휴일·선거일 포함): common/data/korean-holidays.json 을 [loadFixedTable] 로 넣는다
 * - 그 외 연도: 현행 규정 기준 자동 계산 (고정 공휴일 + 음력 환산 + 대체공휴일)
 * - 노동절(5/1)·제헌절(7/17)은 2026년부터 공휴일. 제헌절은 국경일이라 대체공휴일 적용, 노동절은 미적용
 *   ⚠️ 자동 계산분은 임시공휴일/선거일을 알 수 없고, 음력 환산이 드물게 1일 다를 수 있음
 */
object KoreanHolidays {
    data class Holiday(val month: Int, val day: Int, val name: String)

    @Volatile
    private var fixedTable: Map<Int, List<Holiday>> = emptyMap()
    private val computedCache = ConcurrentHashMap<Int, List<Holiday>>()

    /** 확정 테이블 JSON(common/data/korean-holidays.json)을 읽어 들인다. 앱은 Application 에서 assets 로, 테스트는 파일로 */
    fun loadFixedTable(json: String) {
        val years = Json.parseToJsonElement(json).jsonObject["years"]?.jsonObject ?: return
        fixedTable = years.entries.associate { (year, list) ->
            year.toInt() to list.jsonArray.map { element ->
                val item = element.jsonObject
                Holiday(
                    month = item.getValue("month").jsonPrimitive.int,
                    day = item.getValue("day").jsonPrimitive.int,
                    name = item.getValue("name").jsonPrimitive.content,
                )
            }
        }
        computedCache.clear()
    }

    /** 확정 테이블이 있는 연도 목록 (테스트·정보용) */
    val fixedYears: Set<Int> get() = fixedTable.keys

    fun holidays(year: Int): List<Holiday> =
        fixedTable[year] ?: computedCache.getOrPut(year) { computeHolidays(year) }

    /** 해당 날짜의 공휴일 이름 (없으면 null) */
    fun holidayName(date: LocalDate): String? =
        holidays(date.year).firstOrNull { it.month == date.monthValue && it.day == date.dayOfMonth }?.name

    fun isHoliday(date: LocalDate): Boolean = holidayName(date) != null

    // MARK: - 규정 기반 자동 계산 (iOS computeHolidays 와 같은 순서·규칙)

    private fun computeHolidays(year: Int): List<Holiday> {
        val holidayMap = TreeMap<LocalDate, String>()

        fun dayDate(month: Int, day: Int): LocalDate? = runCatching { LocalDate.of(year, month, day) }.getOrNull()

        fun register(date: LocalDate?, name: String) {
            if (date == null || holidayMap.containsKey(date)) return
            holidayMap[date] = name
        }

        // 고정 공휴일
        register(dayDate(1, 1), "신정")
        register(dayDate(3, 1), "삼일절")
        register(dayDate(5, 5), "어린이날")
        register(dayDate(6, 6), "현충일")
        // 노동절·제헌절은 2026년부터 공휴일
        val hasNewHolidays = year >= 2026
        if (hasNewHolidays) {
            register(dayDate(5, 1), "노동절")
            register(dayDate(7, 17), "제헌절")
        }
        register(dayDate(8, 15), "광복절")
        register(dayDate(10, 3), "개천절")
        register(dayDate(10, 9), "한글날")
        register(dayDate(12, 25), "성탄절")

        // 음력 공휴일 (양력 환산)
        val seollal = Lunar.solarDate(year, 1, 1)
        val buddhaDay = Lunar.solarDate(year, 4, 8)
        val chuseok = Lunar.solarDate(year, 8, 15)

        if (seollal != null) {
            register(seollal.minusDays(1), "설날 연휴")
            register(seollal, "설날")
            register(seollal.plusDays(1), "설날 연휴")
        }
        register(buddhaDay, "부처님오신날")
        if (chuseok != null) {
            register(chuseok.minusDays(1), "추석 연휴")
            register(chuseok, "추석")
            register(chuseok.plusDays(1), "추석 연휴")
        }

        // 대체공휴일
        val substitutes = mutableListOf<LocalDate>()

        /** 기준일 다음의 첫 평일 (기존 공휴일/이미 배정된 대체일 제외) */
        fun nextSubstituteDay(after: LocalDate): LocalDate? {
            var candidate = after
            repeat(10) {
                candidate = candidate.plusDays(1)
                if (!candidate.isWeekend && !holidayMap.containsKey(candidate) && candidate !in substitutes) {
                    return candidate
                }
            }
            return null
        }

        // 토/일 겹침 시 대체 (어린이날·국경일·부처님오신날·성탄절. 신정·현충일·노동절은 제외)
        val singleSubstitutable = listOfNotNull(
            dayDate(3, 1), dayDate(5, 5), buddhaDay,
            if (hasNewHolidays) dayDate(7, 17) else null,
            dayDate(8, 15), dayDate(10, 3), dayDate(10, 9), dayDate(12, 25),
        )
        for (holiday in singleSubstitutable) {
            if (holiday.isWeekend) nextSubstituteDay(holiday)?.let { substitutes += it }
        }

        // 설/추석 연휴는 일요일 겹침 시 대체
        for (center in listOfNotNull(seollal, chuseok)) {
            val holidayRun = listOf(center.minusDays(1), center, center.plusDays(1))
            if (holidayRun.any { it.dayOfWeek == DayOfWeek.SUNDAY }) {
                nextSubstituteDay(holidayRun.last())?.let { substitutes += it }
            }
        }

        for (substitute in substitutes) holidayMap[substitute] = "대체공휴일"

        return holidayMap.map { (date, name) -> Holiday(date.monthValue, date.dayOfMonth, name) }
    }
}
