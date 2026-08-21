import SwiftData
import SwiftUI

@main
struct PlanWidgetApp: App {
    @StateObject private var auth = AuthManager()

    private let container = ScheduleStore.makeContainer()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(auth)
                .task { auth.refreshCredentialState() }
        }
        .modelContainer(container)
    }
}
