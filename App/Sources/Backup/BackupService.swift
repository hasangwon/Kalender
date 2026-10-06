import SwiftData
import SwiftUI
import UniformTypeIdentifiers
import WidgetKit

extension UTType {
    /// 우리 달력 백업 (.hscal, JSON)
    static let hscalBackup = UTType(exportedAs: "com.hasangwon.planwidget.backup", conformingTo: .json)
    /// 빨간달력 내보내기 파일 (.redcalendar, Realm DB)
    static let redCalendar = UTType(importedAs: "com.hasangwon.planwidget.redcalendar", conformingTo: .data)
}

/// 백업 내보내기/가져오기. 가져올 때 확장자로 우리 백업과 빨간달력을 구분한다.
enum BackupService {
    struct ImportResult {
        let added: Int
        let skipped: Int
    }

    enum BackupError: LocalizedError {
        case unsupportedFile
        case unreadableFile

        var errorDescription: String? {
            switch self {
            case .unsupportedFile: "지원하지 않는 파일이에요"
            case .unreadableFile: "파일을 읽을 수 없어요"
            }
        }
    }

    // MARK: - 내보내기

    static func exportData(context: ModelContext) throws -> Data {
        let schedules = try context.fetch(FetchDescriptor<Schedule>(sortBy: [SortDescriptor(\.createdAt)]))
        let anniversaries = try context.fetch(FetchDescriptor<AnniversaryEntry>(sortBy: [SortDescriptor(\.createdAt)]))

        let file = BackupFile(
            version: BackupFile.currentVersion,
            exportedAt: .now,
            schedules: schedules.map(BackupFile.ScheduleRecord.init),
            anniversaries: anniversaries.map(BackupFile.AnniversaryRecord.init)
        )

        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return try encoder.encode(file)
    }

    static var defaultExportName: String {
        "hscal_" + Date.now.formatted(.verbatim(
            "\(year: .defaultDigits)\(month: .twoDigits)\(day: .twoDigits)",
            timeZone: .current, calendar: .current
        ))
    }

    // MARK: - 가져오기

    static func importFile(at url: URL, context: ModelContext) throws -> ImportResult {
        let isScoped = url.startAccessingSecurityScopedResource()
        defer { if isScoped { url.stopAccessingSecurityScopedResource() } }

        guard let data = try? Data(contentsOf: url) else { throw BackupError.unreadableFile }

        let result: ImportResult
        switch url.pathExtension.lowercased() {
        case "redcalendar":
            result = try importRedCalendar(data, context: context)
        case "hscal":
            result = try importBackup(data, context: context)
        default:
            throw BackupError.unsupportedFile
        }

        if result.added > 0 {
            try? context.save()
            WidgetCenter.shared.reloadAllTimelines()
            NotificationManager.refresh(context: context)
        }
        return result
    }

    /// 빨간달력 — 한 줄 = 종일 단일 일정. 같은 날 같은 제목이 이미 있으면 건너뛴다.
    private static func importRedCalendar(_ data: Data, context: ModelContext) throws -> ImportResult {
        guard let entries = try? RedCalendarImporter.entries(from: data) else { throw BackupError.unreadableFile }

        let calendar = Calendar.current
        let existing = try context.fetch(FetchDescriptor<Schedule>())
        var seen = Set(existing.filter { $0.recurrence == .none }.map {
            dedupeKey(title: $0.title, day: calendar.startOfDay(for: $0.startDate))
        })

        var added = 0
        var skipped = 0
        for entry in entries {
            guard let date = calendar.date(from: DateComponents(year: entry.year, month: entry.month, day: entry.day)),
                  calendar.component(.day, from: date) == entry.day
            else {
                skipped += 1
                continue
            }

            let key = dedupeKey(title: entry.title, day: date)
            guard seen.insert(key).inserted else {
                skipped += 1
                continue
            }

            context.insert(Schedule(title: entry.title, startDate: date))
            added += 1
        }
        return ImportResult(added: added, skipped: skipped)
    }

    private static func dedupeKey(title: String, day: Date) -> String {
        "\(day.timeIntervalSinceReferenceDate)|\(title)"
    }

    /// 우리 백업 — id가 이미 있는 항목은 건너뛴다 (같은 백업을 두 번 가져와도 중복 없음)
    private static func importBackup(_ data: Data, context: ModelContext) throws -> ImportResult {
        guard let file = try? JSONDecoder().decode(BackupFile.self, from: data),
              file.version <= BackupFile.currentVersion
        else { throw BackupError.unreadableFile }

        let scheduleIDs = Set(try context.fetch(FetchDescriptor<Schedule>()).map(\.id))
        let anniversaryIDs = Set(try context.fetch(FetchDescriptor<AnniversaryEntry>()).map(\.id))

        var added = 0
        var skipped = 0
        for record in file.schedules {
            guard !scheduleIDs.contains(record.id) else {
                skipped += 1
                continue
            }
            context.insert(record.makeSchedule())
            added += 1
        }
        for record in file.anniversaries {
            guard !anniversaryIDs.contains(record.id) else {
                skipped += 1
                continue
            }
            context.insert(record.makeAnniversary())
            added += 1
        }
        return ImportResult(added: added, skipped: skipped)
    }
}

// MARK: - 백업 파일 형식

/// 우리 달력 백업 (.hscal). 모델을 그대로 담는다.
struct BackupFile: Codable {
    static let currentVersion = 1

    var version: Int
    var exportedAt: Date
    var schedules: [ScheduleRecord]
    var anniversaries: [AnniversaryRecord]

    struct ScheduleRecord: Codable {
        var id: UUID
        var title: String
        var memo: String
        var startDate: Date
        var hasTime: Bool
        var recurrenceRaw: String
        var colorRaw: String
        var endDate: Date?
        var hasCustomColor: Bool
        var notifies: Bool
        var monthlyOnLastDay: Bool
        var createdAt: Date

        init(_ schedule: Schedule) {
            id = schedule.id
            title = schedule.title
            memo = schedule.memo
            startDate = schedule.startDate
            hasTime = schedule.hasTime
            recurrenceRaw = schedule.recurrenceRaw
            colorRaw = schedule.colorRaw
            endDate = schedule.endDate
            hasCustomColor = schedule.hasCustomColor
            notifies = schedule.notifies
            monthlyOnLastDay = schedule.monthlyOnLastDay
            createdAt = schedule.createdAt
        }

        func makeSchedule() -> Schedule {
            let schedule = Schedule(
                title: title,
                memo: memo,
                startDate: startDate,
                hasTime: hasTime,
                endDate: endDate,
                hasCustomColor: hasCustomColor,
                notifies: notifies,
                monthlyOnLastDay: monthlyOnLastDay
            )
            schedule.id = id
            schedule.recurrenceRaw = recurrenceRaw
            schedule.colorRaw = colorRaw
            schedule.createdAt = createdAt
            return schedule
        }
    }

    struct AnniversaryRecord: Codable {
        var id: UUID
        var name: String
        var month: Int
        var day: Int
        var isLunar: Bool
        var createdAt: Date

        init(_ entry: AnniversaryEntry) {
            id = entry.id
            name = entry.name
            month = entry.month
            day = entry.day
            isLunar = entry.isLunar
            createdAt = entry.createdAt
        }

        func makeAnniversary() -> AnniversaryEntry {
            let entry = AnniversaryEntry(name: name, month: month, day: day, isLunar: isLunar)
            entry.id = id
            entry.createdAt = createdAt
            return entry
        }
    }
}

/// fileExporter용 문서 래퍼
struct BackupDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.hscalBackup] }

    let data: Data

    init(data: Data) {
        self.data = data
    }

    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents else { throw BackupService.BackupError.unreadableFile }
        self.data = data
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}
