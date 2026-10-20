package com.hasangwon.planwidget.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hasangwon.planwidget.app.appContainer
import com.hasangwon.planwidget.app.ui.theme.AppFont
import com.hasangwon.planwidget.app.ui.theme.appColors
import com.hasangwon.planwidget.app.ui.theme.styleSp
import com.hasangwon.planwidget.shared.KOREAN
import com.hasangwon.planwidget.shared.Lunar
import com.hasangwon.planwidget.shared.model.AnniversaryEntry
import com.hasangwon.planwidget.shared.store.EventColorSettings
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val shortDateFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", KOREAN)

/** 매년 기념일 등록/관리 — 양력 또는 음력(자동 양력 환산)으로 매년 달력·위젯에 표시 (iOS AnniversaryListView 대응) */
@Composable
fun AnniversaryListSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { context.appContainer.repository }
    val colors = appColors
    val scope = rememberCoroutineScope()
    val anniversaries by repository.anniversaries.collectAsStateWithLifecycle(initialValue = emptyList())

    var name by remember { mutableStateOf("") }
    var month by remember { mutableIntStateOf(1) }
    var day by remember { mutableIntStateOf(1) }
    var isLunar by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    val trimmedName = name.trim()
    val currentYear = LocalDate.now().year

    val lunarPreviewText = remember(month, day, isLunar) {
        if (!isLunar) ""
        else Lunar.solarDate(currentYear, month, day)?.let { "올해 양력 ${it.format(shortDateFormatter)}" } ?: "올해는 해당 음력 날짜가 없어요"
    }

    AppSheet(title = "매년 기념일 등록", onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 입력 카드
            AppCard(spacing = 16) {
                CardTitle("새 기념일")

                val style = TextStyle(fontSize = styleSp(AppFont.BODY), color = colors.textPrimary)
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = style,
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier.fillMaxWidth().background(colors.primaryFill(0.05f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        Box {
                            if (name.isEmpty()) Text("이름 (예: 엄마 생일, 결혼기념일)", style = style.copy(color = colors.textTertiary))
                            inner()
                        }
                    },
                )

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("음력 날짜로 입력", fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        CardCaption("음력 생일 등 — 매년 양력으로 환산해 표시돼요")
                    }
                    AppSwitch(isLunar) {
                        isLunar = it
                        if (it && day > 30) day = 30
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(if (isLunar) "음력" else "양력", fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    PickerChip(value = "${month}월", options = (1..12).map { "${it}월" }) { month = it + 1 }
                    PickerChip(value = "${day}일", options = (1..(if (isLunar) 30 else 31)).map { "${it}일" }) { day = it + 1 }
                }

                if (isLunar) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(12.dp))
                        Text(lunarPreviewText, fontSize = styleSp(AppFont.CAPTION), fontWeight = FontWeight.SemiBold, color = colors.primary)
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (trimmedName.isEmpty()) colors.textSecondary.copy(alpha = 0.35f) else colors.primary, RoundedCornerShape(14.dp))
                        .plainClickable(enabled = trimmedName.isNotEmpty()) {
                            val entry = AnniversaryEntry(name = trimmedName, month = month, day = day, isLunar = isLunar)
                            scope.launch { repository.saveAnniversary(entry) }
                            name = ""
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    HorizontalSpace(6)
                    Text("기념일 추가", fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.Bold, color = Color.White)
                }

                CardCaption("등록한 기념일은 매년 달력과 위젯에 자동 표시됩니다. 음력 윤달은 평달 기준으로 표시됩니다.")
            }

            // 등록된 기념일
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.padding(horizontal = 4.dp)) { CardTitle("등록된 기념일") }

                if (anniversaries.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(16.dp)).padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("등록된 기념일이 없습니다", fontSize = styleSp(AppFont.SUBHEADLINE), color = colors.textSecondary)
                    }
                } else {
                    for (anniversary in anniversaries) {
                        AnniversaryCard(
                            anniversary = anniversary,
                            isExpanded = expandedId == anniversary.id,
                            thisYearText = anniversary.solarDate(currentYear)?.format(shortDateFormatter) ?: "올해 없음",
                            onToggle = { expandedId = if (expandedId == anniversary.id) null else anniversary.id },
                            onDelete = {
                                if (expandedId == anniversary.id) expandedId = null
                                scope.launch { repository.deleteAnniversary(anniversary) }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 월/일 선택 칩 — 누르면 목록 메뉴 (iOS .menu 피커 대응) */
@Composable
private fun PickerChip(value: String, options: List<String>, onSelect: (Int) -> Unit) {
    val colors = appColors
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .background(colors.primaryFill(0.05f), RoundedCornerShape(10.dp))
                .plainClickable { open = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, fontSize = styleSp(AppFont.BODY), color = colors.textPrimary)
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = colors.surface, modifier = Modifier.heightIn(max = 320.dp)) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(text = { Text(label, color = colors.textPrimary) }, onClick = { onSelect(index); open = false })
            }
        }
    }
}

@Composable
private fun AnniversaryCard(
    anniversary: AnniversaryEntry,
    isExpanded: Boolean,
    thisYearText: String,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = appColors
    Column(Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(16.dp))) {
        Row(
            Modifier.fillMaxWidth().plainClickable(onClick = onToggle).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(8.dp).background(EventColorSettings.anniversaryColor.composeColor, CircleShape))
            Text(anniversary.name, fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.Bold, color = colors.textPrimary)
            if (anniversary.isLunar) {
                Text(
                    "음력",
                    Modifier.background(colors.accent.copy(alpha = 0.12f), CircleShape).padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = styleSp(AppFont.CAPTION2),
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(anniversary.dateText, fontSize = styleSp(AppFont.CAPTION), color = colors.textSecondary)
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(16.dp).rotate(if (isExpanded) 180f else 0f),
            )
        }

        AnimatedVisibility(isExpanded) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CardDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("올해 표시일", fontSize = styleSp(AppFont.CAPTION), fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Text(thisYearText, fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.Bold, color = colors.primary)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE53935).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .plainClickable(onClick = onDelete)
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                    HorizontalSpace(6)
                    Text("삭제", fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                }
            }
        }
    }
}
