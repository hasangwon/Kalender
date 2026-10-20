package com.hasangwon.planwidget.app.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.hasangwon.planwidget.app.appContainer
import com.hasangwon.planwidget.shared.model.Schedule
import com.hasangwon.planwidget.shared.store.NotificationSettings
import com.hasangwon.planwidget.shared.store.ScheduleStore
import com.hasangwon.planwidget.shared.toEpochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 하루 일괄 알림 예약 (iOS NotificationManager 대응).
 * 알림을 켠 일정이 있는 날마다, 설정된 시간에 그날 일정을 모아 알림 1건을 띄운다.
 *
 * iOS 는 로컬 알림 64개 제한 때문에 30일치를 미리 예약하지만, 안드로이드는 제한이 없어
 * **다음 1건만** 정확 알람으로 예약하고 울릴 때([DigestReceiver]) 다음 것을 다시 건다.
 * 앱 실행·일정 변경·알림 시간 변경·부팅·시간 변경 시 [refresh] 로 전체를 다시 계산한다.
 */
object DigestScheduler {
    const val CHANNEL_ID = "daily-digest"
    private const val REQUEST_CODE = 1001

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    /** 예약할 알림 1건 (값 타입) */
    data class Digest(val fireAtMillis: Long, val title: String, val body: String)

    /** 알림 채널 — 알림 토글을 켤 때와 예약 시점에 보장한다 */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "하루 일정 알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "알림을 켠 일정이 있는 날, 설정한 시간에 하루 일정을 모아 알려드려요."
        }
        manager.createNotificationChannel(channel)
    }

    /** 알림 토글을 켤 때 — 채널 준비. 알림 권한(Android 13+)은 화면이 요청한다 */
    fun requestAuthorizationIfNeeded(context: Context) = ensureChannel(context)

    /** 예약 전체 갱신 요청. 연달아 불려도 한 번에 하나씩 순서대로 반영한다 */
    fun refresh(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            mutex.withLock {
                val digest = runCatching { nextDigest(appContext) }.getOrNull()
                schedule(appContext, digest)
            }
        }
    }

    /** 오늘부터 30일 안에서, 아직 울리지 않은 가장 가까운 일괄 알림 */
    suspend fun nextDigest(context: Context, now: ZonedDateTime = ZonedDateTime.now()): Digest? {
        val zone = now.zone
        val today = now.toLocalDate()
        val days = (0 until 30).map { today.plusDays(it.toLong()) }
        // 알림을 켠 일정 중 향후 30일에 나올 수 있는 것만 읽는다 (반복 일정은 판정에 맡김)
        val schedules = context.appContainer.database.scheduleDao()
            .getNotifyingCandidates(today.toEpochMillis(zone), today.plusDays(31).toEpochMillis(zone))
        if (schedules.isEmpty()) return null

        val hour = NotificationSettings.digestHour
        val minute = NotificationSettings.digestMinute
        val candidates = ScheduleStore.candidatesByDay(schedules, days, zone)

        for (day in days) {
            val dayNotes = ScheduleStore.occurrences(candidates[day] ?: emptyList(), day, zone)
            if (dayNotes.isEmpty()) continue
            val fireAt = day.atTime(hour, minute).atZone(zone)
            // 오늘인데 알림 시간이 이미 지났으면 다음 날로
            if (!fireAt.isAfter(now)) continue
            return Digest(fireAt.toInstant().toEpochMilli(), "오늘의 일정 ${dayNotes.count()}개", digestBody(dayNotes, zone))
        }
        return null
    }

    /** 특정 날짜의 알림 내용 — 리시버가 울리는 시점에 다시 계산할 때 쓴다 */
    suspend fun digestFor(context: Context, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Digest? {
        val schedules = context.appContainer.database.scheduleDao()
            .getNotifyingCandidates(day.toEpochMillis(zone), day.plusDays(1).toEpochMillis(zone))
        val dayNotes = ScheduleStore.occurrences(schedules, day, zone)
        if (dayNotes.isEmpty()) return null
        return Digest(0L, "오늘의 일정 ${dayNotes.count()}개", digestBody(dayNotes, zone))
    }

    private fun digestBody(schedules: List<Schedule>, zone: ZoneId): String {
        val titles = schedules.take(3).map { schedule ->
            schedule.timeText(zone)?.let { "${schedule.title} ($it)" } ?: schedule.title
        }
        val joined = titles.joinToString(" · ")
        val remaining = schedules.size - titles.size
        return if (remaining > 0) "$joined 외 ${remaining}개" else joined
    }

    private fun schedule(context: Context, digest: Digest?) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        val intent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, DigestReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (digest == null) {
            alarms.cancel(intent)
            return
        }
        ensureChannel(context)
        // Android 12~13 은 정확 알람 권한을 사용자가 꺼 둘 수 있다 → 그때는 비정확 알람으로 폴백 (몇 분 늦을 수 있음)
        val exactAllowed = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
        if (exactAllowed) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, digest.fireAtMillis, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, digest.fireAtMillis, intent)
        }
    }
}
