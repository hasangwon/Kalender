import SwiftData
import SwiftUI
import WidgetKit

@main
struct PlanWidgetApp: App {
    @StateObject private var appleCalendar = AppleCalendarManager()

    private let container = ScheduleStore.makeContainer()

    var body: some Scene {
        WindowGroup {
            CalendarView()
                .environmentObject(appleCalendar)
                // 날짜/시간 피커의 AM/PM → 오전/오후 등 한국어 표기 고정
                .environment(\.locale, .korean)
                .onAppear {
                    ReviewRequester.recordFirstLaunchIfNeeded()
                    InterfaceStyle.apply()
                    NotificationManager.refresh(context: ModelContext(container))
                    #if DEBUG
                    // 위젯 기준 날짜(debug.widgetToday)를 바꾼 뒤 앱을 열면 바로 반영되게
                    WidgetCenter.shared.reloadAllTimelines()
                    #endif
                }
                .task {
                    // 애플 달력: 사용자가 켜둔 경우만 로드
                    appleCalendar.refreshAuthorization()
                    if SyncSettings.appleCalendarEnabled {
                        appleCalendar.loadEvents(around: .now)
                    }
                }
        }
        .modelContainer(container)
    }
}
