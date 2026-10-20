package com.hasangwon.planwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * 위젯 갱신 (iOS WidgetCenter.reloadAllTimelines + 자정 타임라인 대응).
 * - 일정·설정이 바뀌면 [requestUpdate] — 저장 경로(ScheduleRepository)와 설정 화면이 부른다
 * - 날짜가 바뀌는 자정에는 알람으로 다시 그린다. 위젯이 하나도 없으면 알람을 걸지 않는다
 */
object WidgetRefresh {
    private const val MIDNIGHT_REQUEST_CODE = 1002

    suspend fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        runCatching { ScheduleWidget().updateAll(appContext) }
        scheduleMidnightRefresh(appContext)
    }

    /** 다음 자정 + 5초에 한 번 울리는 알람. 울리면 [MidnightReceiver] 가 다시 건다 */
    fun scheduleMidnightRefresh(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            val hasWidgets = runCatching {
                GlanceAppWidgetManager(appContext).getGlanceIds(ScheduleWidget::class.java).isNotEmpty()
            }.getOrDefault(false)
            val alarms = appContext.getSystemService<AlarmManager>() ?: return@launch
            val intent = midnightIntent(appContext)
            if (!hasWidgets) {
                alarms.cancel(intent)
                return@launch
            }
            val zone = ZoneId.systemDefault()
            val fireAt = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).plusSeconds(5).toInstant().toEpochMilli()
            val exactAllowed = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
            if (exactAllowed) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, intent)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, intent)
        }
    }

    fun cancelMidnightRefresh(context: Context) {
        context.applicationContext.getSystemService<AlarmManager>()?.cancel(midnightIntent(context.applicationContext))
    }

    private fun midnightIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        MIDNIGHT_REQUEST_CODE,
        Intent(context, MidnightReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** 자정 — 위젯의 "오늘" 을 넘기고 다음 자정 알람을 다시 건다 */
class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                WidgetRefresh.requestUpdate(appContext)
            } finally {
                pending.finish()
            }
        }
    }
}
