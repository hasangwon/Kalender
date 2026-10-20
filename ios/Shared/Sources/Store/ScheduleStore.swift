import Foundation
import SwiftData

enum ScheduleStore {
    /// 앱/위젯 공용 SwiftData 컨테이너.
    /// 우선순위: App Group + iCloud(CloudKit) → App Group(로컬) → 로컬.
    /// iCloud 엔타이틀먼트/계정이 없으면(무료 계정·시뮬레이터 등) 자동으로 로컬 저장으로 폴백합니다.
    static func makeContainer() -> ModelContainer {
        let schema = Schema([Schedule.self, AnniversaryEntry.self])

        let hasAppGroup = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: SharedConstants.appGroupID) != nil

        if hasAppGroup {
            // 1) App Group + CloudKit — 엔타이틀먼트 존재 플래그 AND 사용자가 켰을 때만.
            //    엔타이틀먼트 없이 CloudKit 을 요청하면 실기기에서 크래시하므로 플래그로 막는다.
            if SharedConstants.iCloudSyncEnabled && SyncSettings.iCloudEnabled {
                let cloudConfig = ModelConfiguration(
                    schema: schema,
                    groupContainer: .identifier(SharedConstants.appGroupID),
                    cloudKitDatabase: .private(SharedConstants.iCloudContainerID)
                )
                if let container = try? ModelContainer(for: schema, configurations: [cloudConfig]) {
                    return container
                }
            }

            // 2) App Group만 (동기화 없이 앱↔위젯 공유)
            let groupConfig = ModelConfiguration(
                schema: schema,
                groupContainer: .identifier(SharedConstants.appGroupID)
            )
            if let container = try? ModelContainer(for: schema, configurations: [groupConfig]) {
                return container
            }
        }

        // 3) 로컬 (개발 초기·엔타이틀먼트 없음)
        let localConfig = ModelConfiguration(schema: schema)

        do {
            return try ModelContainer(for: schema, configurations: [localConfig])
        } catch {
            fatalError("SwiftData 컨테이너 생성 실패: \(error)")
        }
    }

    /// 특정 날짜에 발생하는 일정 (종일 → 시간순 정렬)
    static func occurrences(
        in schedules: [Schedule],
        on day: Date,
        calendar: Calendar = .current
    ) -> [Schedule] {
        schedules
            .filter { $0.occurs(on: day, calendar: calendar) }
            .sorted { lhs, rhs in
                switch (lhs.hasTime, rhs.hasTime) {
                case (false, true): return true
                case (true, false): return false
                case (true, true):
                    let lhsMinutes = calendar.component(.hour, from: lhs.startDate) * 60
                        + calendar.component(.minute, from: lhs.startDate)
                    let rhsMinutes = calendar.component(.hour, from: rhs.startDate) * 60
                        + calendar.component(.minute, from: rhs.startDate)
                    if lhsMinutes != rhsMinutes { return lhsMinutes < rhsMinutes }
                    return lhs.title < rhs.title
                case (false, false):
                    return lhs.title < rhs.title
                }
            }
    }

    /// 여러 날짜에 나올 수 있는 일정 후보를 날짜별로 (키: 그 날의 startOfDay).
    /// 날짜마다 전체 일정을 훑으면 날짜 수 × 일정 수만큼 판정이 돌기 때문에, 맞을 수 있는 날에만 넣는다.
    /// 최종 발생 판정은 호출하는 쪽에서 `occurrences(in:on:)` → `Schedule.occurs`로 다시 한다 ("후보 줄이기"일 뿐).
    static func candidatesByDay(
        in schedules: [Schedule],
        days: [Date],
        calendar: Calendar = .current
    ) -> [Date: [Schedule]] {
        let sortedDays = Set(days.map { calendar.startOfDay(for: $0) }).sorted()
        let dayStarts = Set(sortedDays)

        // 날짜별 요일·일·말일 여부를 한 번만 계산해 둔다
        let dayInfos = sortedDays.map { day in
            DayInfo(
                date: day,
                weekday: calendar.component(.weekday, from: day),
                dayOfMonth: calendar.component(.day, from: day),
                isLastDayOfMonth: calendar.range(of: .day, in: .month, for: day)?.count
                    == calendar.component(.day, from: day)
            )
        }
        let rangeStart = sortedDays.first ?? .distantPast
        let rangeEnd = sortedDays.last ?? .distantFuture

        var candidates: [Date: [Schedule]] = [:]
        for schedule in schedules {
            let start = calendar.startOfDay(for: schedule.startDate)
            switch schedule.recurrence {
            case .none:
                if dayStarts.contains(start) {
                    candidates[start, default: []].append(schedule)
                }
            case .weekly, .monthly:
                // 표시 기간과 겹치지 않는 반복(이미 끝났거나 아직 시작 전)은 건너뛴다
                let end = schedule.endDate.map { calendar.startOfDay(for: $0) }
                if start > rangeEnd { continue }
                if let end, end < rangeStart { continue }

                let startWeekday = calendar.component(.weekday, from: start)
                let startDay = calendar.component(.day, from: start)
                for info in dayInfos where info.date >= start && (end.map { info.date <= $0 } ?? true) {
                    let matches: Bool
                    switch schedule.recurrence {
                    case .weekly: matches = info.weekday == startWeekday
                    case .monthly: matches = schedule.monthlyOnLastDay ? info.isLastDayOfMonth : info.dayOfMonth == startDay
                    case .none: matches = false
                    }
                    if matches {
                        candidates[info.date, default: []].append(schedule)
                    }
                }
            }
        }

        return candidates
    }

    /// candidatesByDay용 날짜별 사전 계산 값
    private struct DayInfo {
        let date: Date
        let weekday: Int
        let dayOfMonth: Int
        let isLastDayOfMonth: Bool
    }
}
