import SwiftData
import SwiftUI

@main
struct PlanWidgetApp: App {
    @StateObject private var appleCalendar = AppleCalendarManager()

    private let container = ScheduleStore.makeContainer()

    var body: some Scene {
        WindowGroup {
            CalendarView()
                .environmentObject(appleCalendar)
                // 날짜/시간 피커의 AM/PM → 오전/오후 등 한국어 표기 고정
                .environment(\.locale, Locale(identifier: "ko_KR"))
                .onAppear {
                    InterfaceStyle.apply()
                    NotificationManager.refresh(context: ModelContext(container))
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
