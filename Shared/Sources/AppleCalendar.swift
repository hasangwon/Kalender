import Foundation

/// 애플 기본 달력에서 읽어온 일정 (읽기 전용, 수정 불가)
struct AppleCalendarEvent: Identifiable {
    let id: String
    let title: String
    let startDate: Date
    let isAllDay: Bool
    /// 캘린더 색 (기본 달력 앱의 캘린더별 색)
    let colorHex: String?

    func occurs(on day: Date, calendar: Calendar = .current) -> Bool {
        calendar.isDate(startDate, inSameDayAs: day)
    }

    var timeText: String? {
        guard !isAllDay else { return nil }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "a h:mm"
        return formatter.string(from: startDate)
    }
}
