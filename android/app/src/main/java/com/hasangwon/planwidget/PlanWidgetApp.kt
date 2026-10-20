package com.hasangwon.planwidget

import android.app.Application
import com.hasangwon.planwidget.app.AppContainer
import com.hasangwon.planwidget.app.ReviewRequester
import com.hasangwon.planwidget.shared.KoreanHolidays
import com.hasangwon.planwidget.shared.store.SharedPrefs

/** 앱 전역 객체. DB·설정 저장소처럼 앱과 위젯이 함께 쓰는 싱글턴을 여기서 한 번만 만든다 */
class PlanWidgetApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        SharedPrefs.init(this)
        // 공휴일 확정 테이블 — 저장소 공용 원본(common/data)이 assets 로 들어온다
        KoreanHolidays.loadFixedTable(assets.open("korean-holidays.json").bufferedReader().use { it.readText() })
        container = AppContainer(this)
        ReviewRequester.recordFirstLaunchIfNeeded()
    }
}
