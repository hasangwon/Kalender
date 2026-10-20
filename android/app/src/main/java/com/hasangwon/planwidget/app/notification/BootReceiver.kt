package com.hasangwon.planwidget.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hasangwon.planwidget.widget.WidgetRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 부팅·시간·시간대 변경 뒤 알람은 모두 사라지므로 알림 예약과 위젯을 다시 세운다 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                val appContext = context.applicationContext
                DigestScheduler.refresh(appContext)
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        WidgetRefresh.requestUpdate(appContext)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
