import SwiftUI

/// 일정 색상 팔레트 (8색)
enum ColorTag: String, CaseIterable, Codable, Identifiable {
    case red
    case orange
    case yellow
    case green
    case teal
    case blue
    case purple
    case pink

    var id: String { rawValue }

    var color: Color {
        switch self {
        case .red: Color(red: 0.886, green: 0.353, blue: 0.322)
        case .orange: Color(red: 0.925, green: 0.565, blue: 0.263)
        case .yellow: Color(red: 0.898, green: 0.729, blue: 0.263)
        case .green: Color(red: 0.451, green: 0.663, blue: 0.396)
        case .teal: Color(red: 0.302, green: 0.635, blue: 0.616)
        case .blue: Color(red: 0.333, green: 0.529, blue: 0.808)
        case .purple: Color(red: 0.573, green: 0.463, blue: 0.780)
        case .pink: Color(red: 0.898, green: 0.510, blue: 0.639)
        }
    }

    var label: String {
        switch self {
        case .red: "빨강"
        case .orange: "주황"
        case .yellow: "노랑"
        case .green: "초록"
        case .teal: "청록"
        case .blue: "파랑"
        case .purple: "보라"
        case .pink: "분홍"
        }
    }
}
