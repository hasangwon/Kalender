import AuthenticationServices
import Foundation
import SwiftUI

/// Apple 로그인 상태 관리.
/// 서버 없이 기기 로컬에 사용자 식별자만 보관하는 구조입니다.
@MainActor
final class AuthManager: ObservableObject {
    private enum Keys {
        static let userID = "auth.appleUserID"
        static let displayName = "auth.displayName"
        static let isGuest = "auth.isGuest"
    }

    @Published private(set) var userID: String?
    @Published private(set) var displayName: String
    @Published private(set) var isGuest: Bool

    private let defaults = UserDefaults.standard

    init() {
        userID = defaults.string(forKey: Keys.userID)
        displayName = defaults.string(forKey: Keys.displayName) ?? ""
        isGuest = defaults.bool(forKey: Keys.isGuest)
    }

    var isSignedIn: Bool { userID != nil }

    func handleSignIn(result: Result<ASAuthorization, Error>) {
        guard case .success(let authorization) = result,
              let credential = authorization.credential as? ASAuthorizationAppleIDCredential
        else { return }

        // 이름은 최초 로그인 1회만 내려오므로 그때 저장
        var name = displayName
        if let fullName = credential.fullName {
            let formatter = PersonNameComponentsFormatter()
            let formatted = formatter.string(from: fullName)
            if !formatted.isEmpty { name = formatted }
        }

        save(userID: credential.user, displayName: name, isGuest: false)
    }

    func signInAsGuest() {
        save(userID: "guest", displayName: "게스트", isGuest: true)
    }

    func signOut() {
        defaults.removeObject(forKey: Keys.userID)
        defaults.removeObject(forKey: Keys.displayName)
        defaults.removeObject(forKey: Keys.isGuest)
        userID = nil
        displayName = ""
        isGuest = false
    }

    /// 앱 시작 시 Apple ID 자격 증명이 취소되었는지 확인
    func refreshCredentialState() {
        guard let userID, !isGuest else { return }

        ASAuthorizationAppleIDProvider().getCredentialState(forUserID: userID) { [weak self] state, _ in
            Task { @MainActor in
                if state == .revoked || state == .notFound {
                    self?.signOut()
                }
            }
        }
    }

    private func save(userID: String, displayName: String, isGuest: Bool) {
        defaults.set(userID, forKey: Keys.userID)
        defaults.set(displayName, forKey: Keys.displayName)
        defaults.set(isGuest, forKey: Keys.isGuest)
        self.userID = userID
        self.displayName = displayName
        self.isGuest = isGuest
    }
}
