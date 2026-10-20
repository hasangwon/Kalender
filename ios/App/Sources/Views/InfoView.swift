import SwiftUI

/// 정보 화면 — 앱 정보, 실험실.
struct InfoView: View {
    @Environment(\.openURL) private var openURL
    @State private var showsTodayLabel = LabSettings.showsTodayLabel

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    appInfoCard
                    labCard
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

    // MARK: - 실험실

    private var labCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text("실험실")
                    .cardTitleFont()
                Text("다듬는 중인 기능이에요. 처음에는 모두 꺼져 있어요.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            VStack(alignment: .leading, spacing: 6) {
                Toggle(isOn: $showsTodayLabel) {
                    Text("오늘 표시")
                        .font(.subheadline.weight(.bold))
                }
                .tint(AppTheme.primary)
                .onChange(of: showsTodayLabel) { _, newValue in
                    LabSettings.showsTodayLabel = newValue
                }

                Text("달력에서 오늘 날짜 옆에 \u{2018}오늘\u{2019}을 작게 표시해요.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Divider()

            // 이메일은 눌러서 바로 메일 앱으로
            Text("좋은 아이디어가 있다면 개발자 이메일\n([305243@naver.com](mailto:305243@naver.com))로 문의주세요.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .tint(AppTheme.primary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
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
