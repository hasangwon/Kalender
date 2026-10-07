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
        completion(context.isPreview ? .placeholder : loadEntry(family: context.family))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ScheduleEntry>) -> Void) {
        let entry = loadEntry(family: context.family)
        let calendar = Calendar.current

        // 자정에 갱신 (일정 변경 시에는 앱이 reloadAllTimelines 호출)
        let nextMidnight = calendar.nextDate(
            after: .now,
            matching: DateComponents(hour: 0, minute: 0, second: 5),
            matchingPolicy: .nextTime
        ) ?? calendar.date(byAdding: .hour, value: 1, to: .now) ?? .now.addingTimeInterval(3600)

        completion(Timeline(entries: [entry], policy: .after(nextMidnight)))
    }

    /// 위젯 크기별로 필요한 날짜만 계산한다 — Small: 오늘, Medium: 오늘+7일, Large: 이번 달 그리드
    private func loadEntry(family: WidgetFamily) -> ScheduleEntry {
        let calendar = Calendar.current
        let now = Date.now

        // 내일부터 7일 (Medium에서만 표시)
        let upcomingDays = family == .systemMedium
            ? (1...7).compactMap { calendar.date(byAdding: .day, value: $0, to: now) }
            : []

        // 이번 달 그리드 (Large에서만 표시, 월 제목·빈 칸 수는 가벼워서 항상 계산)
        let monthStart = calendar.startOfMonth(for: now)
        let dayRange = calendar.range(of: .day, in: .month, for: monthStart) ?? 1..<31
        let leadingBlanks = calendar.component(.weekday, from: monthStart) - 1
        let monthDays: [(number: Int, date: Date)] = family == .systemLarge
            ? dayRange.compactMap { number in
                calendar.date(byAdding: .day, value: number - 1, to: monthStart).map { (number, $0) }
            }
            : []

        let days = [now] + upcomingDays + monthDays.map(\.date)
        let (schedules, anniversaries) = fetchData(covering: days, calendar: calendar)

        // 필요한 날짜를 모아 한 번에 계산 — 날짜마다 전체 일정을 훑지 않는다
        let eventsByDay = DayEventResolver.eventsByDay(
            schedules: schedules, anniversaries: anniversaries,
            days: days, calendar: calendar
        )
        func events(on day: Date) -> [DayEvent] {
            eventsByDay[calendar.startOfDay(for: day)] ?? []
        }

        let today = events(on: now)

        // 일정이 있는 날만
        let upcoming: [WidgetDayGroup] = upcomingDays.compactMap { day in
            let items = events(on: day)
            guard !items.isEmpty else { return nil }

            return WidgetDayGroup(date: day, items: items)
        }

        let monthCells: [MonthCell] = monthDays.map { dayNumber, day in
            let events = events(on: day)
            let firstVisible = events.first { $0.kind != .holiday } ?? events.first

            return MonthCell(
                day: dayNumber,
                isToday: calendar.isDateInToday(day),
                isHoliday: events.contains { $0.kind == .holiday }
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
                .dateTime.month(.wide).locale(.korean)
            ),
            leadingBlanks: leadingBlanks,
            monthCells: monthCells
        )
    }

    /// 표시 기간에 나올 수 있는 일정만 읽는다 — 단일 일정은 기간 안의 것만, 반복 일정은 판정에 맡긴다
    private func fetchData(
        covering days: [Date],
        calendar: Calendar
    ) -> (schedules: [Schedule], anniversaries: [AnniversaryEntry]) {
        let context = ModelContext(ScheduleStore.makeContainer())
        let starts = days.map { calendar.startOfDay(for: $0) }
        let rangeStart = starts.min() ?? calendar.startOfDay(for: .now)
        let lastDay = starts.max() ?? rangeStart
        let rangeEnd = calendar.date(byAdding: .day, value: 1, to: lastDay) ?? lastDay
        let singleRaw = Recurrence.none.rawValue

        let descriptor = FetchDescriptor<Schedule>(predicate: #Predicate { schedule in
            schedule.recurrenceRaw != singleRaw
                || (schedule.startDate >= rangeStart && schedule.startDate < rangeEnd)
        })
        let schedules = (try? context.fetch(descriptor)) ?? []
        let anniversaries = (try? context.fetch(FetchDescriptor<AnniversaryEntry>())) ?? []
        return (schedules, anniversaries)
    }
}
