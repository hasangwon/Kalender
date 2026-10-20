package com.hasangwon.planwidget.shared

import java.time.LocalDate
import java.time.ZoneId

/**
 * 음력(중국식 태음태양력) 변환 유틸. iOS Lunar 대응.
 * 기기 내장 ICU(android.icu) 의 chinese 캘린더를 쓴다 — iOS Foundation 의 chinese 캘린더도 ICU 구현이라
 * 두 플랫폼 결과가 같다. 한국 천문연구원 기준과 드물게 1일 차이가 날 수 있는 것도 같다.
 */
object Lunar {
    data class Components(val month: Int, val day: Int, val isLeapMonth: Boolean)

    /**
     * 변환 엔진. 앱은 기기의 android.icu, JVM 단위 테스트는 icu4j 로 바꿔 끼운다 (같은 ICU 코드).
     * 기본값은 처음 쓰일 때 만든다 — JVM 테스트에서 android 클래스에 닿지 않도록.
     */
    fun interface Engine {
        /** 해당 시각(epoch millis)의 음력 월/일/윤달 여부 */
        fun components(epochMillis: Long): Components
    }

    @Volatile
    var engine: Engine? = null

    private val resolvedEngine: Engine
        get() = engine ?: AndroidIcuEngine.also { engine = it }

    /**
     * chinese 캘린더 변환은 비싸다. 달력 그리드·기념일 판정·양력 환산이 같은 날짜를 반복해서 묻기 때문에
     * 날짜별로 기억해 둔다. 상한을 넘으면 오래된 것부터 버린다.
     */
    private const val CACHE_LIMIT = 4000
    private val cache = object : LinkedHashMap<LocalDate, Components>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<LocalDate, Components>?): Boolean =
            size > CACHE_LIMIT
    }

    private fun components(date: LocalDate): Components {
        synchronized(cache) { cache[date]?.let { return it } }
        // iOS 와 같은 기준: 그날 0시(기기 시간대)의 시각을 서울 시간대 chinese 캘린더로 해석
        val result = resolvedEngine.components(date.toEpochMillis(ZoneId.systemDefault()))
        synchronized(cache) { cache[date] = result }
        return result
    }

    /** "음력 7월 9일" (윤달이면 "음력 윤7월 9일") */
    fun text(date: LocalDate): String {
        val parts = components(date)
        val leapPrefix = if (parts.isLeapMonth) "윤" else ""
        return "음력 $leapPrefix${parts.month}월 ${parts.day}일"
    }

    /** 해당 양력 날짜가 주어진 음력 월/일인지 (윤달 제외) */
    fun matches(date: LocalDate, month: Int, day: Int): Boolean {
        val parts = components(date)
        return !parts.isLeapMonth && parts.month == month && parts.day == day
    }

    /** 주어진 연도에서 음력 월/일에 해당하는 양력 날짜 (없으면 null) */
    fun solarDate(year: Int, lunarMonth: Int, lunarDay: Int): LocalDate? {
        var candidate = LocalDate.of(year, 1, 1)
        while (candidate.year == year) {
            if (matches(candidate, lunarMonth, lunarDay)) return candidate
            candidate = candidate.plusDays(1)
        }
        return null
    }

    /** 테스트·디버그용 캐시 비우기 */
    fun clearCache() = synchronized(cache) { cache.clear() }
}

/** 기기 내장 ICU. 서울 시간대로 해석한다 (iOS Lunar 와 동일) */
private object AndroidIcuEngine : Lunar.Engine {
    override fun components(epochMillis: Long): Lunar.Components {
        val calendar = android.icu.util.ChineseCalendar(
            android.icu.util.TimeZone.getTimeZone("Asia/Seoul"),
            android.icu.util.ULocale.KOREA,
        )
        calendar.clear()
        calendar.timeInMillis = epochMillis
        return Lunar.Components(
            month = calendar.get(android.icu.util.Calendar.MONTH) + 1, // ICU 는 0부터
            day = calendar.get(android.icu.util.Calendar.DAY_OF_MONTH),
            isLeapMonth = calendar.get(android.icu.util.ChineseCalendar.IS_LEAP_MONTH) == 1,
        )
    }
}
