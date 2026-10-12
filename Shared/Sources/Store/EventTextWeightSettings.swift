import SwiftUI

/// 달력 칸 일정 글씨 두께 3단계 (앱·위젯 공통)
enum EventTextWeight: String, CaseIterable, Identifiable {
    case light
    case medium
    case semibold

    var id: String { rawValue }

    var label: String {
        switch self {
        case .light: "얇게"
        case .medium: "보통"
        case .semibold: "두껍게"
        }
    }

    var fontWeight: Font.Weight {
        switch self {
        case .light: .light
        case .medium: .medium
        case .semibold: .semibold
        }
    }
}

/// 일정 글씨 두께 설정 (App Group 공유 저장소)
enum EventTextWeightSettings {
    private static let key = "theme.eventTextWeight"

    static var current: EventTextWeight {
        SharedDefaults.value(forKey: key, default: .medium)
    }

    static func setCurrent(_ weight: EventTextWeight) {
        SharedDefaults.set(weight, forKey: key)
    }
}
