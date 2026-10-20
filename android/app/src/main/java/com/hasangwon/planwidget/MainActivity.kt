package com.hasangwon.planwidget

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hasangwon.planwidget.app.notification.DigestScheduler
import com.hasangwon.planwidget.app.ui.CalendarScreen
import com.hasangwon.planwidget.app.ui.theme.PlanWidgetTheme
import com.hasangwon.planwidget.debug.DebugHooks
import java.util.Locale

class MainActivity : ComponentActivity() {
    /** 앱 UI 가 전부 한국어 — 날짜/시간 피커의 AM/PM → 오전/오후 등 표기도 한국어로 고정 (iOS .environment(\.locale, .korean)) */
    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration).apply { setLocale(Locale.KOREAN) }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DebugHooks.handleIntent(this, intent)
        setContent {
            PlanWidgetTheme {
                CalendarScreen()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        DebugHooks.handleIntent(this, intent)
    }

    override fun onStart() {
        super.onStart()
        // 앱을 열 때마다 알림 예약을 다시 계산한다 (iOS onAppear 의 NotificationManager.refresh)
        DigestScheduler.refresh(this)
    }
}
