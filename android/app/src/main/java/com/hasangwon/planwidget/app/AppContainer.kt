package com.hasangwon.planwidget.app

import android.content.Context
import com.hasangwon.planwidget.PlanWidgetApp
import com.hasangwon.planwidget.app.calendar.DeviceCalendarManager
import com.hasangwon.planwidget.shared.store.AppDatabase

/** 앱 전역 의존성. Application 이 하나만 들고, 화면·위젯·리시버가 Context 로 꺼내 쓴다 */
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.build(context)
    val repository: ScheduleRepository = ScheduleRepository(context.applicationContext, database)
    val deviceCalendar: DeviceCalendarManager = DeviceCalendarManager(context.applicationContext)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as PlanWidgetApp).container
