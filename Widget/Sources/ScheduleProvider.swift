import Foundation
import SwiftData
import WidgetKit

/// 위젯에 전달되는 일정 스냅샷 (SwiftData 모델을 값 타입으로 변환)
struct WidgetScheduleItem: Identifiable {
    let id: UUID
    let title: String
    let colorTag: ColorTag
    let timeText: String?
}

struct WidgetDayGroup: Identifiable {
    let date: Date
    let items: [WidgetScheduleItem]

    var id: Date { date }
}

struct ScheduleEntry: TimelineEntry {
    let date: Date
    let today: [WidgetScheduleItem]
    let upcoming: [WidgetDayGroup]

    static let placeholder = ScheduleEntry(
        date: .now,
        today: [
            WidgetScheduleItem(id: UUID(), title: "팀 회의", colorTag: .blue, timeText: "오전 10:00"),
            WidgetScheduleItem(id: UUID(), title: "운동", colorTag: .green, timeText: nil),
        ],
        upcoming: []
    )
}

struct ScheduleProvider: TimelineProvider {
    func placeholder(in context: Context) -> ScheduleEntry {
        .placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (ScheduleEntry) -> Void) {
        completion(context.isPreview ? .placeholder : loadEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ScheduleEntry>) -> Void) {
        let entry = loadEntry()
        let calendar = Calendar.current

        // 자정에 갱신 (일정 변경 시에는 앱이 reloadAllTimelines 호출)
        let nextMidnight = calendar.nextDate(
            after: .now,
            matching: DateComponents(hour: 0, minute: 0, second: 5),
            matchingPolicy: .nextTime
        ) ?? calendar.date(byAdding: .hour, value: 1, to: .now)!

        completion(Timeline(entries: [entry], policy: .after(nextMidnight)))
    }

    private func loadEntry() -> ScheduleEntry {
        let calendar = Calendar.current
        let now = Date.now
        let container = ScheduleStore.makeContainer()
        let context = ModelContext(container)
        let schedules = (try? context.fetch(FetchDescriptor<Schedule>())) ?? []

        let today = ScheduleStore.occurrences(in: schedules, on: now, calendar: calendar)
            .map(makeItem)

        // 내일부터 7일간, 일정이 있는 날만
        let upcoming: [WidgetDayGroup] = (1...7).compactMap { offset in
            guard let day = calendar.date(byAdding: .day, value: offset, to: now) else { return nil }

            let items = ScheduleStore.occurrences(in: schedules, on: day, calendar: calendar)
            guard !items.isEmpty else { return nil }

            return WidgetDayGroup(date: day, items: items.map(makeItem))
        }

        return ScheduleEntry(date: now, today: today, upcoming: upcoming)
    }

    private func makeItem(_ schedule: Schedule) -> WidgetScheduleItem {
        WidgetScheduleItem(
            id: schedule.id,
            title: schedule.title,
            colorTag: schedule.color,
            timeText: schedule.timeText
        )
    }
}
