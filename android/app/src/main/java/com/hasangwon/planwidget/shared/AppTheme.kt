package com.hasangwon.planwidget.shared

import com.hasangwon.planwidget.shared.model.RawValued
import com.hasangwon.planwidget.shared.store.SharedPrefs

// 색값은 common/design/colors.json 과 같다. 바꾸면 그 파일과 iOS AppTheme.swift 도 함께 고친다.

/** 선택 가능한 테마색 (설정 > 테마색). 기본 gold — 앱 아이콘(블랙·골드)과 맞춘 브랜드색 */
enum class ThemeColor(
    override val raw: String,
    val label: String,
    private val light: Long,
    private val dark: Long,
) : RawValued {
    GOLD("gold", "골드", 0xFFAF842F, 0xFFC89E52),
    BLUE("blue", "블루", 0xFF3182F6, 0xFF5B9CF8),
    INDIGO("indigo", "인디고", 0xFF5A65E8, 0xFF7E88F1),
    TEAL("teal", "청록", 0xFF0D9692, 0xFF47B3AF),
    GREEN("green", "그린", 0xFF169651, 0xFF4AB679),
    ORANGE("orange", "오렌지", 0xFFED821B, 0xFFF69B42),
    PINK("pink", "핑크", 0xFFE6518B, 0xFFF176A5),
    PURPLE("purple", "퍼플", 0xFF8B5CE6, 0xFFAB85F1),
    CHARCOAL("charcoal", "차콜", 0xFF3B4352, 0xFF96A0B1);

    fun argb(isDark: Boolean): Long = if (isDark) dark else light
}

object ThemeSettings {
    private const val KEY = "theme.primaryColor"

    val current: ThemeColor get() = SharedPrefs.value(KEY, ThemeColor.GOLD)

    fun setCurrent(theme: ThemeColor) = SharedPrefs.set(KEY, theme)
}

/** 선택 가능한 배경색 (설정 > 배경색) — 연한 톤 + 잉크/블랙골드(다크 강제) */
enum class BackgroundColor(
    override val raw: String,
    val label: String,
    /** 라이트 값. 스와치에도 이 값을 쓴다 */
    val swatch: Long,
) : RawValued {
    GRAY("gray", "그레이", 0xFFF8F9FA),
    CREAM("cream", "살구", 0xFFFEF9F3),
    PINK("pink", "핑크", 0xFFFEF8F9),
    BLUE("blue", "블루", 0xFFF6F9FE),
    GREEN("green", "그린", 0xFFF7FBF8),
    LAVENDER("lavender", "라벤더", 0xFFFAF8FE),
    INK("ink", "잉크", 0xFF1E1F22),
    BLACK_GOLD("blackGold", "블랙골드", 0xFF12100D);

    /** 잉크·블랙골드 선택 시 앱 전체를 다크 팔레트로 렌더링 */
    val forcesDark: Boolean get() = this == INK || this == BLACK_GOLD

    fun argb(isDark: Boolean): Long = when {
        forcesDark -> swatch
        isDark -> 0xFF1B1C1F
        else -> swatch
    }
}

object BackgroundSettings {
    private const val KEY = "theme.backgroundColor"

    val current: BackgroundColor get() = SharedPrefs.value(KEY, BackgroundColor.GRAY)

    fun setCurrent(background: BackgroundColor) = SharedPrefs.set(KEY, background)
}

/** 앱 전역 테마. 대표색·배경색은 설정에서 선택. 값은 ARGB Long — Compose Color 변환은 UI 층에서 */
object AppTheme {
    /** 대표 테마색 — 선택/오늘/버튼/포인트 */
    fun primary(isDark: Boolean): Long = ThemeSettings.current.argb(isDark)

    /** 주말·공휴일 표시용 고정 빨강 (테마색과 무관) */
    fun holidayRed(isDark: Boolean): Long = if (isDark) 0xFFEF6969 else 0xFFDD3E3E

    /** 배경 — 설정에서 선택 (기본 그레이) */
    fun background(isDark: Boolean): Long = BackgroundSettings.current.argb(isDark)

    /** 보조색 (딥 세이지) */
    fun accent(isDark: Boolean): Long = if (isDark) 0xFF8FB3A3 else 0xFF4A6B5D

    /** 카드/셀 표면 (배경 위 한 단계) */
    fun surface(isDark: Boolean): Long = if (isDark) 0xFF2B2D31 else 0xFFFFFFFF

    /** 실제로 다크 팔레트를 써야 하는지 — 배경 설정이 강제하거나 시스템이 다크일 때 */
    fun isDark(systemDark: Boolean): Boolean = BackgroundSettings.current.forcesDark || systemDark
}
