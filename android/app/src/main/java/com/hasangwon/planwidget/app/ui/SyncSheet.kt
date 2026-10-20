package com.hasangwon.planwidget.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hasangwon.planwidget.app.appContainer
import com.hasangwon.planwidget.app.ui.theme.AppFont
import com.hasangwon.planwidget.app.ui.theme.appColors
import com.hasangwon.planwidget.app.ui.theme.styleSp
import com.hasangwon.planwidget.shared.store.SyncSettings
import com.hasangwon.planwidget.widget.WidgetRefresh
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 동기화 설정 화면 — 안드로이드 달력 연동 on/off (iOS SyncView 대응. iCloud 항목은 안드로이드에 없다) */
@Composable
fun SyncSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { context.appContainer.deviceCalendar }
    val colors = appColors
    val scope = rememberCoroutineScope()
    val isAuthorized by manager.isAuthorized.collectAsStateWithLifecycle()
    var enabled by remember { mutableStateOf(SyncSettings.deviceCalendarEnabled) }

    fun refreshWidget() = scope.launch { WidgetRefresh.requestUpdate(context) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        manager.refreshAuthorization()
        if (granted) {
            manager.loadEvents(LocalDate.now())
        } else {
            // 권한이 거부되면 토글을 되돌림
            enabled = false
            SyncSettings.deviceCalendarEnabled = false
        }
        refreshWidget()
    }

    AppSheet(title = "동기화", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            AppCard(spacing = 12) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.DateRange, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                    Text("달력 앱과 동기화", fontSize = styleSp(AppFont.SUBHEADLINE), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Spacer(Modifier.weight(1f))
                    AppSwitch(enabled) { isOn ->
                        enabled = isOn
                        SyncSettings.deviceCalendarEnabled = isOn
                        if (isOn) {
                            if (isAuthorized) manager.loadEvents(LocalDate.now())
                            else permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        } else {
                            manager.clear()
                        }
                        refreshWidget()
                    }
                }

                Text(
                    "안드로이드 기본 달력 앱의 일정을 이 앱에서 함께 볼 수 있어요. (달력 앱에 일정을 넣는 것은 불가능합니다)",
                    fontSize = styleSp(AppFont.CAPTION),
                    color = colors.textSecondary,
                )

                if (enabled && !isAuthorized) {
                    Text(
                        "달력 접근 권한이 필요합니다. 휴대전화 설정 → 앱 → 하상원 달력 → 권한에서 허용해 주세요.",
                        Modifier.plainClickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                                )
                            }
                        },
                        fontSize = styleSp(AppFont.CAPTION2),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE53935),
                    )
                }
            }
        }
    }
}
