import Foundation

/// 반복 유형별 색 설정. 일정 개별 색이 아니라 유형(단일/매주/매달)이 색을 결정한다.
/// `SharedDefaults`(App Group)에 저장해 위젯과 공유.
enum EventColorSettings {
    /// 기념일 표시 색 (고정)
    static let anniversaryColor: ColorTag = .pink

    private static func key(for recurrence: Recurrence) -> String {
        "eventColor.\(recurrence.rawValue)"
    }

    private static func defaultColor(for recurrence: Recurrence) -> ColorTag {
        switch recurrence {
        case .none: .blue
        case .weekly: .green
        case .monthly: .orange
        }
    }

    static func color(for recurrence: Recurrence) -> ColorTag {
        SharedDefaults.value(forKey: key(for: recurrence), default: defaultColor(for: recurrence))
    }

    static func setColor(_ tag: ColorTag, for recurrence: Recurrence) {
        SharedDefaults.set(tag, forKey: key(for: recurrence))
    }
}
