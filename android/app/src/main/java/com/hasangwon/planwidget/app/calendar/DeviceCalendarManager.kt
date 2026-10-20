package com.hasangwon.planwidget.app.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.hasangwon.planwidget.shared.startOfMonth
import com.hasangwon.planwidget.shared.store.DeviceCalendarEvent
import com.hasangwon.planwidget.shared.store.SyncSettings
import com.hasangwon.planwidget.shared.toEpochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 안드로이드 기본 달력 연동 (읽기 전용). iOS AppleCalendarManager 대응.
 * 기기에 등록된 달력의 일정을 우리 앱에 표시만 한다. 쓰기/수정 없음.
 */
class DeviceCalendarManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isAuthorized = MutableStateFlow(hasPermission())
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    /** 표시용 이벤트 (월 단위로 미리 로드) */
    private val _events = MutableStateFlow<List<DeviceCalendarEvent>>(emptyList())
    val events: StateFlow<List<DeviceCalendarEvent>> = _events.asStateFlow()

    /** 마지막으로 불러온 기간 — 그 안으로 이동하면 다시 조회하지 않는다 */
    private var loadedRange: LongRange? = null
    /** 지금 화면이 원하는 기간 — 외부 변경 시 이 기간을 다시 조회한다 */
    private var requestedRange: LongRange? = null
    /** 빠른 월 이동 시 늦게 끝난 이전 조회가 현재 결과를 덮지 않도록 요청마다 번호를 붙인다 */
    private var requestId = 0
    private var observerRegistered = false

    // 달력 앱 등에서 일정이 바뀌면 캐시를 버리고 다시 불러온다
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            val range = requestedRange ?: return
            loadedRange = null
            fetch(range)
        }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** 현재 권한 상태 갱신 (권한 요청 결과를 받은 뒤, 앱 재진입 시) */
    fun refreshAuthorization() {
        _isAuthorized.value = hasPermission()
        if (_isAuthorized.value && !observerRegistered) {
            context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
            observerRegistered = true
        }
    }

    /** 로드한 이벤트 비우기 (동기화 해제 시) */
    fun clear() {
        requestId++
        loadedRange = null
        requestedRange = null
        _events.value = emptyList()
    }

    /** 지정한 달(기준일이 속한 달) 앞뒤로 이벤트 로드 */
    fun loadEvents(around: LocalDate, zone: ZoneId = ZoneId.systemDefault()) {
        if (!SyncSettings.deviceCalendarEnabled || !_isAuthorized.value) {
            clear()
            return
        }

        // 달력이 앞뒤 달 페이지를 미리 깔아 두므로 이전 달 ~ 다음 달 전체, 그리고 그 그리드에 흐리게 보이는 앞뒤 1주까지
        val monthStart = around.startOfMonth()
        val rangeStart = monthStart.minusMonths(1).minusDays(7).toEpochMillis(zone)
        val rangeEnd = monthStart.plusMonths(2).plusDays(7).toEpochMillis(zone)
        val range = rangeStart..rangeEnd

        requestedRange = range
        val loaded = loadedRange
        if (loaded != null && rangeStart in loaded && rangeEnd in loaded) {
            // 이미 있는 결과를 쓰더라도, 그사이 다른 달로 나간 조회가 늦게 끝나 덮어쓰지 않게 무효화한다
            requestId++
            return
        }
        fetch(range)
    }

    /** ContentResolver 조회는 IO 에서 하고, 표시용 값만 메인으로 가져온다 */
    private fun fetch(range: LongRange) {
        requestId++
        val id = requestId
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { query(range) }.getOrDefault(emptyList()) }
            // 그사이 더 새 요청이 나갔으면 버린다
            if (id != requestId) return@launch
            _events.value = result
            loadedRange = range
        }
    }

    private fun query(range: LongRange): List<DeviceCalendarEvent> {
        val resolver = context.contentResolver

        // 사용자가 직접 쓰는 달력만 — 읽기 전용(공휴일·구독·연락처 생일) 달력은 우리 공휴일과 중복되므로 제외
        val calendarIds = mutableListOf<Long>()
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) calendarIds += cursor.getLong(0)
        }
        if (calendarIds.isEmpty()) return emptyList()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(range.first.toString())
            .appendPath(range.last.toString())
            .build()
        val projection = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY,
        )
        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${calendarIds.joinToString(",")})"
        val zone = ZoneId.systemDefault()

        val events = mutableListOf<DeviceCalendarEvent>()
        resolver.query(uri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
            while (cursor.moveToNext()) {
                val instanceId = cursor.getLong(0)
                val eventId = cursor.getLong(1)
                val title = cursor.getString(2)?.takeIf { it.isNotBlank() } ?: "(제목 없음)"
                val begin = cursor.getLong(3)
                val isAllDay = cursor.getInt(4) == 1
                // 종일 일정의 BEGIN 은 UTC 0시 — 기기 시간대의 그날 0시로 바꿔 둔다
                val startMillis = if (isAllDay) {
                    Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate().toEpochMillis(zone)
                } else {
                    begin
                }
                events += DeviceCalendarEvent(id = "$eventId-$instanceId", title = title, startMillis = startMillis, isAllDay = isAllDay)
            }
        }
        return events
    }
}
