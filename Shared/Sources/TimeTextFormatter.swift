import Foundation

/// 일정 시간 표시("오후 3:30")용 공용 포매터.
/// DateFormatter 생성은 비싸서 달력 그리드처럼 수백 번 그릴 때 매번 만들면 프레임이 떨어진다.
enum TimeTextFormatter {
    static let shared: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "a h:mm"
        return formatter
    }()
}
