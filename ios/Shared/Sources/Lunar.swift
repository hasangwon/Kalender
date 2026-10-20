import Foundation

/// 음력(중국식 태음태양력) 변환 유틸.
/// iOS 내장 chinese 캘린더 사용 — 한국 천문연구원 기준과 드물게 1일 차이가 날 수 있음.
enum Lunar {
    private static let chineseCalendar: Calendar = {
        var calendar = Calendar(identifier: .chinese)
        calendar.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        return calendar
    }()

    private struct Components {
        let month: Int
        let day: Int
        let isLeapMonth: Bool
    }

    /// chinese 캘린더 변환은 비싸다. 달력 그리드·기념일 판정·양력 환산이 같은 날짜를 반복해서 묻기 때문에
    /// 결과를 날짜별로 기억해 둔다 (변환 결과는 날짜가 같으면 항상 같다).
    nonisolated(unsafe) private static var componentsCache: [Date: Components] = [:]
    private static let cacheLock = NSLock()
    /// 메모리 상한 — 넘으면 비우고 다시 채운다 (몇 년치 날짜 정도)
    private static let cacheLimit = 4000

    private static func components(from date: Date) -> Components {
        cacheLock.lock()
        defer { cacheLock.unlock() }

        if let cached = componentsCache[date] { return cached }

        let parts = chineseCalendar.dateComponents([.month, .day], from: date)
        let result = Components(
            month: parts.month ?? 1,
            day: parts.day ?? 1,
            isLeapMonth: parts.isLeapMonth ?? false
        )
        if componentsCache.count >= cacheLimit {
            componentsCache.removeAll(keepingCapacity: true)
        }
        componentsCache[date] = result
        return result
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
