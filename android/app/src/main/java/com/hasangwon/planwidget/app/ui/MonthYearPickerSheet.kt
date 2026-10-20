package com.hasangwon.planwidget.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hasangwon.planwidget.app.ui.theme.AppFont
import com.hasangwon.planwidget.app.ui.theme.appColors
import com.hasangwon.planwidget.app.ui.theme.styleSp
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import kotlin.math.abs

/** 연·월 빠른 이동 피커 (iOS MonthYearPickerView). 타이틀 탭 시 뜨며, 원하는 연도·월로 한 번에 점프 */
@Composable
fun MonthYearPickerSheet(
    displayedMonth: LocalDate,
    onDismiss: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = appColors
    val currentYear = LocalDate.now().year
    // 과거 10년 ~ 미래 10년
    val years = remember(currentYear) { (currentYear - 10..currentYear + 10).toList() }
    var year by remember { mutableIntStateOf(displayedMonth.year) }
    var month by remember { mutableIntStateOf(displayedMonth.monthValue) }

    AppSheet(title = "연·월 이동", onDismiss = onDismiss, fullHeight = false) {
        Column(
            Modifier.fillMaxWidth().padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                WheelPicker(
                    items = years,
                    selectedIndex = years.indexOf(year).coerceAtLeast(0),
                    label = { "${it}년" },
                    onSelected = { year = years[it] },
                    modifier = Modifier.weight(1f),
                )
                WheelPicker(
                    items = (1..12).toList(),
                    selectedIndex = month - 1,
                    label = { "${it}월" },
                    onSelected = { month = it + 1 },
                    modifier = Modifier.weight(1f),
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(colors.primary, RoundedCornerShape(14.dp))
                    .plainClickable { onSelect(LocalDate.of(year, month, 1)) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("이동", fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

/** 가운데에 스냅되는 세로 휠 (iOS .wheel 피커 대응) */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    selectedIndex: Int,
    label: (T) -> String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    visibleCount: Int = 5,
) {
    val colors = appColors
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val centerIndex by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            (first + if (offset > itemHeightPx / 2) 1 else 0).coerceIn(0, items.lastIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { centerIndex }.distinctUntilChanged().collect { onSelected(it) }
    }

    val padding = itemHeight * (visibleCount / 2)
    Box(modifier.height(itemHeight * visibleCount)) {
        // 가운데 선택 띠
        Box(
            Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center)
                .padding(horizontal = 12.dp)
                .background(colors.primaryFill(0.06f), RoundedCornerShape(10.dp)),
        )
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(vertical = padding),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(items.size) { index ->
                val distance = abs(index - centerIndex)
                Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                    Text(
                        label(items[index]),
                        fontSize = styleSp(if (distance == 0) AppFont.TITLE3 else AppFont.BODY),
                        fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(
                            when (distance) {
                                0 -> 1f
                                1 -> 0.55f
                                else -> 0.3f
                            },
                        ),
                    )
                }
            }
        }
    }
}
