package com.hasangwon.planwidget

import com.hasangwon.planwidget.shared.KoreanHolidays
import com.hasangwon.planwidget.shared.isWeekend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class KoreanHolidaysTest {
    @Before
    fun setUp() = prepareSharedForTests()

    @Test
    fun `확정 테이블 2025~2027 이 로드된다`() {
        assertEquals(setOf(2025, 2026, 2027), KoreanHolidays.fixedYears)
        assertEquals("추석", KoreanHolidays.holidayName(LocalDate.of(2025, 10, 6)))
        assertEquals("임시공휴일", KoreanHolidays.holidayName(LocalDate.of(2025, 1, 27)))
        assertEquals("지방선거일", KoreanHolidays.holidayName(LocalDate.of(2026, 6, 3)))
        assertEquals("노동절", KoreanHolidays.holidayName(LocalDate.of(2026, 5, 1)))
        assertEquals("대체공휴일", KoreanHolidays.holidayName(LocalDate.of(2027, 12, 27)))
        assertNull(KoreanHolidays.holidayName(LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun `테이블 밖 연도는 자동 계산한다 - 고정 공휴일과 음력 공휴일`() {
        val year = 2028
        assertEquals("신정", KoreanHolidays.holidayName(LocalDate.of(year, 1, 1)))
        assertEquals("노동절", KoreanHolidays.holidayName(LocalDate.of(year, 5, 1)))
        assertEquals("제헌절", KoreanHolidays.holidayName(LocalDate.of(year, 7, 17)))
        assertEquals("성탄절", KoreanHolidays.holidayName(LocalDate.of(year, 12, 25)))
        // 설날·추석 (fixtures/lunar-expected.json 과 같은 날)
        assertEquals("설날", KoreanHolidays.holidayName(LocalDate.of(2028, 1, 26)))
        assertEquals("설날 연휴", KoreanHolidays.holidayName(LocalDate.of(2028, 1, 25)))
        assertEquals("설날 연휴", KoreanHolidays.holidayName(LocalDate.of(2028, 1, 27)))
        assertEquals("설날", KoreanHolidays.holidayName(LocalDate.of(2029, 2, 13)))
        assertEquals("추석", KoreanHolidays.holidayName(LocalDate.of(2029, 9, 22)))
    }

    @Test
    fun `2025년 이전 연도는 노동절·제헌절이 없다`() {
        assertNull(KoreanHolidays.holidayName(LocalDate.of(2024, 5, 1)))
        assertNull(KoreanHolidays.holidayName(LocalDate.of(2024, 7, 17)))
        assertEquals("어린이날", KoreanHolidays.holidayName(LocalDate.of(2024, 5, 5)))
    }

    @Test
    fun `대체공휴일은 평일에만 배정되고 날짜가 겹치지 않는다`() {
        for (year in listOf(2024, 2028, 2029, 2030, 2031, 2032)) {
            val holidays = KoreanHolidays.holidays(year)
            val dates = holidays.map { LocalDate.of(year, it.month, it.day) }
            assertEquals("$year 중복 날짜", dates.size, dates.toSet().size)
            holidays.filter { it.name == "대체공휴일" }.forEach {
                assertFalse("$year-${it.month}-${it.day} 대체공휴일이 주말", LocalDate.of(year, it.month, it.day).isWeekend)
            }
        }
    }

    @Test
    fun `주말 국경일에는 다음 평일이 대체공휴일이 된다`() {
        // 2028-10-09 한글날은 월요일 → 대체 없음. 2028-12-25 성탄절은 월요일 → 대체 없음.
        // 2030-03-01 삼일절은 금요일 → 대체 없음. 2030-06-06 현충일은 목요일.
        // 2029-10-03 개천절은 수요일. 2029-12-25 성탄절은 화요일.
        // 2030-08-15 광복절은 목요일. 2031-03-01 삼일절은 토요일 → 3/3 월요일 대체.
        assertEquals("삼일절", KoreanHolidays.holidayName(LocalDate.of(2031, 3, 1)))
        assertEquals("대체공휴일", KoreanHolidays.holidayName(LocalDate.of(2031, 3, 3)))
        // 2030-10-03 개천절 목요일, 2030-10-09 한글날 수요일, 2030-12-25 성탄절 수요일 → 대체 없음
        assertNull(KoreanHolidays.holidayName(LocalDate.of(2030, 10, 4)))
    }

    @Test
    fun `테이블 로드 전에도 자동 계산으로 동작한다`() {
        KoreanHolidays.loadFixedTable("""{"years":{}}""")
        assertTrue(KoreanHolidays.fixedYears.isEmpty())
        assertEquals("신정", KoreanHolidays.holidayName(LocalDate.of(2026, 1, 1)))
        prepareSharedForTests()
    }
}
