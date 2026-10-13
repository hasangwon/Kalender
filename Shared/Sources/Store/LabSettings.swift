import Foundation

/// 실험실 기능 (정보 화면에서 켜고 끔). 모두 기본 꺼짐.
enum LabSettings {
    private static var store: UserDefaults { SharedDefaults.store }

    private static let todayLabelKey = "lab.showsTodayLabel"

    /// 달력에서 오늘 날짜 숫자 옆에 작게 "오늘" 표시
    static var showsTodayLabel: Bool {
        get { store.bool(forKey: todayLabelKey) }
        set { store.set(newValue, forKey: todayLabelKey) }
    }
}
