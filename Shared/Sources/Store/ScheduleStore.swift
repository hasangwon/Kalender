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
}
