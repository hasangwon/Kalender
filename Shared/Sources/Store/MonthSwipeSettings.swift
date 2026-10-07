import Foundation

/// 달력 월 넘기기 스와이프 방향.
/// 다음 달은 오른쪽/아래에 있다고 보고, 왼쪽/위로 밀면 다음 달, 오른쪽/아래로 밀면 이전 달.
enum MonthSwipeDirection: String, CaseIterable, Identifiable {
    case horizontal
    case vertical
    case both

    var id: String { rawValue }

    var label: String {
        switch self {
        case .horizontal: "좌우"
        case .vertical: "위아래"
        case .both: "좌우+위아래"
        }
    }

    var allowsHorizontal: Bool { self != .vertical }
    var allowsVertical: Bool { self != .horizontal }
}

/// 월 넘기기 방향 설정 (App Group 공유 저장소)
enum MonthSwipeSettings {
    private static let key = "calendar.monthSwipeDirection"

    static var current: MonthSwipeDirection {
        SharedDefaults.value(forKey: key, default: .horizontal)
    }

    static func setCurrent(_ direction: MonthSwipeDirection) {
        SharedDefaults.set(direction, forKey: key)
    }
}
