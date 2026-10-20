import Foundation
import SwiftData

/// 매년 반복 기념일 (생일, 결혼기념일 등).
/// - 양력: 매년 같은 양력 월/일에 표시
/// - 음력: 매년 음력 월/일을 양력으로 환산해 표시 (음력 생일 등)
// CloudKit 동기화 요건: @Attribute(.unique) 사용 불가, 모든 속성에 기본값 필요.
@Model
final class AnniversaryEntry {
    var id: UUID = UUID()
    var name: String = ""
    var month: Int = 1
    var day: Int = 1
    var isLunar: Bool = false
    var createdAt: Date = Date.now

    init(name: String, month: Int, day: Int, isLunar: Bool) {
        self.id = UUID()
        self.name = name
        self.month = month
        self.day = day
        self.isLunar = isLunar
        self.createdAt = .now
    }

    func occurs(on date: Date, calendar: Calendar = .current) -> Bool {
        if isLunar {
            return Lunar.matches(date: date, month: month, day: day)
        }

        return calendar.component(.month, from: date) == month
            && calendar.component(.day, from: date) == day
    }

    /// 등록 기준 표기 ("음력 7월 9일" / "매년 8월 21일")
    var dateText: String {
        isLunar ? "음력 \(month)월 \(day)일" : "매년 \(month)월 \(day)일"
    }

    /// 올해 실제 표시되는 양력 날짜
    func solarDate(inYear year: Int, calendar: Calendar = .current) -> Date? {
        if isLunar {
            return Lunar.solarDate(inYear: year, lunarMonth: month, lunarDay: day, calendar: calendar)
        }

        return calendar.date(from: DateComponents(year: year, month: month, day: day))
    }
}
