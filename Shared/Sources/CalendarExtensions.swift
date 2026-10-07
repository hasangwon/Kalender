import Foundation

extension Calendar {
    /// 그 달 1일 0시
    func startOfMonth(for date: Date) -> Date {
        self.date(from: dateComponents([.year, .month], from: date)) ?? date
    }
}
