package com.hasangwon.planwidget.app.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hasangwon.planwidget.app.AppContainer
import com.hasangwon.planwidget.app.ReviewRequester
import com.hasangwon.planwidget.shared.model.AnniversaryEntry
import com.hasangwon.planwidget.shared.model.Schedule
import com.hasangwon.planwidget.shared.store.DeviceCalendarEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 달력 화면의 데이터와 동작. 화면(CalendarScreen)은 표시와 로컬 상태만 맡는다 */
class CalendarViewModel(private val container: AppContainer) : ViewModel() {
    val schedules: StateFlow<List<Schedule>> = container.repository.schedules
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val anniversaries: StateFlow<List<AnniversaryEntry>> = container.repository.anniversaries
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deviceEvents: StateFlow<List<DeviceCalendarEvent>> = container.deviceCalendar.events

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch { container.repository.deleteSchedule(schedule) }
    }

    /** 달이 바뀌면 안드로이드 달력 일정을 그 달 앞뒤로 다시 읽는다 (켜져 있을 때만) */
    fun loadDeviceEvents(around: LocalDate) {
        container.deviceCalendar.loadEvents(around)
    }

    fun refreshDeviceCalendar() {
        container.deviceCalendar.refreshAuthorization()
    }

    /**
     * 일정을 막 추가한 직후(긍정적인 순간)에만, 조건을 만족하면 리뷰를 요청한다.
     * 실제 노출 여부는 시스템이 정하므로 시도 자체를 버전당 한 번으로 제한한다.
     */
    fun requestReviewIfEarned(activity: Activity) {
        if (!ReviewRequester.shouldRequest(activity, schedules.value.size)) return
        ReviewRequester.markRequested(activity)
        viewModelScope.launch {
            // 시트가 완전히 닫힌 뒤에 띄워야 시스템 팝업이 가려지지 않는다
            delay(1000)
            ReviewRequester.requestReview(activity)
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { CalendarViewModel(container) }
        }
    }
}
