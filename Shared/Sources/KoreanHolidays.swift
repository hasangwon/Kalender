import Foundation

/// 한국 공휴일.
/// - 2025~2027: 확정 공고 기준 수동 테이블 (임시공휴일·선거일 포함)
/// - 그 외 연도: 현행 규정 기준 자동 계산 (고정 공휴일 + 음력 환산 + 대체공휴일)
///   ⚠️ 자동 계산분은 임시공휴일/선거일을 알 수 없고, 음력 환산이 드물게 1일 다를 수 있음.
enum KoreanHolidays {
    /// [연도: [(월, 일, 이름)]] — 확정 공고분
    private static let table: [Int: [(month: Int, day: Int, name: String)]] = [
        2025: [
            (1, 1, "신정"),
            (1, 27, "임시공휴일"),
            (1, 28, "설날 연휴"), (1, 29, "설날"), (1, 30, "설날 연휴"),
            (3, 1, "삼일절"), (3, 3, "대체공휴일"),
            (5, 5, "어린이날·부처님오신날"), (5, 6, "대체공휴일"),
            (6, 6, "현충일"),
            (8, 15, "광복절"),
            (10, 3, "개천절"),
            (10, 5, "추석 연휴"), (10, 6, "추석"), (10, 7, "추석 연휴"), (10, 8, "대체공휴일"),
            (10, 9, "한글날"),
            (12, 25, "성탄절"),
        ],
        2026: [
            (1, 1, "신정"),
            (2, 16, "설날 연휴"), (2, 17, "설날"), (2, 18, "설날 연휴"),
            (3, 1, "삼일절"), (3, 2, "대체공휴일"),
            (5, 5, "어린이날"),
            (5, 24, "부처님오신날"), (5, 25, "대체공휴일"),
            (6, 3, "지방선거일"),
            (6, 6, "현충일"),
            (8, 15, "광복절"), (8, 17, "대체공휴일"),
            (9, 24, "추석 연휴"), (9, 25, "추석"), (9, 26, "추석 연휴"),
            (10, 3, "개천절"), (10, 5, "대체공휴일"),
            (10, 9, "한글날"),
            (12, 25, "성탄절"),
        ],
        2027: [
            (1, 1, "신정"),
            (2, 6, "설날 연휴"), (2, 7, "설날"), (2, 8, "설날 연휴"), (2, 9, "대체공휴일"),
            (3, 1, "삼일절"),
            (5, 5, "어린이날"),
            (5, 13, "부처님오신날"),
            (6, 6, "현충일"),
            (8, 15, "광복절"), (8, 16, "대체공휴일"),
            (9, 14, "추석 연휴"), (9, 15, "추석"), (9, 16, "추석 연휴"),
            (10, 3, "개천절"), (10, 4, "대체공휴일"),
            (10, 9, "한글날"), (10, 11, "대체공휴일"),
            (12, 25, "성탄절"), (12, 27, "대체공휴일"),
        ],
    ]

    private static var computedCache: [Int: [(month: Int, day: Int, name: String)]] = [:]
    private static let cacheLock = NSLock()

    static func holidays(
        forYear year: Int,
        calendar: Calendar = .current
    ) -> [(month: Int, day: Int, name: String)] {
        if let fixed = table[year] { return fixed }

        cacheLock.lock()
        defer { cacheLock.unlock() }

        if let cached = computedCache[year] { return cached }

        let computed = computeHolidays(forYear: year, calendar: calendar)
        computedCache[year] = computed
        return computed
    }

    /// 해당 날짜의 공휴일 이름 (없으면 nil)
    static func holidayName(on date: Date, calendar: Calendar = .current) -> String? {
        let parts = calendar.dateComponents([.year, .month, .day], from: date)

        guard let year = parts.year, let month = parts.month, let day = parts.day else { return nil }

        return holidays(forYear: year, calendar: calendar)
            .first { $0.month == month && $0.day == day }?
            .name
    }

    static func isHoliday(_ date: Date, calendar: Calendar = .current) -> Bool {
        holidayName(on: date, calendar: calendar) != nil
    }

    // MARK: - 규정 기반 자동 계산

    private static func computeHolidays(
        forYear year: Int,
        calendar: Calendar
    ) -> [(month: Int, day: Int, name: String)] {
        var holidayMap: [Date: String] = [:]

        func dayDate(_ month: Int, _ day: Int) -> Date? {
            calendar.date(from: DateComponents(year: year, month: month, day: day))
                .map(calendar.startOfDay(for:))
        }

        func register(_ date: Date?, _ name: String) {
            guard let date, holidayMap[date] == nil else { return }
            holidayMap[date] = name
        }

        // 고정 공휴일
        register(dayDate(1, 1), "신정")
        register(dayDate(3, 1), "삼일절")
        register(dayDate(5, 5), "어린이날")
        register(dayDate(6, 6), "현충일")
        register(dayDate(8, 15), "광복절")
        register(dayDate(10, 3), "개천절")
        register(dayDate(10, 9), "한글날")
        register(dayDate(12, 25), "성탄절")

        // 음력 공휴일 (양력 환산)
        let seollal = Lunar.solarDate(inYear: year, lunarMonth: 1, lunarDay: 1, calendar: calendar)
        let buddhaDay = Lunar.solarDate(inYear: year, lunarMonth: 4, lunarDay: 8, calendar: calendar)
        let chuseok = Lunar.solarDate(inYear: year, lunarMonth: 8, lunarDay: 15, calendar: calendar)

        if let seollal {
            register(calendar.date(byAdding: .day, value: -1, to: seollal), "설날 연휴")
            register(seollal, "설날")
            register(calendar.date(byAdding: .day, value: 1, to: seollal), "설날 연휴")
        }
        register(buddhaDay, "부처님오신날")
        if let chuseok {
            register(calendar.date(byAdding: .day, value: -1, to: chuseok), "추석 연휴")
            register(chuseok, "추석")
            register(calendar.date(byAdding: .day, value: 1, to: chuseok), "추석 연휴")
        }

        // 대체공휴일
        var substitutes: [Date] = []

        func isWeekend(_ date: Date) -> Bool {
            let weekday = calendar.component(.weekday, from: date)
            return weekday == 1 || weekday == 7
        }

        /// 기준일 다음의 첫 평일(기존 공휴일/이미 배정된 대체일 제외)
        func nextSubstituteDay(after date: Date) -> Date? {
            var candidate = date

            for _ in 0..<10 {
                guard let next = calendar.date(byAdding: .day, value: 1, to: candidate) else { return nil }
                candidate = next

                if !isWeekend(candidate),
                   holidayMap[candidate] == nil,
                   !substitutes.contains(candidate) {
                    return candidate
                }
            }

            return nil
        }

        // 토/일 겹침 시 대체 (어린이날·국경일·부처님오신날·성탄절. 신정·현충일은 제외)
        let singleSubstitutable: [Date?] = [
            dayDate(3, 1), dayDate(5, 5), buddhaDay,
            dayDate(8, 15), dayDate(10, 3), dayDate(10, 9), dayDate(12, 25),
        ]

        for case let holiday? in singleSubstitutable where isWeekend(holiday) {
            if let substitute = nextSubstituteDay(after: holiday) {
                substitutes.append(substitute)
            }
        }

        // 설/추석 연휴는 일요일 겹침 시 대체
        for center in [seollal, chuseok] {
            guard let center else { continue }

            let holidayRun = [-1, 0, 1].compactMap {
                calendar.date(byAdding: .day, value: $0, to: center)
            }
            let overlapsSunday = holidayRun.contains {
                calendar.component(.weekday, from: $0) == 1
            }

            if overlapsSunday, let last = holidayRun.last,
               let substitute = nextSubstituteDay(after: last) {
                substitutes.append(substitute)
            }
        }

        for substitute in substitutes {
            holidayMap[substitute] = "대체공휴일"
        }

        return holidayMap
            .sorted { $0.key < $1.key }
            .map { date, name in
                let parts = calendar.dateComponents([.month, .day], from: date)
                return (parts.month ?? 1, parts.day ?? 1, name)
            }
    }
}
