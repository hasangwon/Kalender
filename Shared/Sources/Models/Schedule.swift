import Foundation
import SwiftData

/// 반복 유형
enum Recurrence: String, Codable, CaseIterable, Identifiable {
    case none
    case weekly
    case monthly

    var id: String { rawValue }

    var label: String {
        switch self {
        case .none: "단일"
        case .weekly: "매주"
        case .monthly: "매달"
        }
    }

    var badgeText: String? {
        switch self {
        case .none: nil
        case .weekly: "매주"
        case .monthly: "매달"
        }
    }
}

@Model
final class Schedule {
    @Attribute(.unique) var id: UUID
    var title: String
    var memo: String
    /// 기준 날짜. 반복 일정이면 반복의 시작일이자 요일/일자의 기준.
    var startDate: Date
    /// true면 startDate의 시각 정보를 사용, false면 종일 일정
    var hasTime: Bool
    var recurrenceRaw: String
    var colorRaw: String
    var createdAt: Date

    var recurrence: Recurrence {
        get { Recurrence(rawValue: recurrenceRaw) ?? .none }
        set { recurrenceRaw = newValue.rawValue }
    }

    var color: ColorTag {
        get { ColorTag(rawValue: colorRaw) ?? .blue }
        set { colorRaw = newValue.rawValue }
    }

    init(
        title: String,
        memo: String = "",
        startDate: Date,
        hasTime: Bool = false,
        recurrence: Recurrence = .none,
        color: ColorTag = .blue
    ) {
        self.id = UUID()
        self.title = title
        self.memo = memo
        self.startDate = startDate
        self.hasTime = hasTime
        self.recurrenceRaw = recurrence.rawValue
        self.colorRaw = color.rawValue
        self.createdAt = .now
    }

    /// 해당 날짜에 이 일정이 발생하는지
    func occurs(on day: Date, calendar: Calendar = .current) -> Bool {
        let dayStart = calendar.startOfDay(for: day)
        let baseStart = calendar.startOfDay(for: startDate)

        guard dayStart >= baseStart else { return false }

        switch recurrence {
        case .none:
            return dayStart == baseStart
        case .weekly:
            return calendar.component(.weekday, from: dayStart)
                == calendar.component(.weekday, from: baseStart)
        case .monthly:
            // 기준일이 29~31일이면 해당 일자가 없는 달에는 발생하지 않음
            return calendar.component(.day, from: dayStart)
                == calendar.component(.day, from: baseStart)
        }
    }

    /// 시간 표시 텍스트 (종일이면 nil)
    var timeText: String? {
        guard hasTime else { return nil }

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "a h:mm"
        return formatter.string(from: startDate)
    }
}
