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

    /// 마지막으로 불러온 기간 — 그 안으로 이동하면 다시 조회하지 않는다
    private var loadedRange: ClosedRange<Date>?
    /// 지금 화면이 원하는 기간 — 외부 변경 시 이 기간을 다시 조회한다
    private var requestedRange: ClosedRange<Date>?
    /// 빠른 월 이동 시 늦게 끝난 이전 조회가 현재 결과를 덮지 않도록 요청마다 번호를 붙인다
    private var requestID = 0
    private var storeObserver: NSObjectProtocol?

    init() {
        // 달력 앱 등에서 일정이 바뀌면 캐시를 버리고 다시 불러온다
        storeObserver = NotificationCenter.default.addObserver(
            forName: .EKEventStoreChanged, object: store, queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                guard let self, let range = self.requestedRange else { return }
                self.loadedRange = nil
                self.fetch(range)
            }
        }
    }

    /// 로드한 이벤트 비우기 (동기화 해제 시)
    func clear() {
        requestID += 1
        loadedRange = nil
        requestedRange = nil
        events = []
    }

    /// 지정한 달(기준일이 속한 달) 앞뒤로 이벤트 로드
    func loadEvents(around date: Date, calendar: Calendar = .current) {
        guard SyncSettings.appleCalendarEnabled, isAuthorized else {
            clear()
            return
        }

        // 달력이 앞뒤 달 페이지를 미리 깔아 두므로 이전 달 ~ 다음 달 전체,
        // 그리고 그 그리드에 흐리게 보이는 앞뒤 1주까지
        let monthStart = calendar.startOfMonth(for: date)
        guard let previousMonth = calendar.date(byAdding: .month, value: -1, to: monthStart),
              let rangeStart = calendar.date(byAdding: .day, value: -7, to: previousMonth),
              let afterNextMonth = calendar.date(byAdding: .month, value: 2, to: monthStart),
              let rangeEnd = calendar.date(byAdding: .day, value: 7, to: afterNextMonth)
        else { return }

        requestedRange = rangeStart...rangeEnd
        if let loadedRange, loadedRange.contains(rangeStart), loadedRange.contains(rangeEnd) {
            // 이미 있는 결과를 쓰더라도, 그사이 다른 달로 나간 조회가 늦게 끝나 덮어쓰지 않게 무효화한다
            requestID += 1
            return
        }
        fetch(rangeStart...rangeEnd)
    }

    /// EventKit 조회는 동기 API라 메인 밖에서 하고, 표시용 값만 메인으로 가져온다
    private func fetch(_ range: ClosedRange<Date>) {
        requestID += 1
        let id = requestID
        nonisolated(unsafe) let store = self.store

        Task.detached(priority: .userInitiated) {
            let events = Self.queryEvents(store: store, range: range)
            await MainActor.run {
                // 그사이 더 새 요청이 나갔으면 버린다
                guard id == self.requestID else { return }
                self.events = events
                self.loadedRange = range
            }
        }
    }

    nonisolated private static func queryEvents(store: EKEventStore, range: ClosedRange<Date>) -> [AppleCalendarEvent] {
        // 공휴일/구독 캘린더 제외 — 우리 자체 공휴일과 중복되지 않도록.
        // 사용자가 직접 쓰는 로컬/CalDAV/Exchange 캘린더만 대상으로.
        let editableCalendars = store.calendars(for: .event).filter { cal in
            cal.allowsContentModifications
                && cal.type != .birthday
                && cal.type != .subscription
        }
        guard !editableCalendars.isEmpty else { return [] }

        let predicate = store.predicateForEvents(
            withStart: range.lowerBound, end: range.upperBound, calendars: editableCalendars
        )

        return store.events(matching: predicate).map { ekEvent in
            AppleCalendarEvent(
                id: ekEvent.eventIdentifier ?? UUID().uuidString,
                title: ekEvent.title ?? "(제목 없음)",
                startDate: ekEvent.startDate,
                isAllDay: ekEvent.isAllDay
            )
        }
    }
}
