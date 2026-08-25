import SwiftData
import SwiftUI

/// 일정 검색 — 제목/메모 텍스트 매칭, 결과 탭 시 해당 날짜로 이동.
/// 반복 일정은 첫 발생일(startDate)로 이동.
struct SearchView: View {
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \Schedule.createdAt) private var schedules: [Schedule]

    /// 선택된 결과의 이동 목표 날짜를 부모(CalendarView)로 전달
    let onSelect: (Date) -> Void

    @State private var query = ""
    /// 무한 스크롤: 처음엔 일부만, 스크롤 끝에서 더 로드
    @State private var visibleCount = 20

    @FocusState private var isSearchFocused: Bool

    private let pageSize = 20
    private let calendar = Calendar.current

    private var trimmedQuery: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// 매칭된 전체 결과 (제목/메모, 대소문자 무시), 최신 시작일 순
    private var results: [Schedule] {
        guard !trimmedQuery.isEmpty else { return [] }

        let keyword = trimmedQuery.lowercased()
        return schedules
            .filter { schedule in
                schedule.title.lowercased().contains(keyword)
                    || schedule.memo.lowercased().contains(keyword)
            }
            .sorted { $0.startDate > $1.startDate }
    }

    private var visibleResults: [Schedule] {
        Array(results.prefix(visibleCount))
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                searchField

                if trimmedQuery.isEmpty {
                    emptyPrompt
                } else if results.isEmpty {
                    noResults
                } else {
                    resultList
                }
            }
            .background(AppTheme.background)
            .fontDesign(.rounded)
            .dynamicTypeSize(TextSizeSettings.current.dynamicTypeSize)
            .navigationTitle("검색")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("완료") { dismiss() }
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(AppTheme.primary)
                }
            }
            .toolbarBackground(AppTheme.background, for: .navigationBar)
        }
        .onAppear { isSearchFocused = true }
    }

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField("일정 제목·메모 검색", text: $query)
                .focused($isSearchFocused)
                .submitLabel(.search)
                .autocorrectionDisabled()
                .onChange(of: query) { _, _ in visibleCount = pageSize }
            if !query.isEmpty {
                Button {
                    query = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14))
        .padding(16)
    }

    private var emptyPrompt: some View {
        ContentUnavailableView {
            Label("일정 검색", systemImage: "magnifyingglass")
        } description: {
            Text("제목이나 메모로 일정을 찾아보세요")
        }
        .frame(maxHeight: .infinity)
    }

    private var noResults: some View {
        ContentUnavailableView.search(text: trimmedQuery)
            .frame(maxHeight: .infinity)
    }

    private var resultList: some View {
        ScrollView {
            LazyVStack(spacing: 8) {
                ForEach(visibleResults) { schedule in
                    resultCard(schedule)
                        .onAppear {
                            // 무한 스크롤: 마지막 항목이 보이면 다음 페이지 로드
                            if schedule.id == visibleResults.last?.id,
                               visibleCount < results.count {
                                visibleCount += pageSize
                            }
                        }
                }

                if visibleCount < results.count {
                    ProgressView()
                        .padding(.vertical, 12)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 20)
        }
    }

    private func resultCard(_ schedule: Schedule) -> some View {
        Button {
            onSelect(calendar.startOfDay(for: schedule.startDate))
            dismiss()
        } label: {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(schedule.displayColor.color)
                    .frame(width: 4, height: 40)

                VStack(alignment: .leading, spacing: 3) {
                    Text(schedule.title)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.primary)
                        .lineLimit(1)

                    if !schedule.memo.isEmpty {
                        Text(schedule.memo)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }

                    HStack(spacing: 6) {
                        if let badge = schedule.recurrence.badgeText {
                            Text(badge)
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(schedule.displayColor.color)
                        }
                        Text(dateText(schedule))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(.tertiary)
            }
            .padding(14)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 16))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    /// 반복 일정은 "첫 일정 날짜" 안내
    private func dateText(_ schedule: Schedule) -> String {
        let base = schedule.startDate.formatted(
            .dateTime.year().month().day().locale(Locale(identifier: "ko_KR"))
        )

        switch schedule.recurrence {
        case .none:
            return base
        case .weekly, .monthly:
            return "\(base)부터"
        }
    }
}
