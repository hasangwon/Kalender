package com.hasangwon.planwidget.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.hasangwon.planwidget.app.ReviewRequester
import com.hasangwon.planwidget.app.ui.theme.AppFont
import com.hasangwon.planwidget.app.ui.theme.SettingsRefresh
import com.hasangwon.planwidget.app.ui.theme.appColors
import com.hasangwon.planwidget.app.ui.theme.styleSp
import com.hasangwon.planwidget.shared.store.LabSettings

/** 정보 화면 — 앱 정보, 실험실 (iOS InfoView 대응) */
@Composable
fun InfoSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = appColors
    var showsTodayLabel by remember { mutableStateOf(LabSettings.showsTodayLabel) }
    val version = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
            "${info.versionName} ($code)"
        }.getOrDefault("-")
    }

    AppSheet(title = "정보", onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AppCard {
                CardTitle("앱 정보")
                InfoRow("앱 이름", "하상원의 달력")
                InfoRow("만든 사람", "하상원")
                InfoRow("버전", version)
                CardDivider()
                // 스토어 리뷰 작성 페이지로 바로 이동
                Row(
                    Modifier.fillMaxWidth().plainClickable { ReviewRequester.openStorePage(context) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Star, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Text("리뷰 남기기", fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.SemiBold, color = colors.primary)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
                }
            }

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CardTitle("실험실")
                    Text("다듬는 중인 기능이에요. 처음에는 모두 꺼져 있어요.", fontSize = styleSp(AppFont.CAPTION), color = colors.textSecondary)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("오늘 표시", fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Spacer(Modifier.weight(1f))
                        AppSwitch(showsTodayLabel) {
                            showsTodayLabel = it
                            LabSettings.showsTodayLabel = it
                            SettingsRefresh.bump()
                        }
                    }
                    Text("달력에서 오늘 날짜 옆에 ‘오늘’을 작게 표시해요.", fontSize = styleSp(AppFont.CAPTION), color = colors.textSecondary)
                }
                CardDivider()
                // 이메일은 눌러서 바로 메일 앱으로
                Text(
                    buildAnnotatedString {
                        append("좋은 아이디어가 있다면 개발자 이메일\n(")
                        withLink(
                            LinkAnnotation.Url("mailto:305243@naver.com", TextLinkStyles(style = SpanStyle(color = colors.primary))),
                        ) { append("305243@naver.com") }
                        append(")로 문의주세요.")
                    },
                    fontSize = styleSp(AppFont.CAPTION),
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    val colors = appColors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = styleSp(AppFont.SUBHEADLINE), color = colors.textSecondary)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
}
