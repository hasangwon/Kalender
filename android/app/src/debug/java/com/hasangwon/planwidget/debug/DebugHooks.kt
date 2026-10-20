package com.hasangwon.planwidget.debug

import android.app.Activity
import android.content.Intent
import com.hasangwon.planwidget.app.appContainer
import com.hasangwon.planwidget.shared.model.AnniversaryEntry
import com.hasangwon.planwidget.shared.model.Recurrence
import com.hasangwon.planwidget.shared.model.Schedule
import com.hasangwon.planwidget.shared.toEpochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * 디버그 빌드 전용 — 에뮬레이터 검증용 adb 훅. 릴리스 빌드의 같은 이름 객체는 비어 있다.
 *
 *   adb shell am start -n com.hasangwon.planwidget/.MainActivity --ez seed true   # 샘플 일정 넣기
 *   위젯 렌더링 확인은 DebugWidgetHostActivity 참조
 */
object DebugHooks {
    fun handleIntent(activity: Activity, intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra("seed", false)) seed(activity)
    }

    private fun seed(activity: Activity) {
        val repository = activity.appContainer.repository
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val lastMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        CoroutineScope(Dispatchers.IO).launch {
            val existing = repository.allSchedules()
            if (existing.any { it.memo == "debug-seed" }) return@launch
            val schedules = listOf(
                Schedule(title = "팀 회의", memo = "debug-seed", startDate = today.atTime(10, 0).atZone(zone).toInstant().toEpochMilli(), hasTime = true, notifies = true),
                Schedule(title = "아주 긴 제목의 일정 이름 테스트용", memo = "debug-seed", startDate = today.toEpochMillis(zone)),
                Schedule(title = "운동", memo = "debug-seed", startDate = lastMonday.toEpochMillis(zone), recurrenceRaw = Recurrence.WEEKLY.raw),
                Schedule(title = "월세", memo = "debug-seed", startDate = today.withDayOfMonth(25).minusMonths(3).toEpochMillis(zone), recurrenceRaw = Recurrence.MONTHLY.raw),
                Schedule(title = "정기 점검", memo = "debug-seed", startDate = today.withDayOfMonth(1).minusMonths(2).toEpochMillis(zone), recurrenceRaw = Recurrence.MONTHLY.raw, monthlyOnLastDay = true),
                Schedule(title = "치과", memo = "debug-seed", startDate = today.plusDays(2).atTime(15, 30).atZone(zone).toInstant().toEpochMilli(), hasTime = true),
            )
            val anniversaries = listOf(
                AnniversaryEntry(name = "엄마 생일", month = 9, day = 10, isLunar = true),
                AnniversaryEntry(name = "결혼기념일", month = today.monthValue, day = minOf(today.dayOfMonth + 1, 28)),
            )
            repository.importAll(schedules, anniversaries)
        }
    }

}
