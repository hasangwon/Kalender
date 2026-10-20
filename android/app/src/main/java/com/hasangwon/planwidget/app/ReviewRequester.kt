package com.hasangwon.planwidget.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import com.hasangwon.planwidget.shared.store.SharedPrefs
import java.time.Duration
import java.time.Instant

/**
 * 앱 내 리뷰 요청 조건과 스토어 리뷰 링크를 한곳에서 관리한다 (iOS ReviewRequester 대응).
 *
 * Play In-App Review 도 "요청" 으로만 받아들이고 실제 노출 여부는 시스템이 정한다 (할당량 있음).
 * 그래서 "버전당 한 번만 요청" 해 할당량을 낭비하지 않는 것까지만 책임진다.
 */
object ReviewRequester {
    const val PACKAGE_NAME = "com.hasangwon.planwidget"

    /** 스토어 앱 페이지 — 리뷰 남기기 버튼에서 쓴다 (market:// 가 없으면 웹으로) */
    fun openStorePage(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE_NAME"))
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE_NAME"))
        runCatching { context.startActivity(market) }.onFailure { runCatching { context.startActivity(web) } }
    }

    /** 요청 전 필요한 최소 일정 수 — 앱을 실제로 쓰고 있다는 신호 */
    private const val MINIMUM_SCHEDULE_COUNT = 3
    /** 설치 후 최소 경과 일수 — 설치 직후에는 앱을 평가할 근거가 없다 */
    private const val MINIMUM_DAYS_SINCE_FIRST_LAUNCH = 3L

    private const val FIRST_LAUNCH_KEY = "review.firstLaunchDate"
    private const val REQUESTED_VERSION_KEY = "review.requestedVersion"

    fun currentVersion(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"

    /** 앱 최초 실행 시각을 한 번만 기록한다 */
    fun recordFirstLaunchIfNeeded(now: Instant = Instant.now()) {
        if (SharedPrefs.store.getLong(FIRST_LAUNCH_KEY) != null) return
        SharedPrefs.store.putLong(FIRST_LAUNCH_KEY, now.toEpochMilli())
    }

    /** 지금 리뷰를 요청해도 되는 상태인지 */
    fun shouldRequest(context: Context, scheduleCount: Int, now: Instant = Instant.now()): Boolean {
        if (scheduleCount < MINIMUM_SCHEDULE_COUNT) return false
        if (SharedPrefs.store.getString(REQUESTED_VERSION_KEY) == currentVersion(context)) return false
        val firstLaunch = SharedPrefs.store.getLong(FIRST_LAUNCH_KEY) ?: return false
        val days = Duration.between(Instant.ofEpochMilli(firstLaunch), now).toDays()
        return days >= MINIMUM_DAYS_SINCE_FIRST_LAUNCH
    }

    /** 이번 버전에 대해 요청했음을 기록한다. 실제 노출 여부와 무관하게 한 번만 시도한다 */
    fun markRequested(context: Context) {
        SharedPrefs.store.putString(REQUESTED_VERSION_KEY, currentVersion(context))
    }

    /** Play 인앱 리뷰 흐름 시작 — 실패해도 조용히 넘어간다 */
    fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
        }
    }
}
