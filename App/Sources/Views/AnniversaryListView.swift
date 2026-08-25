import SwiftData
import SwiftUI
import WidgetKit

/// 매년 기념일 등록/관리 — 양력 또는 음력(자동 양력 환산)으로 매년 달력·위젯에 표시
struct AnniversaryListView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \AnniversaryEntry.createdAt) private var anniversaries: [AnniversaryEntry]


    @State private var name = ""
    @State private var month = 1
    @State private var day = 1
    @State private var isLunar = false
    @State private var expandedID: UUID?

    private let calendar = Calendar.current

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    inputCard
                    registeredSection
                }
                .padding(16)
            }
            .background(AppTheme.background)
            .fontDesign(.rounded)
            .dynamicTypeSize(TextSizeSettings.current.dynamicTypeSize)
            .navigationTitle("매년 기념일 등록")
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

    // MARK: - 입력 카드

    private var inputCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("새 기념일")
                .font(.system(.headline, design: .rounded).weight(.bold))

            TextField("이름 (예: 엄마 생일, 결혼기념일)", text: $name)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 12))

            Toggle(isOn: $isLunar.animation(.snappy(duration: 0.2))) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("음력 날짜로 입력")
                        .font(.subheadline.weight(.semibold))
                    Text("음력 생일 등 — 매년 양력으로 환산해 표시돼요")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            .tint(AppTheme.primary)

            HStack(spacing: 20) {
                Text(isLunar ? "음력" : "양력")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.secondary)

                Spacer()

                datePickerChip {
                    Picker("월", selection: $month) {
                        ForEach(1...12, id: \.self) { value in
                            Text("\(value)월").tag(value)
                        }
                    }
                }

                datePickerChip {
                    Picker("일", selection: $day) {
                        ForEach(1...(isLunar ? 30 : 31), id: \.self) { value in
                            Text("\(value)일").tag(value)
                        }
                    }
                }
            }

            if isLunar {
                HStack(spacing: 6) {
                    Image(systemName: "arrow.right")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.tertiary)
                    Text(lunarPreviewText)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(AppTheme.primary)
                }
            }

            Button {
                addAnniversary()
            } label: {
                Label("기념일 추가", systemImage: "plus")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        name.trimmingCharacters(in: .whitespaces).isEmpty
                            ? AnyShapeStyle(Color.secondary.opacity(0.35))
                            : AnyShapeStyle(AppTheme.primary),
                        in: RoundedRectangle(cornerRadius: 14)
                    )
            }
            .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)

            Text("등록한 기념일은 매년 달력과 위젯에 자동 표시됩니다. 음력 윤달은 평달 기준으로 표시됩니다.")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }

    private var lunarPreviewText: String {
        let year = calendar.component(.year, from: .now)

        guard let solar = Lunar.solarDate(inYear: year, lunarMonth: month, lunarDay: day, calendar: calendar) else {
            return "올해는 해당 음력 날짜가 없어요"
        }

        let text = solar.formatted(
            .dateTime.month().day().weekday(.short).locale(Locale(identifier: "ko_KR"))
        )
        return "올해 양력 \(text)"
    }

    private func datePickerChip(@ViewBuilder content: () -> some View) -> some View {
        content()
            .pickerStyle(.menu)
            .tint(.primary)
            .padding(.horizontal, 6)
            .background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 10))
    }

    // MARK: - 등록된 기념일

    private var registeredSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("등록된 기념일")
                .font(.system(.headline, design: .rounded).weight(.bold))
                .padding(.horizontal, 4)

            if anniversaries.isEmpty {
                Text("등록된 기념일이 없습니다")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 28)
                    .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 16))
            } else {
                ForEach(anniversaries) { anniversary in
                    anniversaryCard(anniversary)
                }
            }
        }
    }

    private func anniversaryCard(_ anniversary: AnniversaryEntry) -> some View {
        let isExpanded = expandedID == anniversary.id

        return VStack(spacing: 0) {
            Button {
                withAnimation(.snappy(duration: 0.22)) {
                    expandedID = isExpanded ? nil : anniversary.id
                }
            } label: {
                HStack(spacing: 10) {
                    Circle()
                        .fill(EventColorSettings.anniversaryColor.color)
                        .frame(width: 8, height: 8)
                    Text(anniversary.name)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.primary)
                    if anniversary.isLunar {
                        Text("음력")
                            .font(.caption2.weight(.bold))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(AppTheme.accent.opacity(0.12))
                            .foregroundStyle(AppTheme.accent)
                            .clipShape(Capsule())
                    }
                    Spacer()
                    Text(anniversary.dateText)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Image(systemName: "chevron.down")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.tertiary)
                        .rotationEffect(.degrees(isExpanded ? 180 : 0))
                }
                .padding(16)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if isExpanded {
                VStack(spacing: 12) {
                    Divider()

                    HStack {
                        Text("올해 표시일")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.secondary)
                        Spacer()
                        Text(thisYearText(for: anniversary))
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(AppTheme.primary)
                    }

                    Button(role: .destructive) {
                        delete(anniversary)
                    } label: {
                        Label("삭제", systemImage: "trash")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.red)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 11)
                            .background(Color.red.opacity(0.1), in: RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 14)
            }
        }
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.04), radius: 8, y: 2)
    }

    // MARK: - 동작

    private func thisYearText(for anniversary: AnniversaryEntry) -> String {
        let year = calendar.component(.year, from: .now)

        guard let solar = anniversary.solarDate(inYear: year, calendar: calendar) else {
            return "올해 없음"
        }

        return solar.formatted(
            .dateTime.month().day().weekday(.short).locale(Locale(identifier: "ko_KR"))
        )
    }

    private func addAnniversary() {
        let entry = AnniversaryEntry(
            name: name.trimmingCharacters(in: .whitespaces),
            month: month,
            day: day,
            isLunar: isLunar
        )

        modelContext.insert(entry)
        try? modelContext.save()
        WidgetCenter.shared.reloadAllTimelines()
        name = ""
    }

    private func delete(_ anniversary: AnniversaryEntry) {
        withAnimation(.snappy(duration: 0.2)) {
            if expandedID == anniversary.id { expandedID = nil }
            modelContext.delete(anniversary)
        }
        try? modelContext.save()
        WidgetCenter.shared.reloadAllTimelines()
    }
}
