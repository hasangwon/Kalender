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
    @State private var notifies: Bool
    @State private var hasEndDate: Bool
    @State private var endDate: Date
    /// nil이면 유형 기본 색을 따름
    @State private var pickedColor: ColorTag?
    @State private var isColorExpanded = false
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
        _notifies = State(initialValue: false)
        _hasEndDate = State(initialValue: false)
        _endDate = State(initialValue: defaultDate)
        _pickedColor = State(initialValue: nil)
    }

    init(schedule: Schedule) {
        editingSchedule = schedule
        _title = State(initialValue: schedule.title)
        _memo = State(initialValue: schedule.memo)
        _date = State(initialValue: schedule.startDate)
        _hasTime = State(initialValue: schedule.hasTime)
        _time = State(initialValue: schedule.hasTime ? schedule.startDate : Self.defaultTime)
        _recurrence = State(initialValue: schedule.recurrence)
        _notifies = State(initialValue: schedule.notifies)
        _hasEndDate = State(initialValue: schedule.endDate != nil)
        _endDate = State(initialValue: schedule.endDate ?? schedule.startDate)
        _pickedColor = State(initialValue: schedule.hasCustomColor ? schedule.color : nil)
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

                    Toggle("알림 받기", isOn: $notifies)
                        .onChange(of: notifies) { _, isOn in
                            if isOn {
                                NotificationManager.requestAuthorizationIfNeeded()
                            }
                        }
                } footer: {
                    Text(notifies ? "이 일정이 있는 날, 설정한 시간에 하루 일정을 모아 알려드려요." : "")
                }

                Section {
                    Picker("반복", selection: $recurrence) {
                        ForEach(Recurrence.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                    .pickerStyle(.segmented)

                    // 매주/매달 반복에서만 종료일 지정 가능
                    if recurrence != .none {
                        Toggle("종료 날짜 지정", isOn: $hasEndDate.animation())

                        if hasEndDate {
                            DatePicker(
                                "종료 날짜",
                                selection: $endDate,
                                in: date...,
                                displayedComponents: .date
                            )
                        }
                    }
                } header: {
                    Text("반복")
                } footer: {
                    Text(recurrenceFooter)
                }
                .onChange(of: date) { _, newDate in
                    // 종료일은 시작일보다 앞설 수 없음
                    if endDate < newDate { endDate = newDate }
                }
                .onChange(of: recurrence) { _, newValue in
                    if newValue == .none { hasEndDate = false }
                }

                Section {
                    Button {
                        withAnimation(.snappy(duration: 0.2)) { isColorExpanded.toggle() }
                    } label: {
                        HStack {
                            Text("표시 색")
                                .foregroundStyle(.primary)
                            Spacer()
                            Circle()
                                .fill(effectiveColor.color)
                                .frame(width: 16, height: 16)
                            Image(systemName: "chevron.down")
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(.tertiary)
                                .rotationEffect(.degrees(isColorExpanded ? 180 : 0))
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)

                    if isColorExpanded {
                        HStack(spacing: 10) {
                            ForEach(ColorTag.allCases) { tag in
                                Button {
                                    // 유형 기본 색을 고르면 "기본 따름"으로 되돌림
                                    pickedColor = tag == typeDefaultColor ? nil : tag
                                } label: {
                                    Circle()
                                        .fill(tag.color)
                                        .frame(width: 28, height: 28)
                                        .overlay {
                                            if effectiveColor == tag {
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
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 4)
                    }
                } footer: {
                    Text(pickedColor == nil ? "일정 유형의 기본 색을 따르고 있습니다." : "이 일정만 개별 색을 사용합니다.")
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
            .fontDesign(.rounded)
            .dynamicTypeSize(TextSizeSettings.current.dynamicTypeSize)
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

    private var typeDefaultColor: ColorTag {
        EventColorSettings.color(for: recurrence)
    }

    private var effectiveColor: ColorTag {
        pickedColor ?? typeDefaultColor
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
        // 종료일: 반복 일정 + 지정 시에만, 시작일 이전 방지
        let resolvedEndDate: Date? = (recurrence != .none && hasEndDate)
            ? calendar.startOfDay(for: max(endDate, date))
            : nil
        let usesCustomColor = pickedColor != nil

        if let schedule = editingSchedule {
            schedule.title = trimmedTitle
            schedule.memo = memo.trimmingCharacters(in: .whitespacesAndNewlines)
            schedule.startDate = resolvedStartDate
            schedule.hasTime = hasTime
            schedule.recurrence = recurrence
            schedule.endDate = resolvedEndDate
            schedule.hasCustomColor = usesCustomColor
            schedule.color = pickedColor ?? typeDefaultColor
            schedule.notifies = notifies
        } else {
            let schedule = Schedule(
                title: trimmedTitle,
                memo: memo.trimmingCharacters(in: .whitespacesAndNewlines),
                startDate: resolvedStartDate,
                hasTime: hasTime,
                recurrence: recurrence,
                color: pickedColor ?? typeDefaultColor,
                endDate: resolvedEndDate,
                hasCustomColor: usesCustomColor,
                notifies: notifies
            )
            modelContext.insert(schedule)
        }

        try? modelContext.save()
        WidgetCenter.shared.reloadAllTimelines()
        NotificationManager.refresh(context: modelContext)
        dismiss()
    }

    private func deleteSchedule() {
        guard let schedule = editingSchedule else { return }

        modelContext.delete(schedule)
        try? modelContext.save()
        WidgetCenter.shared.reloadAllTimelines()
        NotificationManager.refresh(context: modelContext)
        dismiss()
    }
}
