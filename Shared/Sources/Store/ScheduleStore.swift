import Foundation
import SwiftData

enum ScheduleStore {
    /// 앱/위젯 공용 SwiftData 컨테이너.
    /// App Group 컨테이너를 우선 사용하고, 엔타이틀먼트가 없으면(개발 초기 등) 로컬 저장소로 폴백합니다.
    static func makeContainer() -> ModelContainer {
        let schema = Schema([Schedule.self])

        let hasAppGroup = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: SharedConstants.appGroupID) != nil

        if hasAppGroup {
            let groupConfig = ModelConfiguration(
                schema: schema,
                groupContainer: .identifier(SharedConstants.appGroupID)
            )

            if let container = try? ModelContainer(for: schema, configurations: [groupConfig]) {
                return container
            }
        }

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
