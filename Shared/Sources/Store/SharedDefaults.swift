import Foundation

/// 앱·위젯이 함께 쓰는 설정 저장소 (App Group UserDefaults, 엔타이틀먼트 없으면 standard 폴백).
/// 인스턴스는 한 번만 만든다 — 달력 칸마다 색/테마를 읽으므로 매번 생성하면 그리기가 느려진다.
/// 값은 캐시하지 않으므로 설정 변경은 즉시 반영된다.
enum SharedDefaults {
    static let store: UserDefaults = UserDefaults(suiteName: SharedConstants.appGroupID) ?? .standard
}
