import SwiftData
import SwiftUI
import WidgetKit

/// 일정 추가/수정 폼 (schedule이 있으면 수정 모드)
struct ScheduleFormView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext

    private let editingSchedule: Schedule?

    @State private var title: String
    @State private var memo: String
    @State private var date: Date
    @State private var hasTime: Bool
    @State private var time: Date
    @State private var recurrence: Recurrence
    @State private var colorTag: ColorTag
    @State private var isConfirmingDelete = false

    private let calendar = Calendar.current

    init(defaultDate: Date = .now) {
        editingSchedule = nil
        _title = State(initialValue: "")
        _memo = State(initialValue: "")
        _date = State(initialValue: defaultDate)
        _hasTime = State(initialValue: false)
        _time = State(initialValue: Self.defaultTime)
        _recurrence = State(initialValue: .none)
        _colorTag = State(initialValue: .blue)
    }

    init(schedule: Schedule) {
        editingSchedule = schedule
        _title = State(initialValue: schedule.title)
        _memo = State(initialValue: schedule.memo)
        _date = State(initialValue: schedule.startDate)
        _hasTime = State(initialValue: schedule.hasTime)
        _time = State(initialValue: schedule.hasTime ? schedule.startDate : Self.defaultTime)
        _recurrence = State(initialValue: schedule.recurrence)
        _colorTag = State(initialValue: schedule.color)
    }

    private static var defaultTime: Date {
        Calendar.current.date(bySettingHour: 9, minute: 0, second: 0, of: .now) ?? .now
    }

    private var isEditing: Bool { editingSchedule != nil }

    private var trimmedTitle: String {
        title.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("제목", text: $title)
                    TextField("메모 (선택)", text: $memo, axis: .vertical)
                        .lineLimit(1...4)
                }

                Section {
                    DatePicker("날짜", selection: $date, displayedComponents: .date)
                    Toggle("시간 설정", isOn: $hasTime.animation())
                    if hasTime {
                        DatePicker("시간", selection: $time, displayedComponents: .hourAndMinute)
                    }
                }

                Section {
                    Picker("반복", selection: $recurrence) {
                        ForEach(Recurrence.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                    .pickerStyle(.segmented)
                } header: {
                    Text("반복")
                } footer: {
                    Text(recurrenceFooter)
                }

                Section("색상") {
                    HStack(spacing: 14) {
                        ForEach(ColorTag.allCases) { tag in
                            Button {
                                colorTag = tag
                            } label: {
                                Circle()
                                    .fill(tag.color)
                                    .frame(width: 30, height: 30)
                                    .overlay {
                                        if colorTag == tag {
                                            Image(systemName: "checkmark")
                                                .font(.caption.bold())
                                                .foregroundStyle(.white)
                                        }
                                    }
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(tag.label)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 4)
                }

                if isEditing {
                    Section {
                        Button("일정 삭제", role: .destructive) {
                            isConfirmingDelete = true
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
            }
            .navigationTitle(isEditing ? "일정 수정" : "새 일정")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("저장") { save() }
                        .disabled(trimmedTitle.isEmpty)
                }
            }
            .confirmationDialog(
                deleteDialogTitle,
                isPresented: $isConfirmingDelete,
                titleVisibility: .visible
            ) {
                Button("삭제", role: .destructive) { deleteSchedule() }
            }
        }
        .presentationDetents([.large])
    }

    private var recurrenceFooter: String {
        switch recurrence {
        case .none:
            return "선택한 날짜에 한 번만 표시됩니다."
        case .weekly:
            let weekday = date.formatted(.dateTime.weekday(.wide).locale(Locale(identifier: "ko_KR")))
            return "선택한 날짜부터 \(weekday)마다 반복됩니다."
        case .monthly:
            let day = calendar.component(.day, from: date)
            return "선택한 날짜부터 매달 \(day)일마다 반복됩니다."
        }
    }

    private var deleteDialogTitle: String {
        editingSchedule?.recurrence == Recurrence.none
            ? "이 일정을 삭제할까요?"
            : "반복 일정 전체가 삭제됩니다. 삭제할까요?"
    }

    /// 날짜 + (선택 시) 시간을 합쳐 저장할 startDate 계산
    private var resolvedStartDate: Date {
        let dayStart = calendar.startOfDay(for: date)

        guard hasTime else { return dayStart }

        let hour = calendar.component(.hour, from: time)
        let minute = calendar.component(.minute, from: time)
        return calendar.date(bySettingHour: hour, minute: minute, second: 0, of: dayStart) ?? dayStart
    }

    private func save() {
        if let schedule = editingSchedule {
            schedule.title = trimmedTitle
            schedule.memo = memo.trimmingCharacters(in: .whitespacesAndNewlines)
            schedule.startDate = resolvedStartDate
            schedule.hasTime = hasTime
            schedule.recurrence = recurrence
            schedule.color = colorTag
        } else {
            let schedule = Schedule(
                title: trimmedTitle,
                memo: memo.trimmingCharacters(in: .whitespacesAndNewlines),
                startDate: resolvedStartDate,
                hasTime: hasTime,
                recurrence: recurrence,
                color: colorTag
            )
            modelContext.insert(schedule)
        }

        try? modelContext.save()
        WidgetCenter.shared.reloadAllTimelines()
        dismiss()
    }

    private func deleteSchedule() {
        guard let schedule = editingSchedule else { return }

        modelContext.delete(schedule)
        try? modelContext.save()
        WidgetCenter.shared.reloadAllTimelines()
        dismiss()
    }
}
