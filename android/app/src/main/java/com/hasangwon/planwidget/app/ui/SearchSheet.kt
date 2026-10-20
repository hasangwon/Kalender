package com.hasangwon.planwidget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hasangwon.planwidget.app.appContainer
import com.hasangwon.planwidget.app.ui.theme.AppFont
import com.hasangwon.planwidget.app.ui.theme.appColors
import com.hasangwon.planwidget.app.ui.theme.styleSp
import com.hasangwon.planwidget.shared.KOREAN
import com.hasangwon.planwidget.shared.model.Recurrence
import com.hasangwon.planwidget.shared.model.Schedule
import com.hasangwon.planwidget.shared.toLocalDate
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val resultDateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일", KOREAN)
private const val PAGE_SIZE = 20

/**
 * 일정 검색 — 제목/메모 텍스트 매칭, 결과 탭 시 해당 날짜로 이동 (iOS SearchView 대응).
 * 반복 일정은 첫 발생일(startDate)로 이동.
 */
@Composable
fun SearchSheet(onDismiss: () -> Unit, onSelect: (LocalDate) -> Unit) {
    val context = LocalContext.current
    val repository = remember { context.appContainer.repository }
    val colors = appColors
    val schedules by repository.schedules.collectAsStateWithLifecycle(initialValue = emptyList())

    var query by remember { mutableStateOf("") }
    val trimmedQuery = query.trim()
    /** 무한 스크롤: 처음엔 일부만, 스크롤 끝에서 더 로드 */
    var visibleCount by remember { mutableIntStateOf(PAGE_SIZE) }
    var results by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    /** results 가 어떤 검색어로 계산된 것인지 — 입력 직후 아직 계산 전이면 다르다 */
    var searchedQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    fun search(keyword: String, source: List<Schedule>): List<Schedule> {
        if (keyword.isEmpty()) return emptyList()
        val lowered = keyword.lowercase()
        return source
            .filter { it.title.lowercase().contains(lowered) || it.memo.lowercase().contains(lowered) }
            .sortedByDescending { it.startDate }
    }

    // 입력이 잠깐 멈췄을 때(250ms)·일정이 바뀌었을 때만 계산하고, 무한 스크롤은 이 값을 재사용한다
    LaunchedEffect(trimmedQuery, schedules) {
        if (trimmedQuery.isNotEmpty() && trimmedQuery != searchedQuery) delay(250)
        results = search(trimmedQuery, schedules)
        searchedQuery = trimmedQuery
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AppSheet(title = "검색", onDismiss = onDismiss, trailing = { SheetTextButton("완료", onClick = onDismiss) }) {
        Column(Modifier.fillMaxSize().imePadding()) {
            // 검색 필드
            val style = TextStyle(fontSize = styleSp(AppFont.BODY), color = colors.textPrimary)
            Row(
                Modifier.fillMaxWidth().padding(16.dp).background(colors.surface, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it; visibleCount = PAGE_SIZE },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = style,
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) Text("일정 제목·메모 검색", style = style.copy(color = colors.textTertiary))
                            inner()
                        }
                    },
                )
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Outlined.Clear,
                        contentDescription = "지우기",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp).plainClickable { query = "" },
                    )
                }
            }

            when {
                trimmedQuery.isEmpty() -> EmptyState("일정 검색", "제목이나 메모로 일정을 찾아보세요")
                results.isNotEmpty() -> ResultList(results, visibleCount, onMore = { visibleCount += PAGE_SIZE }, onSelect = onSelect)
                searchedQuery == trimmedQuery -> EmptyState("'$trimmedQuery'에 대한 결과 없음", "다른 검색어를 입력해 보세요.")
                else -> Spacer(Modifier.weight(1f)) // 입력 직후 검색 대기 중
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, description: String) {
    val colors = appColors
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = styleSp(AppFont.TITLE3), fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(description, fontSize = styleSp(AppFont.SUBHEADLINE), color = colors.textSecondary)
    }
}

@Composable
private fun ResultList(results: List<Schedule>, visibleCount: Int, onMore: () -> Unit, onSelect: (LocalDate) -> Unit) {
    val listState = rememberLazyListState()
    val visibleResults = results.take(visibleCount)
    val hasMore = visibleCount < results.size
    // 무한 스크롤: 마지막 항목이 보이면 다음 페이지 로드
    val reachedEnd by remember(visibleResults.size) {
        derivedStateOf { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) >= visibleResults.size - 1 }
    }
    LaunchedEffect(reachedEnd, hasMore) { if (reachedEnd && hasMore) onMore() }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(visibleResults, key = { it.id }) { schedule -> ResultCard(schedule) { onSelect(schedule.startDate.toLocalDate()) } }
        if (hasMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = appColors.primary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultCard(schedule: Schedule, onClick: () -> Unit) {
    val colors = appColors
    val tint = schedule.displayColor.composeColor
    Row(
        Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(16.dp)).plainClickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(4.dp).height(40.dp).background(tint, RoundedCornerShape(2.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(schedule.title, fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (schedule.memo.isNotEmpty()) {
                Text(schedule.memo, fontSize = styleSp(AppFont.CAPTION), color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                schedule.recurrenceBadgeText?.let {
                    Text(it, fontSize = styleSp(AppFont.CAPTION2), fontWeight = FontWeight.Bold, color = tint)
                }
                // 반복 일정은 "첫 일정 날짜" 안내
                val base = schedule.startDate.toLocalDate().format(resultDateFormatter)
                Text(if (schedule.recurrence == Recurrence.NONE) base else "${base}부터", fontSize = styleSp(AppFont.CAPTION2), color = colors.textSecondary)
            }
        }
        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
    }
}
