import Foundation

/// 동기화 관련 사용자 설정 (App Group 공유)
enum SyncSettings {
    private static var store: UserDefaults { EventColorSettings.store }

    private static let iCloudKey = "sync.iCloudEnabled"
    private static let appleCalendarKey = "sync.appleCalendarEnabled"

    /// iCloud 동기화 (같은 애플 계정끼리 일정 공유). 기본 꺼짐.
    static var iCloudEnabled: Bool {
        get { store.bool(forKey: iCloudKey) }
        set { store.set(newValue, forKey: iCloudKey) }
    }

    /// 애플 기본 달력 앱 일정 표시 (읽기 전용). 기본 꺼짐.
    static var appleCalendarEnabled: Bool {
        get { store.bool(forKey: appleCalendarKey) }
        set { store.set(newValue, forKey: appleCalendarKey) }
    }
}
