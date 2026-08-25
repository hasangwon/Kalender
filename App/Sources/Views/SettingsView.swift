import SwiftUI
import WidgetKit

/// 설정 — 반복 유형별 일정 색 (8색 팔레트, 중복 허용)
struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext


    @State private var themeColor = ThemeSettings.current
    @State private var backgroundColor = BackgroundSettings.current
    @State private var singleColor = EventColorSettings.color(for: .none)
    @State private var weeklyColor = EventColorSettings.color(for: .weekly)
    @State private var monthlyColor = EventColorSettings.color(for: .monthly)
    /// 색상 리스트가 펼쳐진(편집 중인) 유형
    @State private var editingRecurrence: Recurrence?
    @State private var textSize = TextSizeSettings.current
    @State private var widgetTextSize = WidgetTextSizeSettings.current
    @State private var digestTime: Date = {
        Calendar.current.date(
            bySettingHour: NotificationSettings.digestHour,
            minute: NotificationSettings.digestMinute,
            second: 0,
            of: .now
        ) ?? .now
    }()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    themeCard
                    textSizeCard
                    colorCard
                    notificationCard
                }
                .dynamicTypeSize(textSize.dynamicTypeSize)
                .padding(16)
            }
            .background(AppTheme.background)
            .fontDesign(.rounded)
            .navigationTitle("설정")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("완료") {
                        dismiss()
                    }
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(AppTheme.primary)
                }
            }
            .toolbarBackground(AppTheme.background, for: .navigationBar)
        }
    }

    private var themeCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("테마색")
                .font(.system(.headline, design: .rounded).weight(.bold))

            HStack(spacing: 10) {
                ForEach(ThemeColor.allCases) { theme in
                    Button {
                        themeColor = theme
                        ThemeSettings.setCurrent(theme)
                        WidgetCenter.shared.reloadAllTimelines()
                    } label: {
                        Circle()
                            .fill(theme.color)
                            .frame(width: 28, height: 28)
                            .overlay {
                                if themeColor == theme {
                                    Image(systemName: "checkmark")
                                        .font(.caption2.bold())
                                        .foregroundStyle(.white)
                                }
                            }
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(theme.label)
                }
            }

            Text("앱 전체 포인트 색과 위젯에 적용됩니다.")
                .font(.caption2)
                .foregroundStyle(.secondary)

            Divider()

            Text("배경색")
                .font(.system(.headline, design: .rounded).weight(.bold))

            HStack(spacing: 10) {
                ForEach(BackgroundColor.allCases) { background in
                    Button {
                        backgroundColor = background
                        BackgroundSettings.setCurrent(background)
                        InterfaceStyle.apply()
                        WidgetCenter.shared.reloadAllTimelines()
                    } label: {
                        Circle()
                            .fill(background.swatch)
                            .frame(width: 28, height: 28)
                            .overlay(Circle().stroke(Color.primary.opacity(0.12), lineWidth: 1))
                            .overlay {
                                if backgroundColor == background {
                                    Image(systemName: "checkmark")
                                        .font(.caption2.bold())
                                        .foregroundStyle(background == .ink ? .white : .black.opacity(0.6))
                                }
                            }
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(background.label)
                }
            }

            Text("잉크를 선택하면 앱이 어두운 화면으로 표시됩니다.")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }

    private var colorCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("일정별 기본 색상")
                .font(.system(.headline, design: .rounded).weight(.bold))

            // 유형 3개 가로 배치 — 각자 현재 색 표시, 탭하면 아래 공유 팔레트로 편집
            HStack(spacing: 8) {
                ForEach(Recurrence.allCases) { option in
                    typeChip(option)
                }
            }

            if let editing = editingRecurrence {
                Divider()

                HStack(spacing: 10) {
                    ForEach(ColorTag.allCases) { tag in
                        Button {
                            setDefaultColor(tag, for: editing)
                        } label: {
                            Circle()
                                .fill(tag.color)
                                .frame(width: 28, height: 28)
                                .overlay {
                                    if defaultColor(for: editing) == tag {
                                        Image(systemName: "checkmark")
                                            .font(.caption2.bold())
                                            .foregroundStyle(.white)
                                    }
                                }
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(tag.label)
                    }
                }
            }

            Text("각 일정의 기본 색으로 적용됩니다. 일정 작성 화면에서 개별 색으로 바꿀 수도 있습니다.")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }

    private func typeChip(_ option: Recurrence) -> some View {
        let isEditing = editingRecurrence == option
        let color = defaultColor(for: option)

        return Button {
            withAnimation(.snappy(duration: 0.2)) {
                editingRecurrence = isEditing ? nil : option
            }
        } label: {
            HStack(spacing: 6) {
                Circle()
                    .fill(color.color)
                    .frame(width: 12, height: 12)
                Text("\(option.label) 일정")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.primary)
            }
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(isEditing ? color.color.opacity(0.14) : Color.primary.opacity(0.04))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isEditing ? color.color : .clear, lineWidth: 1.5)
            )
        }
        .buttonStyle(.plain)
    }

    private func defaultColor(for recurrence: Recurrence) -> ColorTag {
        switch recurrence {
        case .none: singleColor
        case .weekly: weeklyColor
        case .monthly: monthlyColor
        }
    }

    private func setDefaultColor(_ tag: ColorTag, for recurrence: Recurrence) {
        switch recurrence {
        case .none: singleColor = tag
        case .weekly: weeklyColor = tag
        case .monthly: monthlyColor = tag
        }

        EventColorSettings.setColor(tag, for: recurrence)
        WidgetCenter.shared.reloadAllTimelines()
    }

    private var textSizeCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("글자 크기")
                .font(.system(.headline, design: .rounded).weight(.bold))

            VStack(alignment: .leading, spacing: 8) {
                Text("앱")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.secondary)
                sizeSelector(selection: textSize) { option in
                    textSize = option
                    TextSizeSettings.setCurrent(option)
                }
            }

            Divider()

            VStack(alignment: .leading, spacing: 8) {
                Text("위젯")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.secondary)
                sizeSelector(selection: widgetTextSize) { option in
                    widgetTextSize = option
                    WidgetTextSizeSettings.setCurrent(option)
                    WidgetCenter.shared.reloadAllTimelines()
                }
            }

            Text("앱과 위젯의 글자 크기를 따로 설정할 수 있어요.")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }

    private func sizeSelector(
        selection: TextSizeOption,
        onSelect: @escaping (TextSizeOption) -> Void
    ) -> some View {
        HStack(spacing: 8) {
            ForEach(TextSizeOption.allCases) { option in
                Button {
                    onSelect(option)
                } label: {
                    Text(option.label)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(selection == option ? .white : .primary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 9)
                        .background(
                            RoundedRectangle(cornerRadius: 11)
                                .fill(selection == option ? AppTheme.primary : Color.primary.opacity(0.06))
                        )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var notificationCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("알림")
                .font(.system(.headline, design: .rounded).weight(.bold))

            DatePicker(
                "하루 알림 시간",
                selection: $digestTime,
                displayedComponents: .hourAndMinute
            )
            .font(.subheadline.weight(.semibold))
            .onChange(of: digestTime) { _, newValue in
                let components = Calendar.current.dateComponents([.hour, .minute], from: newValue)
                NotificationSettings.setDigestTime(
                    hour: components.hour ?? 8,
                    minute: components.minute ?? 0
                )
                NotificationManager.refresh(context: modelContext)
            }

            Text("알림을 켠 일정이 있는 날, 이 시간에 하루 일정을 모아 한 번만 알려드려요.")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }


}
