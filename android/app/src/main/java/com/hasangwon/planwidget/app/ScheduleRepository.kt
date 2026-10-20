package com.hasangwon.planwidget.app

import android.content.Context
import com.hasangwon.planwidget.app.notification.DigestScheduler
import com.hasangwon.planwidget.shared.model.AnniversaryEntry
import com.hasangwon.planwidget.shared.model.Schedule
import com.hasangwon.planwidget.shared.store.AppDatabase
import com.hasangwon.planwidget.widget.WidgetRefresh
import kotlinx.coroutines.flow.Flow

/**
 * 일정·기념일 변경의 단일 경로 (iOS ModelContext.commitChanges 대응).
 * 저장/삭제 뒤 위젯을 갱신하고, 일정이 바뀌었으면 하루 일괄 알림도 다시 예약한다.
 * 데이터를 바꾸는 코드는 모두 여기를 거친다 — DAO 를 직접 부르지 않는다.
 */
class ScheduleRepository(
    private val context: Context,
    private val database: AppDatabase,
) {
    val schedules: Flow<List<Schedule>> = database.scheduleDao().observeAll()
    val anniversaries: Flow<List<AnniversaryEntry>> = database.anniversaryDao().observeAll()
    val scheduleCount: Flow<Int> = database.scheduleDao().observeCount()

    suspend fun saveSchedule(schedule: Schedule) {
        database.scheduleDao().upsert(schedule)
        commitChanges(refreshesNotifications = true)
    }

    suspend fun deleteSchedule(schedule: Schedule) {
        database.scheduleDao().delete(schedule)
        commitChanges(refreshesNotifications = true)
    }

    suspend fun saveAnniversary(entry: AnniversaryEntry) {
        database.anniversaryDao().upsert(entry)
        commitChanges(refreshesNotifications = false)
    }

    suspend fun deleteAnniversary(entry: AnniversaryEntry) {
        database.anniversaryDao().delete(entry)
        commitChanges(refreshesNotifications = false)
    }

    /** 백업 가져오기 — 한 번에 넣고 한 번만 갱신 */
    suspend fun importAll(schedules: List<Schedule>, anniversaries: List<AnniversaryEntry>) {
        if (schedules.isNotEmpty()) database.scheduleDao().upsertAll(schedules)
        if (anniversaries.isNotEmpty()) database.anniversaryDao().upsertAll(anniversaries)
        if (schedules.isNotEmpty() || anniversaries.isNotEmpty()) {
            commitChanges(refreshesNotifications = schedules.isNotEmpty())
        }
    }

    suspend fun allSchedules(): List<Schedule> = database.scheduleDao().getAll()

    suspend fun allAnniversaries(): List<AnniversaryEntry> = database.anniversaryDao().getAll()

    private suspend fun commitChanges(refreshesNotifications: Boolean) {
        WidgetRefresh.requestUpdate(context)
        if (refreshesNotifications) DigestScheduler.refresh(context)
    }
}
