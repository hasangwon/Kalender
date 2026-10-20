package com.hasangwon.planwidget.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hasangwon.planwidget.app.ui.theme.AppFont
import com.hasangwon.planwidget.app.ui.theme.appColors
import com.hasangwon.planwidget.app.ui.theme.styleSp
import kotlinx.coroutines.delay

// 여러 화면이 같은 모양으로 쓰는 공용 UI 조각 (iOS CommonComponents 대응)

/** 설정·동기화·정보 화면 등의 흰 카드 컨테이너 (iOS .cardStyle()) */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    spacing: Int = 14,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = appColors
    Column(
        modifier
            .fillMaxWidth()
            .shadow(if (colors.isDark) 0.dp else 3.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.35f))
            .background(colors.surface, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content,
    )
}

/** 카드 제목 (iOS .cardTitleFont()) */
@Composable
fun CardTitle(text: String) {
    Text(text, fontSize = styleSp(AppFont.HEADLINE), fontWeight = FontWeight.Bold, color = appColors.textPrimary)
}

/** 카드 안의 소제목 (iOS .subheadline.weight(.bold) + .secondary) */
@Composable
fun CardSubtitle(text: String) {
    Text(text, fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.Bold, color = appColors.textSecondary)
}

/** 카드 안의 설명 문장 (iOS .caption2 + .secondary) */
@Composable
fun CardCaption(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = styleSp(AppFont.CAPTION2), color = appColors.textSecondary)
}

@Composable
fun CardDivider() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(appColors.textPrimary.copy(alpha = 0.12f)))
}

/** 눌림 표시 없는 클릭 (iOS .buttonStyle(.plain)) */
@Composable
fun Modifier.plainClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )

/** 시트 우상단 닫기(x) 버튼 */
@Composable
fun SheetCloseButton(onClick: () -> Unit) {
    val colors = appColors
    Box(
        Modifier
            .size(30.dp)
            .background(colors.primaryFill(0.05f), CircleShape)
            .plainClickable(onClick = onClick)
            .semantics { contentDescription = "닫기" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Close, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
    }
}

/** 시트 우상단 글자 버튼 ("완료", "저장" 등) */
@Composable
fun SheetTextButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = appColors
    Text(
        text,
        Modifier
            .plainClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        fontSize = styleSp(AppFont.SUBHEADLINE),
        fontWeight = FontWeight.Bold,
        color = if (enabled) colors.primary else colors.textTertiary,
    )
}

/** 색 선택용 동그라미 버튼 (선택 시 흰 체크) */
@Composable
fun ColorSwatchButton(color: Color, isSelected: Boolean, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .background(color, CircleShape)
            .plainClickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

/** 가로로 균등 분할된 선택 버튼 줄 (글자 크기·넘기기 방향 공용) */
@Composable
fun <T> OptionSelector(
    options: List<T>,
    selection: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val colors = appColors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (option in options) {
            val selected = option == selection
            Box(
                Modifier
                    .weight(1f)
                    .background(if (selected) colors.primary else colors.primaryFill(0.06f), RoundedCornerShape(11.dp))
                    .plainClickable { onSelect(option) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    fontSize = styleSp(AppFont.CAPTION),
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 테마색 스위치 (iOS Toggle.tint(AppTheme.primary)) */
@Composable
fun AppSwitch(checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    val colors = appColors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = colors.primary,
            checkedThumbColor = Color.White,
            uncheckedTrackColor = colors.primaryFill(0.12f),
            uncheckedBorderColor = Color.Transparent,
            uncheckedThumbColor = colors.surface,
        ),
    )
}

/**
 * iOS `.sheet` 대응 — 아래에서 올라오는 전체 높이 시트. 상단에 제목과 좌우 버튼.
 * 뒤로가기·바깥 탭으로 닫힌다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    title: String,
    onDismiss: () -> Unit,
    fullHeight: Boolean = true,
    dismissible: Boolean = true,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = { SheetCloseButton(onClick = onDismiss) },
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = appColors
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { dismissible },
    )
    ModalBottomSheet(
        onDismissRequest = { if (dismissible) onDismiss() },
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.textPrimary,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(if (fullHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp)) {
                Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) { leading() }
                Text(
                    title,
                    Modifier.align(Alignment.Center),
                    fontSize = styleSp(AppFont.HEADLINE),
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) { trailing() }
            }
            content()
        }
    }
}

/** 하단에서 잠깐 떴다 사라지는 토스트. 연속 호출 시 쌓이지 않고 메시지만 갱신된다 */
class ToastState {
    var message by mutableStateOf<String?>(null)
        private set

    /** 같은 문구가 연달아 와도 타이머가 다시 시작되도록 토큰을 바꾼다 */
    var token by mutableStateOf(0)
        private set

    fun show(text: String) {
        message = text
        token++
    }

    fun clear() {
        message = null
    }
}

@Composable
fun BoxScope.ToastOverlay(state: ToastState, bottomPadding: Int = 40) {
    val message = state.message
    LaunchedEffect(state.token, message) {
        if (message == null) return@LaunchedEffect
        delay(2000)
        state.clear()
    }
    AnimatedVisibility(
        visible = message != null,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding.dp),
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(),
    ) {
        var shown by remember { mutableStateOf(message ?: "") }
        if (message != null) shown = message
        Text(
            shown,
            Modifier
                .background(Color.Black.copy(alpha = 0.82f), CircleShape)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            color = Color.White,
            fontSize = styleSp(AppFont.SUBHEADLINE),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun VerticalSpace(height: Int) = Spacer(Modifier.height(height.dp))

@Composable
fun HorizontalSpace(width: Int) = Spacer(Modifier.width(width.dp))
