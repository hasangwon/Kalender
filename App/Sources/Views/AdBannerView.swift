import SwiftUI

/// 하단 배너 광고 자리.
/// 지금은 placeholder — 실제로는 여기에 AdMob 배너(GADBannerView)를 넣습니다.
/// - 광고 제거를 구매하면(adsRemoved) 렌더링되지 않음
/// - AdConfig.enabled 로 전역 on/off (AdMob 연동 전까지 false 권장)
enum AdConfig {
    /// AdMob SDK 연동 + 광고 단위 ID 설정이 끝나면 실제 배너로 교체.
    /// true면 배너 공간(placeholder)을 확보한다.
    static let enabled = true

    // AdMob 연동 시 사용할 값 (App은 GADApplicationIdentifier를 Info.plist에도 넣어야 함)
    static let bannerUnitID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
}

struct AdBannerView: View {
    let adsRemoved: Bool

    var body: some View {
        if AdConfig.enabled && !adsRemoved {
            // TODO: AdMob 연동 시 이 placeholder를 GADBannerView 래퍼로 교체
            HStack(spacing: 6) {
                Image(systemName: "megaphone")
                    .font(.caption2)
                Text("광고")
                    .font(.caption2.weight(.semibold))
            }
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(Color.primary.opacity(0.05))
            .overlay(alignment: .top) {
                Rectangle().fill(Color.primary.opacity(0.08)).frame(height: 0.5)
            }
        }
    }
}
