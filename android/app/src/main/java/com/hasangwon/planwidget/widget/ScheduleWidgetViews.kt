package com.hasangwon.planwidget.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.hasangwon.planwidget.MainActivity
import com.hasangwon.planwidget.R
import com.hasangwon.planwidget.shared.AppTheme
import com.hasangwon.planwidget.shared.BackgroundSettings
import com.hasangwon.planwidget.shared.KOREAN
import com.hasangwon.planwidget.shared.WEEKDAY_SYMBOLS
import com.hasangwon.planwidget.shared.store.EventTextWeight
import com.hasangwon.planwidget.shared.store.EventTextWeightSettings
import com.hasangwon.planwidget.shared.store.WidgetTextSizeSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 위젯이 그릴 때 쓰는 색·배율 스냅샷. 설정에서 읽어 한 번 만든다.
 * 잉크·블랙골드 배경은 시스템 모드와 무관하게 다크 색을 고정한다 (iOS forcesDark).
 */
class WidgetTheme(
    val primary: ColorProvider,
    val background: ColorProvider,
    val holidayRed: ColorProvider,
    val textPrimary: ColorProvider,
    val textSecondary: ColorProvider,
    /** 일정 글자 배율 (설정 > 글자 크기 > 위젯) */
    val scale: Float,
    /** 달력 날짜 숫자 배율 — 일정보다 한 단계 크게 */
    val dayScale: Float,
    /** 일정 이름 두께 */
    val eventWeight: FontWeight,
) {
    companion object {
        fun current(): WidgetTheme {
            val forcesDark = BackgroundSettings.current.forcesDark
            fun dynamic(light: Long, dark: Long): ColorProvider =
                if (forcesDark) ColorProvider(Color(dark)) else ColorProvider(day = Color(light), night = Color(dark))

            val textSize = WidgetTextSizeSettings.current
            return WidgetTheme(
                primary = dynamic(AppTheme.primary(false), AppTheme.primary(true)),
                background = dynamic(AppTheme.background(false), AppTheme.background(true)),
                holidayRed = dynamic(AppTheme.holidayRed(false), AppTheme.holidayRed(true)),
                textPrimary = dynamic(0xFF000000, 0xFFFFFFFF),
                textSecondary = dynamic(0x99000000, 0x99FFFFFF),
                scale = textSize.widgetScale,
                dayScale = textSize.widgetDayScale,
                eventWeight = when (EventTextWeightSettings.current) {
                    EventTextWeight.LIGHT -> FontWeight.Normal
                    EventTextWeight.MEDIUM -> FontWeight.Medium
                    EventTextWeight.SEMIBOLD -> FontWeight.Bold
                },
            )
        }
    }
}

private val monthDigitFormatter = DateTimeFormatter.ofPattern("M월", KOREAN)
private val weekdayWideFormatter = DateTimeFormatter.ofPattern("EEEE", KOREAN)
private val upcomingFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", KOREAN)

/**
 * 칸 폭에 들어가는 글자까지만 남긴다 (iOS CharClippedText 대응).
 * RemoteViews 는 말줄임(…)을 끌 수 없으므로, 그리기 전에 글꼴로 폭을 재서 넘치는 글자를 잘라 둔다.
 */
private fun fitToWidth(context: android.content.Context, text: String, maxWidthDp: Float, textSizeSp: Float, bold: Boolean): String {
    val metrics = context.resources.displayMetrics
    val paint = android.graphics.Paint().apply {
        textSize = textSizeSp * metrics.scaledDensity
        typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
    }
    val maxWidthPx = (maxWidthDp * metrics.density) - 2f
    if (maxWidthPx <= 0f) return ""
    val count = paint.breakText(text, true, maxWidthPx, null)
    return text.take(count)
}

@Composable
fun ScheduleWidgetContent(entry: ScheduleEntry, family: WidgetFamily, theme: WidgetTheme) {
    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(theme.background)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        when (family) {
            // 기본 여백을 없애고 얇은 자체 패딩만 사용 — 달력을 최대한 크게
            WidgetFamily.LARGE -> CalendarGridView(entry, theme, GlanceModifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp))
            WidgetFamily.MEDIUM -> MediumScheduleView(entry, theme, GlanceModifier.fillMaxSize().padding(14.dp))
            WidgetFamily.SMALL -> SmallScheduleView(entry, theme, GlanceModifier.fillMaxSize().padding(14.dp))
        }
    }
}

// MARK: - 공통 요소

@Composable
private fun DateHeader(date: LocalDate, theme: WidgetTheme) {
    Column {
        Text(date.format(monthDigitFormatter), style = TextStyle(color = theme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold))
        Text("${date.dayOfMonth}", style = TextStyle(color = theme.textPrimary, fontSize = (34 * theme.scale).sp, fontWeight = FontWeight.Bold))
        Text(date.format(weekdayWideFormatter), style = TextStyle(color = theme.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun EventRow(event: WidgetEvent, theme: WidgetTheme) {
    val bar = eventColor(event, theme)
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.width(3.dp).height(22.dp).background(bar).cornerRadius(1.5.dp)) {}
        Spacer(GlanceModifier.width(6.dp))
        Column {
            Text(event.title, maxLines = 1, style = TextStyle(color = theme.textPrimary, fontSize = (12 * theme.scale).sp, fontWeight = theme.eventWeight))
            if (event.timeText != null) {
                Text(event.timeText, maxLines = 1, style = TextStyle(color = theme.textSecondary, fontSize = (11 * theme.scale).sp))
            }
        }
    }
}

@Composable
private fun EmptyEventsView(theme: WidgetTheme, modifier: GlanceModifier = GlanceModifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(R.drawable.ic_check_circle),
            contentDescription = null,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = ColorFilter.tint(theme.textSecondary),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text("일정 없음", style = TextStyle(color = theme.textSecondary, fontSize = 11.sp))
    }
}

private fun eventColor(event: WidgetEvent, theme: WidgetTheme): ColorProvider = when {
    event.isHoliday -> theme.holidayRed
    event.colorArgb != null -> ColorProvider(Color(event.colorArgb))
    else -> theme.textSecondary
}

// MARK: - Small

@Composable
private fun SmallScheduleView(entry: ScheduleEntry, theme: WidgetTheme, modifier: GlanceModifier) {
    Column(modifier) {
        Row(GlanceModifier.fillMaxWidth()) {
            DateHeader(entry.date, theme)
            Spacer(GlanceModifier.defaultWeight())
            if (entry.today.size > 2) {
                Text("+${entry.today.size - 2}", style = TextStyle(color = theme.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        if (entry.today.isEmpty()) {
            EmptyEventsView(theme, GlanceModifier.fillMaxWidth().height(44.dp))
        } else {
            Column { for (event in entry.today.take(2)) EventRow(event, theme) }
        }
    }
}

// MARK: - Medium

@Composable
private fun MediumScheduleView(entry: ScheduleEntry, theme: WidgetTheme, modifier: GlanceModifier) {
    Row(modifier) {
        Column(GlanceModifier.width(86.dp).fillMaxHeight()) {
            DateHeader(entry.date, theme)
            Spacer(GlanceModifier.defaultWeight())
            entry.upcoming.firstOrNull()?.let { next ->
                Text(next.date.format(upcomingFormatter), style = TextStyle(color = theme.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                Text(next.items.first().title, maxLines = 1, style = TextStyle(color = theme.textSecondary, fontSize = 11.sp))
            }
        }
        Spacer(GlanceModifier.width(14.dp))
        Box(GlanceModifier.width(1.dp).fillMaxHeight().background(ColorProvider(Color(0x33808080)))) {}
        Spacer(GlanceModifier.width(14.dp))
        if (entry.today.isEmpty()) {
            EmptyEventsView(theme, GlanceModifier.defaultWeight().fillMaxHeight())
        } else {
            Column(GlanceModifier.defaultWeight()) {
                for (event in entry.today.take(3)) EventRow(event, theme)
                if (entry.today.size > 3) {
                    Text("외 ${entry.today.size - 3}개", style = TextStyle(color = theme.textSecondary, fontSize = 11.sp))
                }
            }
        }
    }
}

// MARK: - Large (달력형)

/** 월 그리드 셀들을 7칸 × N주 행으로 나눔 */
private fun makeWeekRows(leadingBlanks: Int, cells: List<MonthCell>): List<List<MonthCell?>> {
    val slots = mutableListOf<MonthCell?>()
    repeat(leadingBlanks) { slots += null }
    slots += cells
    while (slots.size % 7 != 0) slots += null
    return slots.chunked(7)
}

@Composable
private fun CalendarGridView(entry: ScheduleEntry, theme: WidgetTheme, modifier: GlanceModifier) {
    // 칸 폭 = (위젯 폭 - 좌우 패딩 20) / 7 - 칸 패딩 2
    val cellWidthDp = (LocalSize.current.width.value - 20f) / 7f - 2f
    Column(modifier) {
        Text(
            entry.monthTitle,
            modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(color = theme.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        )
        Spacer(GlanceModifier.height(4.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            WEEKDAY_SYMBOLS.forEachIndexed { index, symbol ->
                Text(
                    symbol,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = if (index == 0 || index == 6) theme.holidayRed else theme.textSecondary,
                        fontSize = (8 * theme.scale).sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.height(4.dp))

        // 남은 위젯 영역을 주 단위 행으로 균등 분할 — 빈 공간 없이 꽉 채움
        val rows = makeWeekRows(entry.leadingBlanks, entry.monthCells)
        Column(GlanceModifier.fillMaxWidth().defaultWeight()) {
            for (row in rows) {
                Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    for (cell in row) {
                        if (cell == null) {
                            Spacer(GlanceModifier.defaultWeight().fillMaxHeight())
                        } else {
                            DayCell(cell, theme, cellWidthDp, GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = 1.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(cell: MonthCell, theme: WidgetTheme, cellWidthDp: Float, modifier: GlanceModifier) {
    val context = LocalContext.current
    val daySide = (16 * theme.dayScale).dp
    val chipSizeSp = 7.5f * theme.scale
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            GlanceModifier.size(daySide).let { if (cell.isToday) it.background(theme.primary).cornerRadius((5 * theme.dayScale).dp) else it },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${cell.day}",
                style = TextStyle(
                    color = when {
                        cell.isToday -> ColorProvider(Color.White)
                        cell.isHoliday -> theme.holidayRed
                        else -> theme.textPrimary
                    },
                    fontSize = (10 * theme.dayScale).sp,
                    fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
        // 날짜 아래 일정 제목 최대 2줄 — 길면 … 없이 글자 단위로 끊고, 3개째부터 +N
        for (chip in cell.chips) {
            val tint = eventColor(chip, theme)
            Text(
                fitToWidth(context, chip.title, cellWidthDp, chipSizeSp, theme.eventWeight == FontWeight.Bold),
                modifier = GlanceModifier.fillMaxWidth().padding(top = 1.dp).background(ColorProvider(tintWithAlpha(chip, theme))).cornerRadius(2.dp),
                maxLines = 1,
                style = TextStyle(color = tint, fontSize = chipSizeSp.sp, fontWeight = theme.eventWeight, textAlign = TextAlign.Center),
            )
        }
        if (cell.extraCount > 0) {
            Text("+${cell.extraCount}", style = TextStyle(color = theme.textSecondary, fontSize = (6 * theme.scale).sp, fontWeight = FontWeight.Bold))
        }
    }
}

/** 칩 배경 — 글자색의 10% (형광펜처럼 글자 뒤에만 칠한다) */
private fun tintWithAlpha(event: WidgetEvent, theme: WidgetTheme): Color = when {
    event.isHoliday -> Color(AppTheme.holidayRed(false)).copy(alpha = 0.1f)
    event.colorArgb != null -> Color(event.colorArgb).copy(alpha = 0.1f)
    else -> Color(0x1A808080)
}
