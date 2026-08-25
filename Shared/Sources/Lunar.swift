import Foundation

/// 음력(중국식 태음태양력) 변환 유틸.
/// iOS 내장 chinese 캘린더 사용 — 한국 천문연구원 기준과 드물게 1일 차이가 날 수 있음.
enum Lunar {
    private static let chineseCalendar: Calendar = {
        var calendar = Calendar(identifier: .chinese)
        calendar.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        return calendar
    }()

    struct Components {
        let month: Int
        let day: Int
        let isLeapMonth: Bool
    }

    static func components(from date: Date) -> Components {
        let parts = chineseCalendar.dateComponents([.month, .day], from: date)
        return Components(
            month: parts.month ?? 1,
            day: parts.day ?? 1,
            isLeapMonth: parts.isLeapMonth ?? false
        )
    }

    /// "음력 7월 9일" (윤달이면 "음력 윤7월 9일")
    static func text(for date: Date) -> String {
        let parts = components(from: date)
        let leapPrefix = parts.isLeapMonth ? "윤" : ""
        return "음력 \(leapPrefix)\(parts.month)월 \(parts.day)일"
    }

    /// 해당 양력 날짜가 주어진 음력 월/일인지 (윤달 제외)
    static func matches(date: Date, month: Int, day: Int) -> Bool {
        let parts = components(from: date)
        return !parts.isLeapMonth && parts.month == month && parts.day == day
    }

    /// 주어진 연도에서 음력 월/일에 해당하는 양력 날짜 (없으면 nil)
    static func solarDate(
        inYear year: Int,
        lunarMonth: Int,
        lunarDay: Int,
        calendar: Calendar = .current
    ) -> Date? {
        guard let yearStart = calendar.date(from: DateComponents(year: year, month: 1, day: 1)) else {
            return nil
        }

        for offset in 0..<366 {
            guard let candidate = calendar.date(byAdding: .day, value: offset, to: yearStart),
                  calendar.component(.year, from: candidate) == year
            else { break }

            if matches(date: candidate, month: lunarMonth, day: lunarDay) {
                return candidate
            }
        }

        return nil
    }
}
