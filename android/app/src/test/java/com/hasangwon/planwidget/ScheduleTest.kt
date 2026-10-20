package com.hasangwon.planwidget

import com.hasangwon.planwidget.shared.model.AnniversaryEntry
import com.hasangwon.planwidget.shared.model.ColorTag
import com.hasangwon.planwidget.shared.model.Recurrence
import com.hasangwon.planwidget.shared.model.Schedule
import com.hasangwon.planwidget.shared.store.DayEvent
import com.hasangwon.planwidget.shared.store.DayEventResolver
import com.hasangwon.planwidget.shared.store.EventColorSettings
import com.hasangwon.planwidget.shared.store.ScheduleStore
import com.hasangwon.planwidget.shared.toEpochMillis
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ScheduleTest {
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")

    @Before
    fun setUp() = prepareSharedForTests()

    private fun schedule(
        start: LocalDate,
        recurrence: Recurrence = Recurrence.NONE,
        end: LocalDate? = null,
        monthlyOnLastDay: Boolean = false,
        title: String = "일정",
        time: LocalTime? = null,
    ) = Schedule(
        title = title,
        startDate = time?.let { start.atTime(it).atZone(zone).toInstant().toEpochMilli() } ?: start.toEpochMillis(zone),
        hasTime = time != null,
        recurrenceRaw = recurrence.raw,
        endDate = end?.toEpochMillis(zone),
        monthlyOnLastDay = monthlyOnLastDay,
    )

    @Test
    fun `반복 발생 판정은 공용 기대값과 일치한다`() {
        val cases = Json.parseToJsonElement(Common.read("fixtures/recurrence-cases.json")).jsonObject.getValue("cases").jsonArray
        for (case in cases) {
            val item = case.jsonObject
            val subject = schedule(
                start = LocalDate.parse(item.getValue("start").jsonPrimitive.content),
                recurrence = Recurrence.from(item.getValue("recurrence").jsonPrimitive.content),
                end = item["end"]?.jsonPrimitive?.content?.let(LocalDate::parse),
                monthlyOnLastDay = item["monthlyOnLastDay"]?.jsonPrimitive?.boolean ?: false,
            )
            val day = LocalDate.parse(item.getValue("day").jsonPrimitive.content)
            assertEquals(item.getValue("name").jsonPrimitive.content, item.getValue("expected").jsonPrimitive.boolean, subject.occurs(day, zone))
        }
    }

    @Test
    fun `후보 줄이기 결과는 전체 판정과 같다`() {
        val schedules = listOf(
            schedule(LocalDate.of(2026, 10, 5), Recurrence.WEEKLY, title = "매주"),
            schedule(LocalDate.of(2026, 1, 31), Recurrence.MONTHLY, title = "매달 31"),
            schedule(LocalDate.of(2026, 1, 10), Recurrence.MONTHLY, monthlyOnLastDay = true, title = "말일"),
            schedule(LocalDate.of(2026, 10, 20), title = "단일"),
            schedule(LocalDate.of(2026, 9, 1), Recurrence.WEEKLY, end = LocalDate.of(2026, 9, 30), title = "끝난 매주"),
        )
        val days = (0 until 61).map { LocalDate.of(2026, 10, 1).plusDays(it.toLong()) }

        val candidates = ScheduleStore.candidatesByDay(schedules, days, zone)
        for (day in days) {
            val expected = schedules.filter { it.occurs(day, zone) }.map { it.title }.sorted()
            val actual = ScheduleStore.occurrences(candidates[day] ?: emptyList(), day, zone).map { it.title }.sorted()
            assertEquals("$day", expected, actual)
        }
    }

    @Test
    fun `하루 일정은 종일 → 시간순 → 제목순으로 정렬된다`() {
        val day = LocalDate.of(2026, 10, 19)
        val list = ScheduleStore.occurrences(
            listOf(
                schedule(day, title = "나 15시", time = LocalTime.of(15, 0)),
                schedule(day, title = "다 종일"),
                schedule(day, title = "가 09시", time = LocalTime.of(9, 0)),
                schedule(day, title = "가 종일"),
            ),
            day, zone,
        )
        assertEquals(listOf("가 종일", "다 종일", "가 09시", "나 15시"), list.map { it.title })
        assertEquals("오전 9:00", list[2].timeText(zone))
        assertEquals("오후 3:00", list[3].timeText(zone))
    }

    @Test
    fun `하루 이벤트는 공휴일 → 기념일 → 일정 순서이고 색 규칙을 따른다`() {
        EventColorSettings.setColor(ColorTag.PURPLE, Recurrence.WEEKLY)
        val day = LocalDate.of(2026, 10, 9) // 한글날, 금요일
        val events = DayEventResolver.events(
            schedules = listOf(
                schedule(LocalDate.of(2026, 10, 2), Recurrence.WEEKLY, title = "매주 금"),
                schedule(day, title = "단일").copy(hasCustomColor = true, colorRaw = ColorTag.RED.raw),
            ),
            anniversaries = listOf(AnniversaryEntry(name = "기념일", month = 10, day = 9)),
            day = day,
            zone = zone,
        )
        assertEquals(listOf(DayEvent.Kind.HOLIDAY, DayEvent.Kind.ANNIVERSARY, DayEvent.Kind.SCHEDULE, DayEvent.Kind.SCHEDULE), events.map { it.kind })
        assertEquals("한글날", events[0].title)
        assertEquals(ColorTag.PINK, events[1].colorTag)
        assertEquals(ColorTag.PURPLE, events.first { it.title == "매주 금" }.colorTag)
        assertEquals(ColorTag.RED, events.first { it.title == "단일" }.colorTag)
    }

    @Test
    fun `음력 기념일은 매년 환산된 양력 날짜에 발생한다`() {
        val entry = AnniversaryEntry(name = "설", month = 1, day = 1, isLunar = true)
        assertEquals(true, entry.occurs(LocalDate.of(2026, 2, 17)))
        assertEquals(false, entry.occurs(LocalDate.of(2026, 1, 1)))
        assertEquals(LocalDate.of(2027, 2, 7), entry.solarDate(2027))
        assertEquals("음력 1월 1일", entry.dateText)
    }
}
