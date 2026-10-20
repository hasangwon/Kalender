import SwiftData
import WidgetKit

extension ModelContext {
    /// 일정/기념일 변경을 저장하고 위젯을 갱신한다 — 변경 코드는 모두 이 경로를 쓴다.
    /// 일정(Schedule)이 바뀌었으면 `refreshesNotifications`로 하루 일괄 알림도 다시 예약한다.
    func commitChanges(refreshesNotifications: Bool) {
        try? save()
        WidgetCenter.shared.reloadAllTimelines()
        if refreshesNotifications {
            NotificationManager.refresh(context: self)
        }
    }
}
