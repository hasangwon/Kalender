package com.hasangwon.planwidget

import com.hasangwon.planwidget.app.backup.BackupFile
import com.hasangwon.planwidget.app.backup.SwiftDate
import com.hasangwon.planwidget.shared.model.ColorTag
import com.hasangwon.planwidget.shared.model.Recurrence
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** .hscal 형식 — iOS 가 쓴 파일을 읽고, 우리가 쓴 파일이 같은 규칙을 지키는지 (common/spec/backup-format.md) */
class BackupFormatTest {
    @Test
    fun `Swift 기준시 초와 epoch millis 를 서로 바꾼다`() {
        // 2001-01-01T00:00:00Z == Swift 0 == Unix 978307200
        assertEquals(978_307_200_000L, SwiftDate.toEpochMillis(0.0))
        assertEquals(0.0, SwiftDate.fromEpochMillis(978_307_200_000L), 0.0)

        val millis = Instant.parse("2026-10-19T01:00:00Z").toEpochMilli()
        assertEquals(millis, SwiftDate.toEpochMillis(SwiftDate.fromEpochMillis(millis)))
        // 소수점 초도 밀리초 단위로 보존
        assertEquals(813_000_000_500L + 978_307_200_000L, SwiftDate.toEpochMillis(813_000_000.5))
    }

    @Test
    fun `아이폰 백업 샘플을 읽는다`() {
        val file = BackupFile.json.decodeFromString(BackupFile.serializer(), Common.read("fixtures/sample-backup.hscal"))
        assertEquals(1, file.version)
        assertEquals(2, file.schedules.size)
        assertEquals(1, file.anniversaries.size)

        val meeting = file.schedules.first { it.title == "팀 회의" }.toSchedule()
        assertEquals("7C9E6679-7425-40DE-944B-E07FC1F90AE7", meeting.id)
        assertEquals(Recurrence.WEEKLY, meeting.recurrence)
        assertTrue(meeting.hasTime)
        assertTrue(meeting.notifies)
        assertNull(meeting.endDate) // 키가 없으면 null
        assertEquals(SwiftDate.toEpochMillis(813_034_800.0), meeting.startDate)

        val settlement = file.schedules.first { it.title == "월말 정산" }.toSchedule()
        assertEquals(ColorTag.RED, settlement.color)
        assertTrue(settlement.hasCustomColor)
        assertTrue(settlement.monthlyOnLastDay)
        assertEquals(SwiftDate.toEpochMillis(820_000_000.0), settlement.endDate)

        val birthday = file.anniversaries.single().toAnniversary()
        assertEquals("엄마 생일", birthday.name)
        assertTrue(birthday.isLunar)
        assertEquals(7, birthday.month)
        assertEquals(9, birthday.day)
    }

    @Test
    fun `내보내기는 키 정렬·선택 필드 생략·대문자 UUID 규칙을 지킨다`() {
        val original = BackupFile.json.decodeFromString(BackupFile.serializer(), Common.read("fixtures/sample-backup.hscal"))
        val roundTrip = BackupFile(
            anniversaries = original.anniversaries.map { BackupFile.AnniversaryRecord.from(it.toAnniversary()) },
            exportedAt = original.exportedAt,
            schedules = original.schedules.map { BackupFile.ScheduleRecord.from(it.toSchedule()) },
            version = original.version,
        )
        val text = BackupFile.json.encodeToString(BackupFile.serializer(), roundTrip)
        val parsed = Json.parseToJsonElement(text).jsonObject

        // 최상위·레코드 키가 알파벳순 (iOS sortedKeys 와 같은 순서)
        assertEquals(parsed.keys.toList(), parsed.keys.sorted())
        val first = parsed.getValue("schedules").jsonArray.first().jsonObject
        assertEquals(first.keys.toList(), first.keys.sorted())

        // endDate 가 없는 일정은 키 자체가 없다 (null 을 쓰지 않는다)
        val meeting = parsed.getValue("schedules").jsonArray.map { it.jsonObject }.first { it["title"].toString().contains("팀 회의") }
        assertFalse(meeting.containsKey("endDate"))

        // 다시 읽어도 같은 값
        val reparsed = BackupFile.json.decodeFromString(BackupFile.serializer(), text)
        assertEquals(original, reparsed)
        assertTrue(reparsed.schedules.all { it.id == it.id.uppercase() })
    }

    @Test
    fun `알 수 없는 키는 무시하고 알 수 없는 원시값은 기본값으로 읽는다`() {
        val text = """
            {"version":1,"exportedAt":0,"anniversaries":[],"futureField":123,
             "schedules":[{"colorRaw":"neon","createdAt":0,"hasCustomColor":true,"hasTime":false,"id":"x",
                           "memo":"","monthlyOnLastDay":false,"notifies":false,"recurrenceRaw":"yearly","startDate":0,"title":"t"}]}
        """.trimIndent()
        val schedule = BackupFile.json.decodeFromString(BackupFile.serializer(), text).schedules.single().toSchedule()
        assertEquals(Recurrence.NONE, schedule.recurrence)
        assertEquals(ColorTag.BLUE, schedule.color)
        assertEquals("X", schedule.id)
    }
}
