import Foundation
import SwiftData
import UserNotifications

/// 하루 일괄 알림 관리.
/// 알림을 켠 일정이 있는 날마다, 설정된 시간에 그날 일정을 모아 로컬 알림 1건을 예약한다.
/// 향후 30일치를 미리 예약하고, 데이터/설정 변경과 앱 실행 시마다 전체 갱신한다.
enum NotificationManager {
    private static let identifierPrefix = "daily-digest-"

    /// 알림 토글을 켤 때 호출 — 권한이 미결정이면 요청
    static func requestAuthorizationIfNeeded() {
        let center = UNUserNotificationCenter.current()

        center.getNotificationSettings { settings in
            guard settings.authorizationStatus == .notDetermined else { return }

            center.requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
        }
    }

    /// 예약할 알림 1건 (값 타입 — 비동기 작업에 SwiftData 모델을 넘기지 않기 위해)
    private struct Digest {
        let components: DateComponents
        let title: String
        let body: String
    }

    /// 예약 전체 갱신
    static func refresh(context: ModelContext) {
        // 모델 읽기는 호출한 컨텍스트에서 동기로 끝내고, Task에는 값만 넘긴다
        let digests = makeDigests(context: context)

        Task {
            let center = UNUserNotificationCenter.current()
            let settings = await center.notificationSettings()

            // 기존 예약 제거 후 다시 계산
            let pending = await center.pendingNotificationRequests()
                .map(\.identifier)
                .filter { $0.hasPrefix(identifierPrefix) }
            center.removePendingNotificationRequests(withIdentifiers: pending)

            guard settings.authorizationStatus == .authorized
                || settings.authorizationStatus == .provisional
            else { return }

            for digest in digests {
                let content = UNMutableNotificationContent()
                content.title = digest.title
                content.body = digest.body
                content.sound = .default

                let request = UNNotificationRequest(
                    identifier: identifierPrefix + digestID(digest.components),
                    content: content,
                    trigger: UNCalendarNotificationTrigger(dateMatching: digest.components, repeats: false)
                )

                try? await center.add(request)
            }
        }
    }

    /// 향후 30일 중 알림 일정이 있는 날의 일괄 알림 내용
    private static func makeDigests(context: ModelContext) -> [Digest] {
        let schedules = ((try? context.fetch(FetchDescriptor<Schedule>())) ?? [])
            .filter(\.notifies)
        guard !schedules.isEmpty else { return [] }

        let calendar = Calendar.current
        let now = Date.now
        let hour = NotificationSettings.digestHour
        let minute = NotificationSettings.digestMinute

        return (0..<30).compactMap { offset in
            guard let day = calendar.date(byAdding: .day, value: offset, to: now) else { return nil }

            let dayNotes = ScheduleStore.occurrences(in: schedules, on: day, calendar: calendar)
            guard !dayNotes.isEmpty else { return nil }

            var components = calendar.dateComponents([.year, .month, .day], from: day)
            components.hour = hour
            components.minute = minute

            // 오늘인데 알림 시간이 이미 지났으면 스킵
            guard let fireDate = calendar.date(from: components), fireDate > now else { return nil }

            return Digest(
                components: components,
                title: "오늘의 일정 \(dayNotes.count)개",
                body: digestBody(for: dayNotes)
            )
        }
    }

    private static func digestID(_ components: DateComponents) -> String {
        String(format: "%04d%02d%02d", components.year ?? 0, components.month ?? 0, components.day ?? 0)
    }

    private static func digestBody(for schedules: [Schedule]) -> String {
        let titles = schedules.prefix(3).map { schedule in
            if let timeText = schedule.timeText {
                return "\(schedule.title) (\(timeText))"
            }
            return schedule.title
        }

        let joined = titles.joined(separator: " · ")
        let remaining = schedules.count - titles.count

        return remaining > 0 ? "\(joined) 외 \(remaining)개" : joined
    }
}
