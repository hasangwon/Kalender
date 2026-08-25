import SwiftUI

/// 앱/위젯 글자 크기 4단계
enum TextSizeOption: String, CaseIterable, Identifiable {
    case small
    case medium
    case large
    case extraLarge

    var id: String { rawValue }

    var label: String {
        switch self {
        case .small: "작게"
        case .medium: "중간"
        case .large: "크게"
        case .extraLarge: "매우 크게"
        }
    }

    /// 앱 전체 Dynamic Type 크기 (iPad는 한 단계 크게)
    var dynamicTypeSize: DynamicTypeSize {
        #if os(iOS)
        let isPad = UIDevice.current.userInterfaceIdiom == .pad
        #else
        let isPad = false
        #endif

        switch self {
        case .small: return isPad ? .large : .small
        case .medium: return isPad ? .xLarge : .large
        case .large: return isPad ? .xxLarge : .xLarge
        case .extraLarge: return isPad ? .accessibility2 : .xxLarge
        }
    }

    /// iPhone용 배율 — 좁은 셀에 맞춰 완만하게
    private var phoneScale: CGFloat {
        switch self {
        case .small: 0.9
        case .medium: 1.0
        case .large: 1.15
        case .extraLarge: 1.32
        }
    }

    /// iPad용 배율 — 넓은 셀을 채우도록 크게
    private var padScale: CGFloat {
        switch self {
        case .small: 1.15
        case .medium: 1.35
        case .large: 1.6
        case .extraLarge: 1.9
        }
    }

    /// 앱 내부 고정 폰트에 곱할 배율 (기기 자동 판별)
    var scale: CGFloat {
        #if os(iOS)
        return UIDevice.current.userInterfaceIdiom == .pad ? padScale : phoneScale
        #else
        return phoneScale
        #endif
    }

    /// 위젯 전용 배율 — 앱처럼 단계별로 확실한 차이
    var widgetScale: CGFloat {
        switch self {
        case .small: 0.82
        case .medium: 1.0
        case .large: 1.18
        case .extraLarge: 1.38
        }
    }
}

/// 위젯 글자 크기 — 앱과 독립적으로 저장 (App Group 공유)
enum WidgetTextSizeSettings {
    private static let key = "theme.widgetTextSize"

    static var current: TextSizeOption {
        guard let raw = EventColorSettings.store.string(forKey: key),
              let option = TextSizeOption(rawValue: raw)
        else { return .medium }

        return option
    }

    static func setCurrent(_ option: TextSizeOption) {
        EventColorSettings.store.set(option.rawValue, forKey: key)
    }
}

enum TextSizeSettings {
    private static let key = "theme.textSize"

    /// 저장값이 없으면 기기별 기본 (iPad는 한 단계 크게)
    static var current: TextSizeOption {
        if let raw = EventColorSettings.store.string(forKey: key),
           let option = TextSizeOption(rawValue: raw) {
            return option
        }

        #if os(iOS)
        return isPad ? .large : .medium
        #else
        return .medium
        #endif
    }

    static func setCurrent(_ option: TextSizeOption) {
        EventColorSettings.store.set(option.rawValue, forKey: key)
    }

    #if os(iOS)
    private static var isPad: Bool {
        UIDevice.current.userInterfaceIdiom == .pad
    }
    #endif
}
