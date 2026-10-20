import UIKit

enum InterfaceStyle {
    /// 배경색 설정(잉크=다크 강제)을 모든 윈도우에 즉시 적용.
    /// preferredColorScheme은 .dark → nil 복원이 안 되는 결함이 있어 UIKit으로 직접 제어한다.
    static func apply() {
        let style: UIUserInterfaceStyle =
            BackgroundSettings.current.forcesDark ? .dark : .unspecified

        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .forEach { $0.overrideUserInterfaceStyle = style }
    }
}
