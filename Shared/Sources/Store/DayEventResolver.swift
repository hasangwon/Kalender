import Foundation

/// 하루치 표시 이벤트 (공휴일 + 기념일 + 일정 통합, 표시용 값 타입)
struct DayEvent: Identifiable {
    enum Kind {
        case holiday
        case anniversary
        case schedule
        case appleCalendar   // 애플 기본 달력 (읽기 전용)
    }

    let id: String
    let kind: Kind
    let title: String
    let colorTag: ColorTag?
    let timeText: String?
}

enum DayEventResolver {
    /// 해당 날짜의 전체 표시 이벤트 — 공휴일 → 기념일 → 일정(종일→시간순) 순서
    static func events(
        schedules: [Schedule],
        anniversaries: [AnniversaryEntry],
        appleEvents: [AppleCalendarEvent] = [],
        on day: Date,
        calendar: Calendar = .current
    ) -> [DayEvent] {
        var result: [DayEvent] = []

        if let holidayName = KoreanHolidays.holidayName(on: day, calendar: calendar) {
            result.append(DayEvent(
                id: "holiday-\(holidayName)",
                kind: .holiday,
                title: holidayName,
                colorTag: nil,
                timeText: nil
            ))
        }

        for anniversary in anniversaries where anniversary.occurs(on: day, calendar: calendar) {
            result.append(DayEvent(
                id: "anniversary-\(anniversary.id.uuidString)",
                kind: .anniversary,
                title: anniversary.name,
                colorTag: EventColorSettings.anniversaryColor,
                timeText: anniversary.isLunar ? anniversary.dateText : nil
            ))
        }

        for schedule in ScheduleStore.occurrences(in: schedules, on: day, calendar: calendar) {
            result.append(DayEvent(
                id: "schedule-\(schedule.id.uuidString)",
                kind: .schedule,
                title: schedule.title,
                colorTag: schedule.displayColor,
                timeText: schedule.timeText
            ))
        }

        for appleEvent in appleEvents where appleEvent.occurs(on: day, calendar: calendar) {
            result.append(DayEvent(
                id: "apple-\(appleEvent.id)",
                kind: .appleCalendar,
                title: appleEvent.title,
                colorTag: nil,
                timeText: appleEvent.timeText
            ))
        }

        return result
    }

    /// 여러 날짜의 이벤트를 한 번에 — 달력 그리드용 (키: 그 날의 startOfDay).
    /// 날짜마다 전체 일정을 훑으면 칸 수 × 일정 수만큼 판정이 돌아 화면이 멈춘다.
    /// 일정을 먼저 날짜별 후보로 나눠 두고, 발생 판정·정렬은 기존 `events(...)` 경로를 그대로 쓴다.
    static func eventsByDay(
        schedules: [Schedule],
        anniversaries: [AnniversaryEntry],
        appleEvents: [AppleCalendarEvent] = [],
        days: [Date],
        calendar: Calendar = .current
    ) -> [Date: [DayEvent]] {
        let sortedDays = Set(days.map { calendar.startOfDay(for: $0) }).sorted()
        let candidates = ScheduleStore.candidatesByDay(in: schedules, days: sortedDays, calendar: calendar)

        let appleByDay = Dictionary(grouping: appleEvents) { calendar.startOfDay(for: $0.startDate) }

        var result: [Date: [DayEvent]] = [:]
        for day in sortedDays {
            result[day] = events(
                schedules: candidates[day] ?? [],
                anniversaries: anniversaries,
                appleEvents: appleByDay[day] ?? [],
                on: day,
                calendar: calendar
            )
        }
        return result
    }
}
