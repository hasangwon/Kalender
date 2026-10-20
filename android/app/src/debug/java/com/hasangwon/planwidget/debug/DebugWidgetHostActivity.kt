package com.hasangwon.planwidget.debug

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.compose
import com.hasangwon.planwidget.widget.ScheduleWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 디버그 전용 — 위젯 3크기를 런처 없이 한 화면에 그려 렌더링을 확인한다.
 * Glance 의 compose() 로 크기별 RemoteViews 를 직접 만들어 붙인다.
 *
 *   adb shell am start -n com.hasangwon.planwidget/.debug.DebugWidgetHostActivity
 */
class DebugWidgetHostActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @OptIn(ExperimentalGlanceApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density
        fun px(dp: Int) = (dp * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#5A6B7D"))
            setPadding(px(16), px(48), px(16), px(48))
        }
        setContentView(ScrollView(this).apply { addView(root) })

        val sizes = listOf(ScheduleWidget.SMALL, ScheduleWidget.MEDIUM, ScheduleWidget.LARGE)
        for (size in sizes) {
            val frame = FrameLayout(this)
            root.addView(frame, LinearLayout.LayoutParams(px(size.width.value.toInt()), px(size.height.value.toInt())).apply { topMargin = px(16) })
            scope.launch {
                val remoteViews = ScheduleWidget().compose(context = this@DebugWidgetHostActivity, size = size)
                frame.addView(remoteViews.apply(this@DebugWidgetHostActivity, frame))
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

