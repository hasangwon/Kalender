import Foundation

/// 반복 유형별 색 설정. 일정 개별 색이 아니라 유형(단일/매주/매달)이 색을 결정한다.
/// App Group UserDefaults에 저장해 위젯과 공유 (엔타이틀먼트 없으면 standard 폴백).
enum EventColorSettings {
    static var store: UserDefaults {
        UserDefaults(suiteName: SharedConstants.appGroupID) ?? .standard
    }

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
        guard let raw = store.string(forKey: key(for: recurrence)),
              let tag = ColorTag(rawValue: raw)
        else { return defaultColor(for: recurrence) }

        return tag
    }

    static func setColor(_ tag: ColorTag, for recurrence: Recurrence) {
        store.set(tag.rawValue, forKey: key(for: recurrence))
    }
}
