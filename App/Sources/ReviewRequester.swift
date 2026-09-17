import Foundation

/// 앱 내 리뷰 요청 조건과 App Store 리뷰 링크를 한곳에서 관리한다.
///
/// iOS는 `requestReview`를 "요청"으로만 받아들이고 실제 노출 여부는 시스템이 정한다.
/// - 이미 현재 버전에 평점을 남긴 사용자에게는 표시하지 않는다
/// - 앱당 연 3회로 제한된다 (그 이상 호출해도 무시)
/// - 사용자가 설정에서 '앱 내 평가 및 리뷰'를 끄면 표시되지 않는다
///
/// 앱이 사용자의 리뷰 작성 여부를 조회할 방법은 없다. 그래서 여기서는
/// "버전당 한 번만 요청"해 위 연 3회를 낭비하지 않는 것까지만 책임진다.
enum ReviewRequester {
    /// App Store 앱 ID — 리뷰 작성 페이지 링크에 사용
    static let appStoreID = "6804972538"

    /// 리뷰 작성 화면을 바로 여는 링크
    static var writeReviewURL: URL? {
        URL(string: "https://apps.apple.com/app/id\(appStoreID)?action=write-review")
    }

    /// 요청 전 필요한 최소 일정 수 — 앱을 실제로 쓰고 있다는 신호
    private static let minimumScheduleCount = 3
    /// 설치 후 최소 경과 일수 — 설치 직후에는 앱을 평가할 근거가 없다
    private static let minimumDaysSinceFirstLaunch = 3

    private static let firstLaunchKey = "review.firstLaunchDate"
    private static let requestedVersionKey = "review.requestedVersion"

    private static var store: UserDefaults {
        UserDefaults(suiteName: SharedConstants.appGroupID) ?? .standard
    }

    private static var currentVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "-"
    }

    /// 앱 최초 실행 시각을 한 번만 기록한다.
    static func recordFirstLaunchIfNeeded(now: Date = .now) {
        guard store.object(forKey: firstLaunchKey) == nil else { return }

        store.set(now, forKey: firstLaunchKey)
    }

    /// 지금 리뷰를 요청해도 되는 상태인지 판단한다.
    static func shouldRequest(
        scheduleCount: Int,
        now: Date = .now,
        calendar: Calendar = .current
    ) -> Bool {
        guard scheduleCount >= minimumScheduleCount,
              store.string(forKey: requestedVersionKey) != currentVersion,
              let firstLaunch = store.object(forKey: firstLaunchKey) as? Date
        else { return false }

        let days = calendar.dateComponents([.day], from: firstLaunch, to: now).day ?? 0
        return days >= minimumDaysSinceFirstLaunch
    }

    /// 이번 버전에 대해 요청했음을 기록한다. 실제 노출 여부와 무관하게 한 번만 시도한다.
    static func markRequested() {
        store.set(currentVersion, forKey: requestedVersionKey)
    }
}
