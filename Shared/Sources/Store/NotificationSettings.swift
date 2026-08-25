import Foundation

/// 하루 일괄 알림 시간 설정 (App Group 저장)
enum NotificationSettings {
    private static let hourKey = "notification.digestHour"
    private static let minuteKey = "notification.digestMinute"

    static var digestHour: Int {
        let stored = EventColorSettings.store.object(forKey: hourKey) as? Int
        return stored ?? 8
    }

    static var digestMinute: Int {
        let stored = EventColorSettings.store.object(forKey: minuteKey) as? Int
        return stored ?? 0
    }

    static func setDigestTime(hour: Int, minute: Int) {
        EventColorSettings.store.set(hour, forKey: hourKey)
        EventColorSettings.store.set(minute, forKey: minuteKey)
    }
}
