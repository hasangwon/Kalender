import SwiftUI

/// 정보 화면 — 앱 정보.
struct InfoView: View {
    @Environment(\.openURL) private var openURL

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    appInfoCard
                }
                .padding(16)
            }
            .background(AppTheme.background)
            .fontDesign(.rounded)
            .navigationTitle("정보")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    SheetCloseButton()
                }
            }
            .toolbarBackground(AppTheme.background, for: .navigationBar)
        }
    }

    // MARK: - 앱 정보

    private var appInfoCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("앱 정보")
                .cardTitleFont()

            infoRow("앱 이름", value: "하상원의 달력")
            infoRow("만든 사람", value: "하상원")
            infoRow("버전", value: Self.appVersion)

            Divider()

            reviewButton
        }
        .cardStyle()
    }

    /// App Store 리뷰 작성 화면으로 바로 이동
    private var reviewButton: some View {
        Button {
            guard let url = ReviewRequester.writeReviewURL else { return }

            openURL(url)
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "star.fill")
                    .font(.footnote)
                Text("리뷰 남기기")
                    .font(.subheadline.weight(.semibold))
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(.tertiary)
            }
            .foregroundStyle(AppTheme.primary)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    /// 번들에서 읽은 표시용 버전 (예: "1.1.0 (6)")
    private static var appVersion: String {
        let info = Bundle.main.infoDictionary
        let short = info?["CFBundleShortVersionString"] as? String ?? "-"
        guard let build = info?["CFBundleVersion"] as? String, build != short else {
            return short
        }

        return "\(short) (\(build))"
    }

    private func infoRow(_ title: String, value: String) -> some View {
        HStack {
            Text(title).font(.subheadline).foregroundStyle(.secondary)
            Spacer()
            Text(value).font(.subheadline.weight(.semibold))
        }
    }
}
