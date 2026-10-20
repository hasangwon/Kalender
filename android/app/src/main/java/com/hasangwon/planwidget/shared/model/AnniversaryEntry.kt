package com.hasangwon.planwidget.shared.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hasangwon.planwidget.shared.Lunar
import java.time.LocalDate

/**
 * 매년 반복 기념일 (생일, 결혼기념일 등). iOS AnniversaryEntry 대응.
 * - 양력: 매년 같은 양력 월/일에 표시
 * - 음력: 매년 음력 월/일을 양력으로 환산해 표시 (윤달 제외)
 */
@Entity(tableName = "anniversary")
data class AnniversaryEntry(
    @PrimaryKey val id: String = Schedule.newId(),
    val name: String = "",
    val month: Int = 1,
    val day: Int = 1,
    val isLunar: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun occurs(date: LocalDate): Boolean =
        if (isLunar) Lunar.matches(date, month, day)
        else date.monthValue == month && date.dayOfMonth == day

    /** 등록 기준 표기 ("음력 7월 9일" / "매년 8월 21일") */
    val dateText: String
        get() = if (isLunar) "음력 ${month}월 ${day}일" else "매년 ${month}월 ${day}일"

    /** 해당 연도에 실제 표시되는 양력 날짜 (없으면 null) */
    fun solarDate(year: Int): LocalDate? =
        if (isLunar) Lunar.solarDate(year, month, day)
        else runCatching { LocalDate.of(year, month, day) }.getOrNull()
}
