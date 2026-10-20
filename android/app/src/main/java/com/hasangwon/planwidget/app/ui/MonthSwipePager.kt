package com.hasangwon.planwidget.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.hasangwon.planwidget.shared.store.MonthSwipeDirection
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

enum class PagerAxis { HORIZONTAL, VERTICAL }

/**
 * 월 넘기기 상태 (iOS MonthSwipePager + MonthPagerGate 대응).
 * 앞뒤 달 3장을 항상 깔아 두고, 드래그 중엔 위치만 옮긴다 — 날짜 칸을 새로 만들거나 다시 계산하지 않는다.
 */
class MonthPagerState(initialMonth: LocalDate) {
    var displayedMonth by mutableStateOf(initialMonth.withDayOfMonth(1))
        private set

    /** 손가락을 따라 밀린 거리 (잠긴 축 기준, px) */
    val offset = Animatable(0f)

    /** 드래그 시작 시 잠근 축 — 넘기기 애니메이션이 끝날 때까지 유지. 이웃 달은 이때만 보인다 */
    var dragAxis by mutableStateOf<PagerAxis?>(null)
        internal set

    /** 놓은 뒤 넘기기/복귀 애니메이션 중 — 새 드래그·버튼 이동을 받지 않는다 */
    var isPaging by mutableStateOf(false)
        internal set

    /** 월 선택·검색처럼 멀리 이동할 때 이웃 슬롯에 대신 보여줄 달 */
    var pendingTarget by mutableStateOf<LocalDate?>(null)
        private set

    var sizePx by mutableStateOf(IntSize.Zero)
        internal set

    val isBusy: Boolean get() = dragAxis != null || isPaging

    /** 슬롯(-1/0/1)에 그릴 달 */
    fun monthAt(index: Int): LocalDate {
        val target = pendingTarget
        if (target != null && index != 0 && index == targetSign(target)) return target
        return displayedMonth.plusMonths(index.toLong())
    }

    fun pageLength(axis: PagerAxis, gapPx: Float): Float =
        (if (axis == PagerAxis.HORIZONTAL) sizePx.width else sizePx.height) + gapPx

    private fun targetSign(target: LocalDate): Int = if (target > displayedMonth) 1 else -1

    /**
     * 버튼·월 선택·검색으로 이동. 이웃 슬롯에 목표 달을 깔고 한 페이지 밀어서 보여준다 —
     * 인접 달이든 몇 년 뒤든 같은 움직임이다. 이동 중이면 무시 (화살표 연타로 애니메이션이 겹치지 않게).
     */
    suspend fun animateTo(target: LocalDate, axis: PagerAxis, gapPx: Float, durationMillis: Int = 280) {
        val normalized = target.withDayOfMonth(1)
        if (isBusy || normalized == displayedMonth) return

        val sign = targetSign(normalized)
        pendingTarget = normalized
        dragAxis = axis
        isPaging = true
        try {
            offset.animateTo(-sign * pageLength(axis, gapPx), tween(durationMillis))
        } finally {
            // 목표 달이 이미 제자리에 와 있으므로 기준만 바꾸고 오프셋을 0 으로 — 같은 프레임에 반영돼 튀지 않는다
            displayedMonth = normalized
            offset.snapTo(0f)
            pendingTarget = null
            dragAxis = null
            isPaging = false
        }
    }

    /** 드래그를 놓은 뒤 — step(-1/0/1) 만큼 마저 밀고 기준 월을 확정한다 */
    internal suspend fun finishDrag(step: Int, axis: PagerAxis, gapPx: Float) {
        isPaging = true
        try {
            offset.animateTo(-step * pageLength(axis, gapPx), tween(220, easing = LinearOutSlowInEasing))
        } finally {
            if (step != 0) displayedMonth = displayedMonth.plusMonths(step.toLong())
            offset.snapTo(0f)
            dragAxis = null
            isPaging = false
        }
    }
}

/**
 * 설정한 방향으로 끌면 달력이 손가락을 따라 움직이고, 놓을 때 넘길지 정한다.
 * 왼쭉/위로 밀면 다음 달, 오른쪽/아래로 밀면 이전 달.
 * 페이지 내용은 부모가 만든 값을 그대로 쓰므로 드래그 중 날짜 칸을 다시 계산하지 않는다.
 */
@Composable
fun MonthSwipePager(
    state: MonthPagerState,
    swipeDirection: MonthSwipeDirection,
    gapPx: Float,
    onPagingFinished: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    page: @Composable (month: LocalDate) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val layoutAxis = state.dragAxis
        ?: if (swipeDirection == MonthSwipeDirection.VERTICAL) PagerAxis.VERTICAL else PagerAxis.HORIZONTAL

    Box(
        modifier
            .onSizeChanged { state.sizePx = it }
            .clipToBounds()
            .pointerInput(swipeDirection, state) {
                detectDragGestures(
                    onDragStart = { },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (state.isPaging) return@detectDragGestures

                        var axis = state.dragAxis
                        if (axis == null) {
                            // 처음 움직인 방향으로 축을 잠근다. 설정에서 막힌 방향이면 이번 드래그는 무시
                            val horizontal = abs(dragAmount.x) > abs(dragAmount.y)
                            axis = when {
                                horizontal && swipeDirection.allowsHorizontal -> PagerAxis.HORIZONTAL
                                !horizontal && swipeDirection.allowsVertical -> PagerAxis.VERTICAL
                                else -> return@detectDragGestures
                            }
                            state.dragAxis = axis
                            tracker.resetTracking()
                        }
                        tracker.addPosition(change.uptimeMillis, change.position)

                        val pageLength = state.pageLength(axis, gapPx)
                        val delta = if (axis == PagerAxis.HORIZONTAL) dragAmount.x else dragAmount.y
                        // 한 페이지 이상은 밀리지 않게
                        val next = (state.offset.value + delta).coerceIn(-pageLength, pageLength)
                        scope.launch { state.offset.snapTo(next) }
                    },
                    onDragEnd = {
                        val axis = state.dragAxis ?: return@detectDragGestures
                        if (state.isPaging) return@detectDragGestures

                        val pageLength = state.pageLength(axis, gapPx)
                        val velocity = tracker.calculateVelocity()
                        val speed = if (axis == PagerAxis.HORIZONTAL) velocity.x else velocity.y
                        // 놓은 속도로 0.25초 더 간다고 보고 예측 거리를 잡는다 (iOS predictedEndTranslation 대응)
                        val predicted = state.offset.value + speed * 0.25f
                        val offset = state.offset.value

                        val step = when {
                            offset < 0 && (-offset > pageLength * 0.25f || -predicted > pageLength * 0.5f) -> 1
                            offset > 0 && (offset > pageLength * 0.25f || predicted > pageLength * 0.5f) -> -1
                            else -> 0
                        }
                        scope.launch {
                            state.finishDrag(step, axis, gapPx)
                            if (step != 0) onPagingFinished(state.displayedMonth)
                        }
                    },
                    onDragCancel = {
                        // 전화·알림 센터 등으로 제스처가 취소되면 밀린 상태를 되돌린다
                        val axis = state.dragAxis ?: return@detectDragGestures
                        if (!state.isPaging) scope.launch { state.finishDrag(0, axis, gapPx) }
                    },
                )
            },
    ) {
        for (index in -1..1) {
            val month = state.monthAt(index)
            key(month) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val axis = layoutAxis
                            val base = index * state.pageLength(axis, gapPx)
                            val shift = state.offset.value
                            translationX = if (axis == PagerAxis.HORIZONTAL) base + shift else 0f
                            translationY = if (axis == PagerAxis.VERTICAL) base + shift else 0f
                            // 드래그 중이 아니면 이웃 달은 그리지 않는다 (레이아웃은 유지되므로 드래그 시작 때 다시 만들지 않는다)
                            alpha = if (index == 0 || state.dragAxis != null) 1f else 0f
                        },
                ) {
                    page(month)
                }
            }
        }
    }
}

private val tracker = VelocityTracker()
