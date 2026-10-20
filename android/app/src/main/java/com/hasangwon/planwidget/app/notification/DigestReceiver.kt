package com.hasangwon.planwidget.app.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hasangwon.planwidget.MainActivity
import com.hasangwon.planwidget.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 하루 일괄 알림이 울리는 시점 — 그날 일정을 다시 계산해 알림을 띄우고, 다음 알림을 예약한다 */
class DigestReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                val today = LocalDate.now()
                // 예약 이후 일정이 바뀌었을 수 있으니 울리는 순간의 데이터로 내용을 만든다
                DigestScheduler.digestFor(appContext, today)?.let { post(appContext, today, it) }
            } finally {
                DigestScheduler.refresh(appContext)
                pending.finish()
            }
        }
    }

    private fun post(context: Context, day: LocalDate, digest: DigestScheduler.Digest) {
        if (!canPost(context)) return
        DigestScheduler.ensureChannel(context)

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, DigestScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(digest.title)
            .setContentText(digest.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(digest.body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()
        NotificationManagerCompat.from(context).notify(day.toEpochDay().toInt(), notification)
    }

    private fun canPost(context: Context): Boolean {
        val granted = android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
