package com.hasangwon.planwidget.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

/**
 * 홈 위젯 (iOS ScheduleWidget 대응). 사용자가 크기를 바꿀 수 있는 안드로이드 위젯을
 * Small / Medium / Large 3구간으로 받아 iOS 와 같은 레이아웃을 그린다.
 */
class ScheduleWidget : GlanceAppWidget() {
    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
        val LARGE = DpSize(250.dp, 250.dp)

        fun familyOf(size: DpSize): WidgetFamily = when {
            size.width >= LARGE.width && size.height >= LARGE.height -> WidgetFamily.LARGE
            size.width >= MEDIUM.width -> WidgetFamily.MEDIUM
            else -> WidgetFamily.SMALL
        }
    }

    /** 실제 위젯 크기로 그린다 — 구간(Small/Medium/Large)은 [familyOf] 가 정하고, 달력 칸 글자 맞춤에는 실제 폭을 쓴다 */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 데이터·설정은 그리기 전에 한 번 읽는다 (provideContent 안은 RemoteViews 로 변환되는 합성 — DB 를 부르지 않는다)
        val entries = WidgetFamily.entries.associateWith { ScheduleProvider.loadEntry(context, it) }
        val theme = WidgetTheme.current()

        provideContent {
            val family = familyOf(LocalSize.current)
            ScheduleWidgetContent(entries.getValue(family), family, theme)
        }
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefresh.scheduleMidnightRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefresh.cancelMidnightRefresh(context)
    }
}
