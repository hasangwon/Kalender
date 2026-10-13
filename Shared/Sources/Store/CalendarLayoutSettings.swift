import Foundation

/// 앱 달력 모양 — 와이드(좌우 여백 없이 화면 폭 전체) / 노말(카드).
/// 케이스 순서가 설정 화면의 선택 버튼 순서다
enum CalendarLayoutStyle: String, CaseIterable, Identifiable {
    case wide
    case normal

    var id: String { rawValue }

    var label: String {
        switch self {
        case .normal: "노말"
        case .wide: "와이드"
        }
    }
}

/// 달력 모양 설정 (App Group 공유 저장소). 기본은 와이드
enum CalendarLayoutSettings {
    private static let key = "calendar.layoutStyle"

    static var current: CalendarLayoutStyle {
        SharedDefaults.value(forKey: key, default: .wide)
    }

    static func setCurrent(_ style: CalendarLayoutStyle) {
        SharedDefaults.set(style, forKey: key)
    }
}
