package com.hasangwon.planwidget.app.backup

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.hasangwon.planwidget.app.ScheduleRepository
import com.hasangwon.planwidget.shared.KOREAN
import com.hasangwon.planwidget.shared.model.AnniversaryEntry
import com.hasangwon.planwidget.shared.model.Recurrence
import com.hasangwon.planwidget.shared.model.Schedule
import com.hasangwon.planwidget.shared.toEpochMillis
import com.hasangwon.planwidget.shared.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

/**
 * 백업 내보내기/가져오기 (iOS BackupService 대응). 형식은 common/spec/backup-format.md.
 * 가져올 때 파일 이름 확장자로 타입 1(.hscal)과 타입 2(.redcalendar)를 구분하고, 확장자가 없으면 내용으로 판별한다.
 */
object BackupService {
    data class ImportResult(val added: Int, val skipped: Int)

    class BackupException(message: String) : Exception(message) {
        companion object {
            val unsupportedFile get() = BackupException("지원하지 않는 파일이에요")
            val unreadableFile get() = BackupException("파일을 읽을 수 없어요")
        }
    }

    /** 저장 대화상자에 제안할 파일 이름 (확장자 포함) */
    val defaultExportName: String
        get() = "hscal_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd", KOREAN)) + ".hscal"

    /** SAF 저장 시 .hscal 확장자가 유지되도록 쓰는 MIME — application/json 을 주면 .json 이 덧붙는다 */
    const val EXPORT_MIME = "application/octet-stream"

    // MARK: - 내보내기

    suspend fun exportData(repository: ScheduleRepository): ByteArray = withContext(Dispatchers.IO) {
        val file = BackupFile(
            anniversaries = repository.allAnniversaries().map(BackupFile.AnniversaryRecord::from),
            exportedAt = SwiftDate.fromEpochMillis(System.currentTimeMillis()),
            schedules = repository.allSchedules().map(BackupFile.ScheduleRecord::from),
            version = BackupFile.CURRENT_VERSION,
        )
        BackupFile.json.encodeToString(BackupFile.serializer(), file).toByteArray(Charsets.UTF_8)
    }

    // MARK: - 가져오기

    suspend fun importFile(context: Context, uri: Uri, repository: ScheduleRepository): ImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val data = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            ?: throw BackupException.unreadableFile

        val extension = displayName(context, uri)?.substringAfterLast('.', "")?.lowercase()
        val kind = when {
            extension == "redcalendar" -> Kind.TYPE2
            extension == "hscal" -> Kind.TYPE1
            looksLikeRealm(data) -> Kind.TYPE2
            looksLikeJson(data) -> Kind.TYPE1
            else -> throw BackupException.unsupportedFile
        }
        when (kind) {
            Kind.TYPE1 -> importType1(data, repository)
            Kind.TYPE2 -> importType2(data, repository)
        }
    }

    private enum class Kind { TYPE1, TYPE2 }

    private fun displayName(context: Context, uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment

    private fun looksLikeRealm(data: ByteArray): Boolean =
        data.size >= 24 && data.copyOfRange(16, 20).contentEquals("T-DB".toByteArray())

    private fun looksLikeJson(data: ByteArray): Boolean =
        data.asSequence().map { it.toInt().toChar() }.firstOrNull { !it.isWhitespace() } == '{'

    /** 타입 1 — id 가 이미 있는 항목은 건너뛴다 (같은 백업을 두 번 가져와도 중복 없음) */
    private suspend fun importType1(data: ByteArray, repository: ScheduleRepository): ImportResult {
        val file = runCatching { BackupFile.json.decodeFromString(BackupFile.serializer(), data.toString(Charsets.UTF_8)) }.getOrNull()
            ?: throw BackupException.unreadableFile
        if (file.version > BackupFile.CURRENT_VERSION) throw BackupException.unreadableFile

        val scheduleIds = repository.allSchedules().map { it.id.uppercase() }.toHashSet()
        val anniversaryIds = repository.allAnniversaries().map { it.id.uppercase() }.toHashSet()

        var skipped = 0
        val schedules = file.schedules.mapNotNull { record ->
            if (record.id.uppercase() in scheduleIds) { skipped++; null } else record.toSchedule()
        }
        val anniversaries = file.anniversaries.mapNotNull { record ->
            if (record.id.uppercase() in anniversaryIds) { skipped++; null } else record.toAnniversary()
        }
        repository.importAll(schedules, anniversaries)
        return ImportResult(added = schedules.size + anniversaries.size, skipped = skipped)
    }

    /** 타입 2 — 한 줄 = 종일 단일 일정. 같은 날 같은 제목이 이미 있으면 건너뛴다 */
    private suspend fun importType2(data: ByteArray, repository: ScheduleRepository): ImportResult {
        val entries = runCatching { ExportType2Importer.entries(data) }.getOrNull() ?: throw BackupException.unreadableFile
        val zone = ZoneId.systemDefault()

        val seen = repository.allSchedules()
            .filter { it.recurrence == Recurrence.NONE }
            .map { dedupeKey(it.title, it.startDate.toLocalDate(zone)) }
            .toHashSet()

        var skipped = 0
        val schedules = mutableListOf<Schedule>()
        for (entry in entries) {
            val date = runCatching { LocalDate.of(entry.year, entry.month, entry.day) }.getOrNull()
            if (date == null) { skipped++; continue }
            if (!seen.add(dedupeKey(entry.title, date))) { skipped++; continue }
            schedules += Schedule(title = entry.title, startDate = date.toEpochMillis(zone))
        }
        repository.importAll(schedules, emptyList())
        return ImportResult(added = schedules.size, skipped = skipped)
    }

    private fun dedupeKey(title: String, day: LocalDate): String = "${day.toEpochDay()}|$title"
}

/** Swift `Date` 의 JSON 기본 인코딩 — 2001-01-01T00:00:00Z 기준 초(Double). 파일 호환을 위해 그대로 쓴다 */
object SwiftDate {
    private const val REFERENCE_EPOCH_SECONDS = 978_307_200.0

    fun fromEpochMillis(millis: Long): Double = millis / 1000.0 - REFERENCE_EPOCH_SECONDS

    fun toEpochMillis(swiftSeconds: Double): Long = ((swiftSeconds + REFERENCE_EPOCH_SECONDS) * 1000).roundToLong()
}

/** 내보내기 타입 1 (.hscal). 필드는 알파벳순 — iOS 가 sortedKeys 로 쓰는 순서와 같다 */
@Serializable
data class BackupFile(
    val anniversaries: List<AnniversaryRecord>,
    val exportedAt: Double,
    val schedules: List<ScheduleRecord>,
    val version: Int,
) {
    @Serializable
    data class ScheduleRecord(
        val colorRaw: String,
        val createdAt: Double,
        val endDate: Double? = null,
        val hasCustomColor: Boolean,
        val hasTime: Boolean,
        val id: String,
        val memo: String,
        val monthlyOnLastDay: Boolean,
        val notifies: Boolean,
        val recurrenceRaw: String,
        val startDate: Double,
        val title: String,
    ) {
        fun toSchedule(): Schedule = Schedule(
            id = id.uppercase(),
            title = title,
            memo = memo,
            startDate = SwiftDate.toEpochMillis(startDate),
            hasTime = hasTime,
            recurrenceRaw = recurrenceRaw,
            colorRaw = colorRaw,
            endDate = endDate?.let(SwiftDate::toEpochMillis),
            hasCustomColor = hasCustomColor,
            notifies = notifies,
            monthlyOnLastDay = monthlyOnLastDay,
            createdAt = SwiftDate.toEpochMillis(createdAt),
        )

        companion object {
            fun from(schedule: Schedule) = ScheduleRecord(
                colorRaw = schedule.colorRaw,
                createdAt = SwiftDate.fromEpochMillis(schedule.createdAt),
                endDate = schedule.endDate?.let(SwiftDate::fromEpochMillis),
                hasCustomColor = schedule.hasCustomColor,
                hasTime = schedule.hasTime,
                id = schedule.id.uppercase(),
                memo = schedule.memo,
                monthlyOnLastDay = schedule.monthlyOnLastDay,
                notifies = schedule.notifies,
                recurrenceRaw = schedule.recurrenceRaw,
                startDate = SwiftDate.fromEpochMillis(schedule.startDate),
                title = schedule.title,
            )
        }
    }

    @Serializable
    data class AnniversaryRecord(
        val createdAt: Double,
        val day: Int,
        val id: String,
        val isLunar: Boolean,
        val month: Int,
        val name: String,
    ) {
        fun toAnniversary(): AnniversaryEntry = AnniversaryEntry(
            id = id.uppercase(),
            name = name,
            month = month,
            day = day,
            isLunar = isLunar,
            createdAt = SwiftDate.toEpochMillis(createdAt),
        )

        companion object {
            fun from(entry: AnniversaryEntry) = AnniversaryRecord(
                createdAt = SwiftDate.fromEpochMillis(entry.createdAt),
                day = entry.day,
                id = entry.id.uppercase(),
                isLunar = entry.isLunar,
                month = entry.month,
                name = entry.name,
            )
        }
    }

    companion object {
        const val CURRENT_VERSION = 1

        val json: Json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            ignoreUnknownKeys = true
            explicitNulls = false // endDate 가 없으면 키를 생략 (iOS 와 동일)
            encodeDefaults = true
        }
    }
}
