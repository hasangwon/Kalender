import SwiftUI
import UIKit

/// 선택 가능한 테마색 (설정 > 테마색)
enum ThemeColor: String, CaseIterable, Identifiable {
    case blue
    case indigo
    case teal
    case green
    case orange
    case pink
    case purple
    case charcoal

    var id: String { rawValue }

    var label: String {
        switch self {
        case .blue: "블루"
        case .indigo: "인디고"
        case .teal: "청록"
        case .green: "그린"
        case .orange: "오렌지"
        case .pink: "핑크"
        case .purple: "퍼플"
        case .charcoal: "차콜"
        }
    }

    var color: Color {
        switch self {
        case .blue:
            AppTheme.dynamicColor(light: (0.192, 0.510, 0.965), dark: (0.357, 0.612, 0.973))
        case .indigo:
            AppTheme.dynamicColor(light: (0.353, 0.396, 0.910), dark: (0.494, 0.533, 0.945))
        case .teal:
            AppTheme.dynamicColor(light: (0.051, 0.588, 0.573), dark: (0.278, 0.702, 0.686))
        case .green:
            AppTheme.dynamicColor(light: (0.086, 0.588, 0.318), dark: (0.290, 0.714, 0.475))
        case .orange:
            AppTheme.dynamicColor(light: (0.929, 0.510, 0.106), dark: (0.965, 0.608, 0.259))
        case .pink:
            AppTheme.dynamicColor(light: (0.902, 0.318, 0.545), dark: (0.945, 0.463, 0.647))
        case .purple:
            AppTheme.dynamicColor(light: (0.545, 0.361, 0.902), dark: (0.671, 0.522, 0.945))
        case .charcoal:
            AppTheme.dynamicColor(light: (0.231, 0.263, 0.322), dark: (0.588, 0.627, 0.694))
        }
    }
}

/// 테마색 저장/로드 (App Group — 위젯 공유)
enum ThemeSettings {
    private static let key = "theme.primaryColor"

    static var current: ThemeColor {
        guard let raw = EventColorSettings.store.string(forKey: key),
              let theme = ThemeColor(rawValue: raw)
        else { return .blue }

        return theme
    }

    static func setCurrent(_ theme: ThemeColor) {
        EventColorSettings.store.set(theme.rawValue, forKey: key)
    }
}

/// 선택 가능한 배경색 (설정 > 배경색) — 연한 톤 + 잉크(다크)
enum BackgroundColor: String, CaseIterable, Identifiable {
    case gray
    case cream
    case pink
    case blue
    case green
    case lavender
    case ink

    var id: String { rawValue }

    var label: String {
        switch self {
        case .gray: "그레이"
        case .cream: "살구"
        case .pink: "핑크"
        case .blue: "블루"
        case .green: "그린"
        case .lavender: "라벤더"
        case .ink: "잉크"
        }
    }

    /// 잉크 선택 시 앱 전체를 다크 팔레트로 렌더링
    var forcesDark: Bool { self == .ink }

    private var lightRGB: (Double, Double, Double) {
        switch self {
        case .gray: (0.973, 0.976, 0.980)
        case .cream: (0.996, 0.978, 0.953)
        case .pink: (0.996, 0.973, 0.976)
        case .blue: (0.966, 0.978, 0.996)
        case .green: (0.969, 0.984, 0.971)
        case .lavender: (0.980, 0.973, 0.996)
        case .ink: (0.118, 0.122, 0.133)
        }
    }

    var color: Color {
        if self == .ink {
            return AppTheme.dynamicColor(light: lightRGB, dark: lightRGB)
        }

        return AppTheme.dynamicColor(light: lightRGB, dark: (0.106, 0.110, 0.122))
    }

    /// 설정 화면 스와치용 (라이트 값 고정)
    var swatch: Color {
        Color(red: lightRGB.0, green: lightRGB.1, blue: lightRGB.2)
    }
}

/// 배경색 저장/로드 (App Group — 위젯 공유)
enum BackgroundSettings {
    private static let key = "theme.backgroundColor"

    static var current: BackgroundColor {
        guard let raw = EventColorSettings.store.string(forKey: key),
              let background = BackgroundColor(rawValue: raw)
        else { return .gray }

        return background
    }

    static func setCurrent(_ background: BackgroundColor) {
        EventColorSettings.store.set(background.rawValue, forKey: key)
    }
}

/// 앱 전역 테마. 대표색·배경색은 설정에서 선택.
enum AppTheme {
    /// 대표 테마색 — 선택/오늘/버튼/포인트 (설정에서 변경 가능)
    static var primary: Color {
        ThemeSettings.current.color
    }

    /// 주말·공휴일 표시용 고정 빨강 (테마색과 무관)
    static let holidayRed = dynamicColor(
        light: (0.867, 0.243, 0.243),
        dark: (0.937, 0.412, 0.412)
    )

    /// 배경 — 설정에서 선택 (기본 그레이)
    static var background: Color {
        BackgroundSettings.current.color
    }

    /// 보조색 (딥 세이지) — 서브 포인트
    static let accent = dynamicColor(
        light: (0.290, 0.420, 0.365),
        dark: (0.561, 0.702, 0.639)
    )

    /// 카드/셀 표면 (배경 위 한 단계)
    static let surface = dynamicColor(
        light: (1.0, 1.0, 1.0),
        dark: (0.169, 0.176, 0.192)
    )

    static func dynamicColor(
        light: (Double, Double, Double),
        dark: (Double, Double, Double)
    ) -> Color {
        Color(uiColor: UIColor { traits in
            let rgb = traits.userInterfaceStyle == .dark ? dark : light
            return UIColor(red: rgb.0, green: rgb.1, blue: rgb.2, alpha: 1)
        })
    }
}
