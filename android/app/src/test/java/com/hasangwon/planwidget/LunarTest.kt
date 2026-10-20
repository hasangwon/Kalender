package com.hasangwon.planwidget

import com.hasangwon.planwidget.shared.Lunar
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class LunarTest {
    private val fixture by lazy { Json.parseToJsonElement(Common.read("fixtures/lunar-expected.json")).jsonObject }

    @Before
    fun setUp() = prepareSharedForTests()

    @Test
    fun `음력 월일을 양력으로 환산한다`() {
        for (case in fixture.getValue("lunarToSolar").jsonArray) {
            val item = case.jsonObject
            val expected = LocalDate.parse(item.getValue("solar").jsonPrimitive.content)
            val actual = Lunar.solarDate(
                item.getValue("year").jsonPrimitive.int,
                item.getValue("month").jsonPrimitive.int,
                item.getValue("day").jsonPrimitive.int,
            )
            assertEquals("음력 ${item["year"]}-${item["month"]}-${item["day"]}", expected, actual)
        }
    }

    @Test
    fun `양력 날짜의 음력 월일을 읽는다`() {
        for (case in fixture.getValue("solarToLunar").jsonArray) {
            val item = case.jsonObject
            val date = LocalDate.parse(item.getValue("solar").jsonPrimitive.content)
            val month = item.getValue("month").jsonPrimitive.int
            val day = item.getValue("day").jsonPrimitive.int
            assertTrue("$date 는 음력 $month/$day", Lunar.matches(date, month, day))
            assertEquals("음력 ${month}월 ${day}일", Lunar.text(date))
        }
    }

    @Test
    fun `윤달은 평달 기념일과 매칭되지 않고 윤 표기가 붙는다`() {
        val leap = fixture.getValue("leapMonth").jsonObject
        val date = LocalDate.parse(leap.getValue("solar").jsonPrimitive.content)
        val month = leap.getValue("month").jsonPrimitive.int
        val day = leap.getValue("day").jsonPrimitive.int
        assertTrue(leap.getValue("leap").jsonPrimitive.boolean)
        assertFalse(Lunar.matches(date, month, day))
        assertEquals("음력 윤${month}월 ${day}일", Lunar.text(date))
    }

    @Test
    fun `해당 연도에 없는 음력 날짜는 null`() {
        // 음력 13월은 없다
        assertEquals(null, Lunar.solarDate(2026, 13, 1))
    }
}
