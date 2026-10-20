package com.hasangwon.planwidget.shared.store

import com.hasangwon.planwidget.shared.model.ColorTag
import com.hasangwon.planwidget.shared.model.RawValued
import com.hasangwon.planwidget.shared.model.Recurrence

// iOS Shared/Sources/Store/*Settings.swift 대응. 파일 하나에 모았지만 객체 이름은 iOS 와 같다.

/** 반복 유형별 색 설정. 일정 개별 색이 아니라 유형(단일/매주/매달)이 색을 결정한다 */
object EventColorSettings {
    /** 기념일 표시 색 (고정) */
    val anniversaryColor: ColorTag = ColorTag.PINK

    private fun key(recurrence: Recurrence) = "eventColor.${recurrence.raw}"

    private fun defaultColor(recurrence: Recurrence): ColorTag = when (recurrence) {
        Recurrence.NONE -> ColorTag.BLUE
        Recurrence.WEEKLY -> ColorTag.GREEN
        Recurrence.MONTHLY -> ColorTag.ORANGE
    }

    fun color(recurrence: Recurrence): ColorTag = SharedPrefs.value(key(recurrence), defaultColor(recurrence))

    fun setColor(tag: ColorTag, recurrence: Recurrence) = SharedPrefs.set(key(recurrence), tag)
}

/** 앱/위젯 글자 크기 4단계 */
enum class TextSizeOption(override val raw: String, val label: String) : RawValued {
    SMALL("small", "작게"),
    MEDIUM("medium", "중간"),
    LARGE("large", "크게"),
    EXTRA_LARGE("extraLarge", "매우 크게");

    /** iOS dynamicTypeSize 대응 — 제목·캡션 같은 "시스템 텍스트 스타일" 에 곱하는 배율 (태블릿은 한 단계 크게) */
    fun typeScale(isTablet: Boolean): Float = when (this) {
        SMALL -> if (isTablet) 1.0f else 0.88f
        MEDIUM -> if (isTablet) 1.12f else 1.0f
        LARGE -> if (isTablet) 1.23f else 1.12f
        EXTRA_LARGE -> if (isTablet) 1.5f else 1.23f
    }

    /** 앱 내부 고정 크기 폰트에 곱할 배율 (iOS scale). 폰은 좁은 셀에 맞춰 완만하게, 태블릿은 넓은 셀을 채우도록 크게 */
    fun scale(isTablet: Boolean): Float = when (this) {
        SMALL -> if (isTablet) 1.15f else 0.9f
        MEDIUM -> if (isTablet) 1.35f else 1.0f
        LARGE -> if (isTablet) 1.6f else 1.15f
        EXTRA_LARGE -> if (isTablet) 1.9f else 1.32f
    }

    /** 위젯 전용 배율 — 작은 글씨는 위젯에서 쓸 일이 거의 없어 전체를 한 단계씩 올렸다 */
    val widgetScale: Float
        get() = when (this) {
            SMALL -> 1.0f
            MEDIUM -> 1.18f
            LARGE -> 1.38f
            EXTRA_LARGE -> 1.6f
        }

    /** 위젯 달력 날짜 숫자 배율 — 일정보다 한 단계 크게 */
    val widgetDayScale: Float
        get() = when (this) {
            SMALL -> 1.18f
            MEDIUM -> 1.38f
            LARGE -> 1.6f
            EXTRA_LARGE -> 1.84f
        }
}

object TextSizeSettings {
    private const val KEY = "theme.textSize"

    /** 저장값이 없으면 기기별 기본 (태블릿은 한 단계 크게) */
    fun current(isTablet: Boolean): TextSizeOption =
        SharedPrefs.value(KEY, if (isTablet) TextSizeOption.LARGE else TextSizeOption.MEDIUM)

    fun setCurrent(option: TextSizeOption) = SharedPrefs.set(KEY, option)
}

/** 위젯 글자 크기 — 앱과 독립적으로 저장 */
object WidgetTextSizeSettings {
    private const val KEY = "theme.widgetTextSize"

    val current: TextSizeOption get() = SharedPrefs.value(KEY, TextSizeOption.MEDIUM)

    fun setCurrent(option: TextSizeOption) = SharedPrefs.set(KEY, option)
}

/** 달력 칸 일정 글씨 두께 3단계 (앱·위젯 공통) */
enum class EventTextWeight(override val raw: String, val label: String, val fontWeightValue: Int) : RawValued {
    LIGHT("light", "얇게", 300),
    MEDIUM("medium", "보통", 500),
    SEMIBOLD("semibold", "두껍게", 600),
}

object EventTextWeightSettings {
    private const val KEY = "theme.eventTextWeight"

    val current: EventTextWeight get() = SharedPrefs.value(KEY, EventTextWeight.MEDIUM)

    fun setCurrent(weight: EventTextWeight) = SharedPrefs.set(KEY, weight)
}

/** 달력 월 넘기기 스와이프 방향. 왼쪽/위로 밀면 다음 달, 오른쪽/아래로 밀면 이전 달 */
enum class MonthSwipeDirection(override val raw: String, val label: String) : RawValued {
    HORIZONTAL("horizontal", "좌우"),
    VERTICAL("vertical", "위아래"),
    BOTH("both", "좌우+위아래");

    val allowsHorizontal: Boolean get() = this != VERTICAL
    val allowsVertical: Boolean get() = this != HORIZONTAL
}

object MonthSwipeSettings {
    private const val KEY = "calendar.monthSwipeDirection"

    val current: MonthSwipeDirection get() = SharedPrefs.value(KEY, MonthSwipeDirection.HORIZONTAL)

    fun setCurrent(direction: MonthSwipeDirection) = SharedPrefs.set(KEY, direction)
}

/** 앱 달력 모양 — 와이드(좌우 여백 없이 화면 폭 전체) / 노말(카드). 순서가 설정 화면의 버튼 순서 */
enum class CalendarLayoutStyle(override val raw: String, val label: String) : RawValued {
    WIDE("wide", "와이드"),
    NORMAL("normal", "노말"),
}

object CalendarLayoutSettings {
    private const val KEY = "calendar.layoutStyle"

    val current: CalendarLayoutStyle get() = SharedPrefs.value(KEY, CalendarLayoutStyle.WIDE)

    fun setCurrent(style: CalendarLayoutStyle) = SharedPrefs.set(KEY, style)
}

/** 하루 일괄 알림 시간 설정 */
object NotificationSettings {
    private const val HOUR_KEY = "notification.digestHour"
    private const val MINUTE_KEY = "notification.digestMinute"

    val digestHour: Int get() = SharedPrefs.store.getInt(HOUR_KEY) ?: 8
    val digestMinute: Int get() = SharedPrefs.store.getInt(MINUTE_KEY) ?: 0

    fun setDigestTime(hour: Int, minute: Int) {
        SharedPrefs.store.putInt(HOUR_KEY, hour)
        SharedPrefs.store.putInt(MINUTE_KEY, minute)
    }
}

/** 동기화 관련 사용자 설정. 안드로이드는 iCloud 대응 기능이 없어 기기 달력 연동 하나만 */
object SyncSettings {
    private const val DEVICE_CALENDAR_KEY = "sync.deviceCalendarEnabled"

    /** 안드로이드 달력 앱 일정 표시 (읽기 전용). 기본 꺼짐 */
    var deviceCalendarEnabled: Boolean
        get() = SharedPrefs.store.getBoolean(DEVICE_CALENDAR_KEY, false)
        set(value) = SharedPrefs.store.putBoolean(DEVICE_CALENDAR_KEY, value)
}

/** 실험실 기능 (정보 화면에서 켜고 끔). 모두 기본 꺼짐 */
object LabSettings {
    private const val TODAY_LABEL_KEY = "lab.showsTodayLabel"

    /** 달력에서 오늘 날짜 숫자 옆에 작게 "오늘" 표시 */
    var showsTodayLabel: Boolean
        get() = SharedPrefs.store.getBoolean(TODAY_LABEL_KEY, false)
        set(value) = SharedPrefs.store.putBoolean(TODAY_LABEL_KEY, value)
}
