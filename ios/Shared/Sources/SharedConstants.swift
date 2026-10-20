import Foundation

enum SharedConstants {
    /// 앱 ↔ 위젯이 데이터를 공유하는 App Group ID.
    /// Apple Developer 계정의 실제 그룹 ID로 바꿔야 하면 여기 한 곳만 수정하면 됩니다.
    static let appGroupID = "group.com.hasangwon.planwidget"

    /// iCloud(CloudKit) 컨테이너 ID. 유료 개발자 계정에서 이 ID로 컨테이너를 만들어야 실제 동기화됩니다.
    static let iCloudContainerID = "iCloud.com.hasangwon.planwidget"

    /// iCloud 동기화 활성화 플래그.
    /// ⚠️ project.yml 의 iCloud 엔타이틀먼트와 반드시 함께 켜야 함.
    ///    엔타이틀먼트 없이 true 로 두면 실기기에서 앱이 크래시합니다.
    ///    유료 계정 승인 후: 엔타이틀먼트 주석 해제 + 이 값을 true 로.
    static let iCloudSyncEnabled = true
}
