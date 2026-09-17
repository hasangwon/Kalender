import SwiftUI

/// 정보 화면 — 앱 정보.
struct InfoView: View {
    @Environment(\.dismiss) private var dismiss
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
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.footnote.weight(.bold))
                            .foregroundStyle(.secondary)
                            .frame(width: 30, height: 30)
                            .background(Color.primary.opacity(0.05), in: Circle())
                    }
                }
            }
            .toolbarBackground(AppTheme.background, for: .navigationBar)
        }
    }

    // MARK: - 앱 정보

    private var appInfoCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("앱 정보")
                .font(.system(.headline, design: .rounded).weight(.bold))

            infoRow("앱 이름", value: "하상원의 달력")
            infoRow("만든 사람", value: "장인 하상원")
            infoRow("버전", value: Self.appVersion)

            Divider()

            reviewButton
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
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
