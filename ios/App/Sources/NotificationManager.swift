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

    /// 예약 전체 갱신 요청.
    /// 알림 시간 피커를 돌리거나 연달아 저장하면 짧은 간격으로 여러 번 불린다 →
    /// 잠깐 모았다가 마지막 요청 하나만, 이전 작업이 끝난 뒤 순서대로 반영한다.
    static func refresh(context: ModelContext) {
        let container = context.container
        Task { @MainActor in
            RefreshScheduler.shared.request(container: container)
        }
    }

    @MainActor
    private final class RefreshScheduler {
        static let shared = RefreshScheduler()

        /// 연속 요청을 모으는 시간
        private let debounce: Duration = .milliseconds(300)
        private var current: Task<Void, Never>?

        func request(container: ModelContainer) {
            let previous = current
            previous?.cancel()

            current = Task { @MainActor in
                // 앞선 반영이 끝난 뒤에 시작 — 예약 삭제/추가가 섞이지 않게
                await previous?.value
                do { try await Task.sleep(for: debounce) } catch { return }

                // 모델 읽기는 메인 액터에서 끝내고, 이후엔 값(Digest)만 다룬다
                let digests = NotificationManager.makeDigests(context: ModelContext(container))
                await NotificationManager.apply(digests)
            }
        }
    }

    /// 기존 예약을 지우고 새 일괄 알림을 예약한다. 더 새 요청이 오면(취소) 중간에 멈춘다.
    private static func apply(_ digests: [Digest]) async {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()

        let pending = await center.pendingNotificationRequests()
            .map(\.identifier)
            .filter { $0.hasPrefix(identifierPrefix) }
        center.removePendingNotificationRequests(withIdentifiers: pending)

        guard settings.authorizationStatus == .authorized
            || settings.authorizationStatus == .provisional
        else { return }

        for digest in digests {
            // 더 새 요청이 들어왔으면 그쪽이 지우고 다시 예약하므로 여기서 멈춘다
            if Task.isCancelled { return }

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

    /// 향후 30일 중 알림 일정이 있는 날의 일괄 알림 내용
    private static func makeDigests(context: ModelContext) -> [Digest] {
        let calendar = Calendar.current
        let now = Date.now
        // 알림을 켠 일정 중 향후 30일에 나올 수 있는 것만 읽는다 (반복 일정은 판정에 맡김)
        let rangeStart = calendar.startOfDay(for: now)
        let rangeEnd = calendar.date(byAdding: .day, value: 31, to: rangeStart) ?? now
        let singleRaw = Recurrence.none.rawValue
        let descriptor = FetchDescriptor<Schedule>(predicate: #Predicate { schedule in
            schedule.notifies && (
                schedule.recurrenceRaw != singleRaw
                    || (schedule.startDate >= rangeStart && schedule.startDate < rangeEnd)
            )
        })
        let schedules = (try? context.fetch(descriptor)) ?? []
        guard !schedules.isEmpty else { return [] }

        let hour = NotificationSettings.digestHour
        let minute = NotificationSettings.digestMinute

        let days = (0..<30).compactMap { calendar.date(byAdding: .day, value: $0, to: now) }
        // 종료됐거나 아직 시작 전인 반복 일정은 빼고, 맞을 수 있는 날에만 후보로 둔다
        let candidates = ScheduleStore.candidatesByDay(in: schedules, days: days, calendar: calendar)

        return days.compactMap { day in
            let dayNotes = ScheduleStore.occurrences(
                in: candidates[calendar.startOfDay(for: day)] ?? [], on: day, calendar: calendar
            )
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
