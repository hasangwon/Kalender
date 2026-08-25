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

// CloudKit 동기화 요건: @Attribute(.unique) 사용 불가, 모든 속성에 기본값 필요.
@Model
final class Schedule {
    var id: UUID = UUID()
    var title: String = ""
    var memo: String = ""
    /// 기준 날짜. 반복 일정이면 반복의 시작일이자 요일/일자의 기준.
    var startDate: Date = Date.now
    /// true면 startDate의 시각 정보를 사용, false면 종일 일정
    var hasTime: Bool = false
    var recurrenceRaw: String = Recurrence.none.rawValue
    var colorRaw: String = ColorTag.blue.rawValue
    /// 반복 종료일 (포함). nil이면 무제한. 단일 일정은 항상 nil.
    var endDate: Date?
    /// true면 colorRaw의 개별 색 사용, false면 유형 기본 색을 따름
    var hasCustomColor: Bool = false
    /// 하루 일괄 알림 대상 여부
    var notifies: Bool = false
    var createdAt: Date = Date.now

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
        color: ColorTag = .blue,
        endDate: Date? = nil,
        hasCustomColor: Bool = false,
        notifies: Bool = false
    ) {
        self.id = UUID()
        self.title = title
        self.memo = memo
        self.startDate = startDate
        self.hasTime = hasTime
        self.recurrenceRaw = recurrence.rawValue
        self.colorRaw = color.rawValue
        self.endDate = endDate
        self.hasCustomColor = hasCustomColor
        self.notifies = notifies
        self.createdAt = .now
    }

    /// 실제 표시 색 — 개별 지정이 없으면 유형 기본 색
    var displayColor: ColorTag {
        hasCustomColor ? color : EventColorSettings.color(for: recurrence)
    }

    /// 해당 날짜에 이 일정이 발생하는지
    func occurs(on day: Date, calendar: Calendar = .current) -> Bool {
        let dayStart = calendar.startOfDay(for: day)
        let baseStart = calendar.startOfDay(for: startDate)

        guard dayStart >= baseStart else { return false }

        // 반복 종료일 (포함) 이후에는 발생하지 않음
        if let endDate, dayStart > calendar.startOfDay(for: endDate) {
            return false
        }

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
