import Foundation

extension Calendar {
    /// 그 달 1일 0시
    func startOfMonth(for date: Date) -> Date {
        self.date(from: dateComponents([.year, .month], from: date)) ?? date
    }
}

extension Locale {
    /// 사용자 노출 날짜 포맷용 한국어 로케일
    static let korean = Locale(identifier: "ko_KR")
}
