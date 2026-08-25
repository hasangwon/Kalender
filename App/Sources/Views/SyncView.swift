import SwiftUI
import WidgetKit

/// 동기화 설정 화면 — iCloud 동기화 + 애플 달력 연동 (각각 on/off)
struct SyncView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var appleCalendar: AppleCalendarManager

    @State private var iCloudEnabled = SyncSettings.iCloudEnabled
    @State private var appleCalendarEnabled = SyncSettings.appleCalendarEnabled
    @State private var showRestartNotice = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    iCloudCard
                    appleCalendarCard
                }
                .padding(16)
            }
            .background(AppTheme.background)
            .fontDesign(.rounded)
            .navigationTitle("동기화")
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

    // MARK: - iCloud 동기화

    private var iCloudCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Toggle(isOn: $iCloudEnabled) {
                HStack(spacing: 8) {
                    Image(systemName: "icloud")
                    Text("iCloud 동기화")
                        .font(.subheadline.weight(.bold))
                }
            }
            .tint(AppTheme.primary)
            .onChange(of: iCloudEnabled) { _, newValue in
                SyncSettings.iCloudEnabled = newValue
                showRestartNotice = true
                WidgetCenter.shared.reloadAllTimelines()
            }

            Text("같은 애플 계정으로 로그인한 기기끼리 일정을 함께 볼 수 있어요. (예: 내 아이폰 ↔ 내 아이패드)")
                .font(.caption)
                .foregroundStyle(.secondary)

            if showRestartNotice {
                Text("변경 사항은 앱을 완전히 껐다 켜면 적용됩니다.")
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(AppTheme.primary)
            }
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }

    // MARK: - 애플 달력 연동

    private var appleCalendarCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Toggle(isOn: $appleCalendarEnabled) {
                HStack(spacing: 8) {
                    Image(systemName: "calendar")
                    Text("달력 앱과 동기화")
                        .font(.subheadline.weight(.bold))
                }
            }
            .tint(AppTheme.primary)
            .onChange(of: appleCalendarEnabled) { _, newValue in
                SyncSettings.appleCalendarEnabled = newValue
                if newValue {
                    Task {
                        if !appleCalendar.isAuthorized {
                            await appleCalendar.requestAccess()
                        }
                        appleCalendar.loadEvents(around: .now)
                        // 권한이 거부되면 토글을 되돌림
                        if !appleCalendar.isAuthorized {
                            appleCalendarEnabled = false
                            SyncSettings.appleCalendarEnabled = false
                        }
                    }
                } else {
                    appleCalendar.clear()
                }
                WidgetCenter.shared.reloadAllTimelines()
            }

            Text("애플 기본 달력 앱의 일정을 Kalender에서 함께 볼 수 있어요. (달력 앱에 일정을 넣는 것은 불가능합니다)")
                .font(.caption)
                .foregroundStyle(.secondary)

            if appleCalendarEnabled && !appleCalendar.isAuthorized {
                Text("달력 접근 권한이 필요합니다. 아이폰 설정 → Kalender → 캘린더에서 허용해 주세요.")
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.red)
            }
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }
}
