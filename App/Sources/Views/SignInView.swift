import AuthenticationServices
import SwiftUI

struct SignInView: View {
    @EnvironmentObject private var auth: AuthManager
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: 12) {
                Image(systemName: "calendar.badge.clock")
                    .font(.system(size: 64, weight: .medium))
                    .foregroundStyle(.tint)
                Text("일정위젯")
                    .font(.largeTitle.bold())
                Text("달력에서 일정을 관리하고\n홈 화면 위젯으로 바로 확인하세요")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }

            Spacer()

            VStack(spacing: 12) {
                SignInWithAppleButton(.signIn) { request in
                    request.requestedScopes = [.fullName]
                } onCompletion: { result in
                    auth.handleSignIn(result: result)
                }
                .signInWithAppleButtonStyle(colorScheme == .dark ? .white : .black)
                .frame(height: 52)
                .clipShape(RoundedRectangle(cornerRadius: 12))

                Button("로그인 없이 시작") {
                    auth.signInAsGuest()
                }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
                .padding(.vertical, 6)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
    }
}
