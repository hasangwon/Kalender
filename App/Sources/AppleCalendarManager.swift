import EventKit
import Foundation
import SwiftUI

/// 애플 기본 달력 앱 연동 (읽기 전용).
/// 같은 애플 계정에 연결된 기본 달력의 일정을 우리 앱에 표시만 한다. 쓰기/수정 없음.
@MainActor
final class AppleCalendarManager: ObservableObject {
    private let store = EKEventStore()

    @Published private(set) var isAuthorized = false
    /// 표시용 이벤트 (월 단위로 미리 로드)
    @Published private(set) var events: [AppleCalendarEvent] = []

    /// 현재 권한 상태 갱신
    func refreshAuthorization() {
        let status = EKEventStore.authorizationStatus(for: .event)
        isAuthorized = (status == .fullAccess)
    }

    /// 권한 요청 (설정에서 "애플 달력 연동" 켤 때 호출)
    func requestAccess() async {
        do {
            let granted = try await store.requestFullAccessToEvents()
            isAuthorized = granted
        } catch {
            isAuthorized = false
        }
    }

    /// 지정한 달(기준일이 속한 달) 앞뒤로 이벤트 로드
    func clear() {
        events = []
    }

    func loadEvents(around date: Date, calendar: Calendar = .current) {
        guard SyncSettings.appleCalendarEnabled, isAuthorized else {
            events = []
            return
        }

        // 이전 달 1일 ~ 다음 달 말일까지 넉넉히 (인접 달 표시 대응)
        guard let monthStart = calendar.date(from: calendar.dateComponents([.year, .month], from: date)),
              let rangeStart = calendar.date(byAdding: .day, value: -7, to: monthStart),
              let rangeEnd = calendar.date(byAdding: .month, value: 2, to: monthStart)
        else { return }

        // 공휴일/구독 캘린더 제외 — 우리 자체 공휴일과 중복되지 않도록.
        // 사용자가 직접 쓰는 로컬/CalDAV/Exchange 캘린더만 대상으로.
        let editableCalendars = store.calendars(for: .event).filter { cal in
            cal.allowsContentModifications
                && cal.type != .birthday
                && cal.type != .subscription
        }

        guard !editableCalendars.isEmpty else {
            events = []
            return
        }

        let predicate = store.predicateForEvents(
            withStart: rangeStart, end: rangeEnd, calendars: editableCalendars
        )

        events = store.events(matching: predicate).map { ekEvent in
            AppleCalendarEvent(
                id: ekEvent.eventIdentifier ?? UUID().uuidString,
                title: ekEvent.title ?? "(제목 없음)",
                startDate: ekEvent.startDate,
                isAllDay: ekEvent.isAllDay,
                colorHex: ekEvent.calendar?.cgColor.flatMap(Self.hexString(from:))
            )
        }
    }

    private static func hexString(from cgColor: CGColor) -> String? {
        guard let comps = cgColor.components, comps.count >= 3 else { return nil }
        let r = Int((comps[0] * 255).rounded())
        let g = Int((comps[1] * 255).rounded())
        let b = Int((comps[2] * 255).rounded())
        return String(format: "#%02X%02X%02X", r, g, b)
    }
}
