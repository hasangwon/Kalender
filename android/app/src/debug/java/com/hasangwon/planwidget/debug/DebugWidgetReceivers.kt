package com.hasangwon.planwidget.debug

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.hasangwon.planwidget.widget.ScheduleWidget

// 디버그 전용 — 런처가 처음부터 4×2 / 4×4 로 놓도록 기본 크기만 다른 같은 위젯 (크기별 렌더링 확인용)

class DebugMediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

class DebugLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}
