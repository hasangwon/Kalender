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

    /// 검색 결과 (제목/메모, 대소문자 무시), 최신 시작일 순.
    /// 입력이 잠깐 멈췄을 때·일정이 바뀌었을 때만 계산하고, 무한 스크롤은 이 값을 재사용한다.
    @State private var results: [Schedule] = []
    /// `results`가 어떤 검색어로 계산된 것인지 — 입력 직후 아직 계산 전이면 다르다
    @State private var searchedQuery = ""

    /// 연속 입력 중 중복 검색을 줄이는 대기 시간
    private let searchDelay: Duration = .milliseconds(250)

    /// 검색 결과에 영향을 주는 값 (제목·메모로 매칭, 시작일로 정렬) — 바뀌면 다시 검색
    private var searchSnapshot: [String] {
        schedules.map { "\($0.title)\u{1F}\($0.memo)\u{1F}\($0.startDate.timeIntervalSinceReferenceDate)" }
    }

    private func search(_ keyword: String) -> [Schedule] {
        guard !keyword.isEmpty else { return [] }

        let lowered = keyword.lowercased()
        return schedules
            .filter { schedule in
                schedule.title.lowercased().contains(lowered)
                    || schedule.memo.lowercased().contains(lowered)
            }
            .sorted { $0.startDate > $1.startDate }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                searchField

                if trimmedQuery.isEmpty {
                    emptyPrompt
                } else if !results.isEmpty {
                    resultList(results)
                } else if searchedQuery == trimmedQuery {
                    noResults
                } else {
                    // 입력 직후 검색 대기 중
                    Spacer()
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
        .task(id: trimmedQuery) {
            let keyword = trimmedQuery
            if !keyword.isEmpty {
                // 입력이 이어지면 이 task가 취소되고 새 검색어로 다시 시작된다
                do { try await Task.sleep(for: searchDelay) } catch { return }
            }
            results = search(keyword)
            searchedQuery = keyword
        }
        // 같은 일정의 제목·메모·날짜만 바뀌면 배열 비교로는 감지되지 않아(같은 객체) 값으로 비교한다
        .onChange(of: searchSnapshot) { _, _ in
            results = search(searchedQuery)
        }
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

    private func resultList(_ results: [Schedule]) -> some View {
        let visibleResults = Array(results.prefix(visibleCount))

        return ScrollView {
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
                        if let badge = schedule.recurrenceBadgeText {
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
            .dateTime.year().month().day().locale(.korean)
        )

        switch schedule.recurrence {
        case .none:
            return base
        case .weekly, .monthly:
            return "\(base)부터"
        }
    }
}
