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

    /// 달력 셀 도트용 색 (기념일+일정, 최대 3개 — 공휴일은 도트가 아니라 숫자색으로 표현)
    static func dotColors(
        schedules: [Schedule],
        anniversaries: [AnniversaryEntry],
        on day: Date,
        calendar: Calendar = .current
    ) -> [ColorTag] {
        events(schedules: schedules, anniversaries: anniversaries, on: day, calendar: calendar)
            .compactMap(\.colorTag)
            .prefix(3)
            .map { $0 }
    }
}
