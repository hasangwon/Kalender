package com.hasangwon.planwidget.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit

/**
 * 한 줄에 들어가는 글자까지만 보여주고 말줄임(…) 없이 글자 단위로 끊는 텍스트 (iOS CharClippedText 대응).
 * 글자를 줄바꿈 없이 한 줄로 재고, 칸에 들어가면 가운데 정렬·넘치면 앞에서부터 보이는 만큼만 남기고 자른다.
 * 서브컴포지션 없이 레이아웃 단계에서만 처리하므로 달력 칸 수백 개에 써도 가볍다.
 */
@Composable
fun CharClippedText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            Text(
                text = text,
                color = color,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                // 글꼴 여백 없이 한 줄 높이를 꼭 맞춘다 — 칸 높이를 일정 2줄 + N 에 쓰기 위해 (iOS 와 같은 밀도)
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    lineHeight = fontSize * 1.25f,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                ),
            )
        },
        modifier = modifier
            .clipToBounds()
            .semantics { contentDescription = text },
    ) { measurables, constraints ->
        // 폭 제한 없이 재서 글자의 실제 폭을 얻는다
        val placeable = measurables.first().measure(Constraints(maxHeight = constraints.maxHeight))
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
        val height = placeable.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            val x = if (placeable.width <= width) (width - placeable.width) / 2 else 0
            placeable.placeRelative(x, 0)
        }
    }
}
