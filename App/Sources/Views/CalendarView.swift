import SwiftData
import SwiftUI
import WidgetKit

struct CalendarView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \Schedule.createdAt) private var schedules: [Schedule]

    @State private var displayedMonth: Date = Calendar.current.startOfMonth(for: .now)
    @State private var selectedDate: Date = Calendar.current.startOfDay(for: .now)
    @State private var editingSchedule: Schedule?
    @State private var isAddingSchedule = false

    private let calendar = Calendar.current

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                monthHeader
                weekdayHeader
                monthGrid
                Divider()
                    .padding(.top, 8)
                daySection
            }
            .navigationTitle("일정위젯")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        isAddingSchedule = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $isAddingSchedule) {
                ScheduleFormView(defaultDate: selectedDate)
            }
            .sheet(item: $editingSchedule) { schedule in
                ScheduleFormView(schedule: schedule)
            }
        }
    }

    // MARK: - 월 헤더

    private var monthHeader: some View {
        HStack {
            Text(displayedMonth.formatted(.dateTime.year().month(.wide).locale(Locale(identifier: "ko_KR"))))
                .font(.title3.bold())

            Spacer()

            Button("오늘") { goToToday() }
                .font(.subheadline.weight(.semibold))
                .buttonStyle(.bordered)
                .controlSize(.small)

            HStack(spacing: 4) {
                Button { moveMonth(by: -1) } label: {
                    Image(systemName: "chevron.left")
                        .frame(width: 32, height: 32)
                }
                Button { moveMonth(by: 1) } label: {
                    Image(systemName: "chevron.right")
                        .frame(width: 32, height: 32)
                }
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }

    private var weekdayHeader: some View {
        HStack(spacing: 0) {
            ForEach(Array(["일", "월", "화", "수", "목", "금", "토"].enumerated()), id: \.offset) { index, symbol in
                Text(symbol)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(index == 0 ? .red : index == 6 ? .blue : .secondary)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 8)
        .padding(.bottom, 4)
    }

    // MARK: - 달력 그리드

    private var monthGrid: some View {
        let days = makeMonthDays()

        return LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 0), count: 7), spacing: 4) {
            ForEach(days.indices, id: \.self) { index in
                if let day = days[index] {
                    dayCell(for: day)
                } else {
                    Color.clear.frame(height: 46)
                }
            }
        }
        .padding(.horizontal, 8)
        .gesture(
            DragGesture(minimumDistance: 24)
                .onEnded { value in
                    if value.translation.width < -40 {
                        moveMonth(by: 1)
                    } else if value.translation.width > 40 {
                        moveMonth(by: -1)
                    }
                }
        )
    }

    private func dayCell(for day: Date) -> some View {
        let isSelected = calendar.isDate(day, inSameDayAs: selectedDate)
        let isToday = calendar.isDateInToday(day)
        let weekday = calendar.component(.weekday, from: day)
        let dayColors = ScheduleStore.occurrences(in: schedules, on: day, calendar: calendar)
            .prefix(3)
            .map(\.color)

        return Button {
            selectedDate = day
        } label: {
            VStack(spacing: 3) {
                Text("\(calendar.component(.day, from: day))")
                    .font(.callout.weight(isToday ? .heavy : .medium))
                    .foregroundStyle(
                        isSelected ? Color.white
                            : isToday ? Color.accentColor
                            : weekday == 1 ? .red
                            : weekday == 7 ? .blue
                            : .primary
                    )
                    .frame(width: 32, height: 32)
                    .background {
                        if isSelected {
                            Circle().fill(Color.accentColor)
                        } else if isToday {
                            Circle().stroke(Color.accentColor, lineWidth: 1.5)
                        }
                    }

                HStack(spacing: 3) {
                    ForEach(dayColors.indices, id: \.self) { colorIndex in
                        Circle()
                            .fill(dayColors[colorIndex].color)
                            .frame(width: 5, height: 5)
                    }
                }
                .frame(height: 6)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 46)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: - 선택한 날짜 일정

    private var daySection: some View {
        let dayNotes = ScheduleStore.occurrences(in: schedules, on: selectedDate, calendar: calendar)

        return Group {
            HStack {
                Text(selectedDate.formatted(
                    .dateTime.month().day().weekday(.wide).locale(Locale(identifier: "ko_KR"))
                ))
                .font(.headline)

                Spacer()

                Text("\(dayNotes.count)개")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal)
            .padding(.top, 12)

            if dayNotes.isEmpty {
                ContentUnavailableView {
                    Label("일정 없음", systemImage: "calendar.badge.plus")
                } description: {
                    Text("+ 버튼으로 일정을 추가해 보세요")
                }
                .frame(maxHeight: .infinity)
            } else {
                List {
                    ForEach(dayNotes) { schedule in
                        scheduleRow(schedule)
                            .contentShape(Rectangle())
                            .onTapGesture { editingSchedule = schedule }
                    }
                    .onDelete { offsets in
                        deleteSchedules(at: offsets, in: dayNotes)
                    }
                }
                .listStyle(.plain)
            }
        }
    }

    private func scheduleRow(_ schedule: Schedule) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 2)
                .fill(schedule.color.color)
                .frame(width: 4, height: 36)

            VStack(alignment: .leading, spacing: 2) {
                Text(schedule.title)
                    .font(.body.weight(.medium))
                if !schedule.memo.isEmpty {
                    Text(schedule.memo)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 4) {
                if let badge = schedule.recurrence.badgeText {
                    Text(badge)
                        .font(.caption2.weight(.bold))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(schedule.color.color.opacity(0.15))
                        .foregroundStyle(schedule.color.color)
                        .clipShape(Capsule())
                }
                Text(schedule.timeText ?? "종일")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 2)
    }

    // MARK: - 동작

    private func makeMonthDays() -> [Date?] {
        guard let range = calendar.range(of: .day, in: .month, for: displayedMonth) else { return [] }

        let firstWeekday = calendar.component(.weekday, from: displayedMonth)
        let leadingBlanks = Array<Date?>(repeating: nil, count: firstWeekday - 1)
        let days: [Date?] = range.compactMap { day in
            calendar.date(byAdding: .day, value: day - 1, to: displayedMonth)
        }

        return leadingBlanks + days
    }

    private func moveMonth(by value: Int) {
        guard let next = calendar.date(byAdding: .month, value: value, to: displayedMonth) else { return }

        withAnimation(.snappy(duration: 0.2)) {
            displayedMonth = calendar.startOfMonth(for: next)
        }
    }

    private func goToToday() {
        withAnimation(.snappy(duration: 0.2)) {
            displayedMonth = calendar.startOfMonth(for: .now)
            selectedDate = calendar.startOfDay(for: .now)
        }
    }

    private func deleteSchedules(at offsets: IndexSet, in dayNotes: [Schedule]) {
        for offset in offsets {
            modelContext.delete(dayNotes[offset])
        }
        try? modelContext.save()
        WidgetCenter.shared.reloadAllTimelines()
    }
}

extension Calendar {
    func startOfMonth(for date: Date) -> Date {
        self.date(from: dateComponents([.year, .month], from: date)) ?? date
    }
}
