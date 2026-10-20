package com.hasangwon.planwidget.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.hasangwon.planwidget.shared.AppTheme
import com.hasangwon.planwidget.shared.store.TextSizeOption
import com.hasangwon.planwidget.shared.store.TextSizeSettings

/** 화면이 쓰는 색 묶음 (iOS AppTheme 대응). 설정이 바뀌면 PlanWidgetTheme 이 새로 만든다 */
@Immutable
data class AppColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val holidayRed: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val isDark: Boolean,
) {
    /** iOS `Color.primary.opacity(x)` 대응 — 글자색 바탕의 옅은 채움 */
    fun primaryFill(alpha: Float): Color = textPrimary.copy(alpha = alpha)
}

val LocalAppColors = staticCompositionLocalOf<AppColors> { error("PlanWidgetTheme 안에서만 쓸 수 있습니다") }
val LocalTextSize = compositionLocalOf { TextSizeOption.MEDIUM }
val LocalIsTablet = compositionLocalOf { false }

/**
 * 설정 화면에서 값을 바꾸면 [bump] — 테마·글자 크기·달력 설정을 읽는 화면이 다시 그려진다
 * (iOS 가 설정 시트 onDismiss 에서 값을 다시 읽는 것과 같은 역할).
 * 설정을 직접 읽는 화면은 상단에서 [version] 을 한 번 읽어 구독한다.
 */
object SettingsRefresh {
    var version by mutableIntStateOf(0)
        private set

    fun bump() {
        version++
    }
}

val appColors: AppColors
    @Composable get() = LocalAppColors.current

/** iOS 텍스트 스타일 기본 크기 (pt) */
object AppFont {
    const val LARGE_TITLE = 34f
    const val TITLE = 28f
    const val TITLE2 = 22f
    const val TITLE3 = 20f
    const val HEADLINE = 17f
    const val BODY = 17f
    const val CALLOUT = 16f
    const val SUBHEADLINE = 15f
    const val FOOTNOTE = 13f
    const val CAPTION = 12f
    const val CAPTION2 = 11f
}

/** 시스템 텍스트 스타일 크기 — 글자 크기 설정(iOS dynamicTypeSize) 배율 적용 */
@Composable
fun styleSp(base: Float): TextUnit = (base * LocalTextSize.current.typeScale(LocalIsTablet.current)).sp

/** 고정 크기 폰트 — iOS `size * textSize.scale` 대응 */
@Composable
fun fixedSp(base: Float): TextUnit = (base * LocalTextSize.current.scale(LocalIsTablet.current)).sp

/** 고정 크기 배율 값 자체가 필요할 때 (프레임 크기 등) */
val textScale: Float
    @Composable get() = LocalTextSize.current.scale(LocalIsTablet.current)

fun Color.Companion.argb(value: Long): Color = Color(value)

@Composable
fun PlanWidgetTheme(content: @Composable () -> Unit) {
    val settingsVersion = SettingsRefresh.version
    val systemDark = isSystemInDarkTheme()
    val isDark = AppTheme.isDark(systemDark)
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600

    val colors = remember(isDark, settingsVersion) {
        val textPrimary = if (isDark) Color.White else Color.Black
        AppColors(
            primary = Color(AppTheme.primary(isDark)),
            background = Color(AppTheme.background(isDark)),
            surface = Color(AppTheme.surface(isDark)),
            holidayRed = Color(AppTheme.holidayRed(isDark)),
            accent = Color(AppTheme.accent(isDark)),
            textPrimary = textPrimary,
            textSecondary = textPrimary.copy(alpha = 0.6f),
            textTertiary = textPrimary.copy(alpha = 0.3f),
            isDark = isDark,
        )
    }
    val textSize = remember(settingsVersion, isTablet) { TextSizeSettings.current(isTablet) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    val base = if (isDark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = colors.primary,
        onPrimary = Color.White,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surface,
        onSurfaceVariant = colors.textSecondary,
        surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surface,
        surfaceContainerHighest = colors.surface,
        surfaceContainerLow = colors.surface,
        outline = colors.textPrimary.copy(alpha = 0.12f),
        secondaryContainer = colors.primary.copy(alpha = 0.14f),
        onSecondaryContainer = colors.textPrimary,
    )

    // 시스템 글자 크기 설정은 무시하고 앱 설정(4단계)만 따른다 — iOS 와 같은 동작
    val density = LocalDensity.current
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalAppColors provides colors,
            LocalTextSize provides textSize,
            LocalIsTablet provides isTablet,
            LocalDensity provides Density(density.density, 1f),
            LocalContentColor provides colors.textPrimary,
            content = content,
        )
    }
}
