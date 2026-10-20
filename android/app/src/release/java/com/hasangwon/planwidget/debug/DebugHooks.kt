package com.hasangwon.planwidget.debug

import android.app.Activity
import android.content.Intent

/** 릴리스 빌드 — 디버그 훅 없음 */
object DebugHooks {
    @Suppress("UNUSED_PARAMETER")
    fun handleIntent(activity: Activity, intent: Intent?) = Unit
}
