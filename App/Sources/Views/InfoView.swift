import SwiftUI

/// 정보 화면 — 앱 정보.
struct InfoView: View {
    @Environment(\.dismiss) private var dismiss

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

            infoRow("앱 이름", value: "Kalender")
            infoRow("버전", value: "1.0.0")
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }

    private func infoRow(_ title: String, value: String) -> some View {
        HStack {
            Text(title).font(.subheadline).foregroundStyle(.secondary)
            Spacer()
            Text(value).font(.subheadline.weight(.semibold))
        }
    }
}
