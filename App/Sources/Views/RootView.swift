import SwiftUI

struct RootView: View {
    @EnvironmentObject private var auth: AuthManager

    var body: some View {
        if auth.isSignedIn {
            CalendarView()
        } else {
            SignInView()
        }
    }
}
