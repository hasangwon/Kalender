import Foundation
import SwiftData
import WidgetKit

/// Large 달력 위젯의 셀 하나 (nil이면 빈 칸)
struct MonthCell: Identifiable {
    let day: Int
    let isToday: Bool
    let isHoliday: Bool
    let title: String?
    let colorTag: ColorTag?
    let extraCount: Int

    var id: Int { day }
}

struct WidgetDayGroup: Identifiable {
    let date: Date
    let items: [DayEvent]

    var id: Date { date }
}

struct ScheduleEntry: TimelineEntry {
    let date: Date
    let today: [DayEvent]
    let upcoming: [WidgetDayGroup]
    let monthTitle: String
    /// 요일 정렬용 선행 빈 칸 수 + 월의 셀들
    let leadingBlanks: Int
    let monthCells: [MonthCell]

    static let placeholder = ScheduleEntry(
        date: .now,
        today: [
            DayEvent(id: "p1", kind: .schedule, title: "팀 회의", colorTag: .blue, timeText: "오전 10:00"),
            DayEvent(id: "p2", kind: .schedule, title: "운동", colorTag: .green, timeText: nil),
        ],
        upcoming: [],
        monthTitle: "8월",
        leadingBlanks: 0,
        monthCells: (1...30).map {
            MonthCell(day: $0, isToday: $0 == 15, isHoliday: false, title: nil, colorTag: nil, extraCount: 0)
        }
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
        let anniversaries = (try? context.fetch(FetchDescriptor<AnniversaryEntry>())) ?? []

        let today = DayEventResolver.events(
            schedules: schedules, anniversaries: anniversaries, on: now, calendar: calendar
        )

        // 내일부터 7일간, 일정이 있는 날만
        let upcoming: [WidgetDayGroup] = (1...7).compactMap { offset in
            guard let day = calendar.date(byAdding: .day, value: offset, to: now) else { return nil }

            let items = DayEventResolver.events(
                schedules: schedules, anniversaries: anniversaries, on: day, calendar: calendar
            )
            guard !items.isEmpty else { return nil }

            return WidgetDayGroup(date: day, items: items)
        }

        // Large 달력 그리드 (이번 달)
        let monthStart = calendar.date(from: calendar.dateComponents([.year, .month], from: now)) ?? now
        let dayRange = calendar.range(of: .day, in: .month, for: monthStart) ?? 1..<31
        let leadingBlanks = calendar.component(.weekday, from: monthStart) - 1

        let monthCells: [MonthCell] = dayRange.compactMap { dayNumber in
            guard let day = calendar.date(byAdding: .day, value: dayNumber - 1, to: monthStart) else {
                return nil
            }

            let events = DayEventResolver.events(
                schedules: schedules, anniversaries: anniversaries, on: day, calendar: calendar
            )
            let firstVisible = events.first { $0.kind != .holiday } ?? events.first

            return MonthCell(
                day: dayNumber,
                isToday: calendar.isDateInToday(day),
                isHoliday: KoreanHolidays.isHoliday(day, calendar: calendar)
                    || calendar.component(.weekday, from: day) == 1
                    || calendar.component(.weekday, from: day) == 7,
                title: firstVisible?.title,
                colorTag: firstVisible?.colorTag,
                extraCount: firstVisible == nil ? 0 : max(events.count - 1, 0)
            )
        }

        return ScheduleEntry(
            date: now,
            today: today,
            upcoming: upcoming,
            monthTitle: monthStart.formatted(
                .dateTime.month(.wide).locale(Locale(identifier: "ko_KR"))
            ),
            leadingBlanks: leadingBlanks,
            monthCells: monthCells
        )
    }
}
