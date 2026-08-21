import SwiftData
import SwiftUI

@main
struct PlanWidgetApp: App {
    private let container = ScheduleStore.makeContainer()

    var body: some Scene {
        WindowGroup {
            CalendarView()
        }
        .modelContainer(container)
    }
}
