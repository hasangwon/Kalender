import SwiftUI
import WidgetKit

private struct WidgetTextScaleKey: EnvironmentKey {
    static let defaultValue: CGFloat = 1.0
}

extension EnvironmentValues {
    var widgetTextScale: CGFloat {
        get { self[WidgetTextScaleKey.self] }
        set { self[WidgetTextScaleKey.self] = newValue }
    }
}

/// 위젯 내부 고정 폰트에 배율을 적용하는 헬퍼
private struct ScaledFont: ViewModifier {
    @Environment(\.widgetTextScale) private var scale
    let size: CGFloat
    let weight: Font.Weight
    var design: Font.Design = .default

    func body(content: Content) -> some View {
        content.font(.system(size: size * scale, weight: weight, design: design))
    }
}

private extension View {
    func scaledFont(size: CGFloat, weight: Font.Weight, design: Font.Design = .default) -> some View {
        modifier(ScaledFont(size: size, weight: weight, design: design))
    }
}

struct ScheduleWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "ScheduleWidget", provider: ScheduleProvider()) { entry in
            if BackgroundSettings.current.forcesDark {
                ScheduleWidgetEntryView(entry: entry)
                    .environment(\.colorScheme, .dark)
                    .containerBackground(for: .widget) {
                        AppTheme.background
                    }
            } else {
                ScheduleWidgetEntryView(entry: entry)
                    .containerBackground(for: .widget) {
                        AppTheme.background
                    }
            }
        }
        .configurationDisplayName("Kalender")
        .description("달력과 일정을 홈 화면에서 바로 확인하세요.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
        .contentMarginsDisabled()
    }
}

struct ScheduleWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family

    let entry: ScheduleEntry

    var body: some View {
        content
            // 위젯 글자 크기 배율 (설정과 연동)
            .environment(\.widgetTextScale, WidgetTextSizeSettings.current.widgetScale)
    }

    @ViewBuilder
    private var content: some View {
        switch family {
        case .systemLarge:
            // 기본 여백을 없애고 얇은 자체 패딩만 사용 — 달력을 최대한 크게
            LargeCalendarView(entry: entry)
                .padding(.horizontal, 10)
                .padding(.top, 10)
                .padding(.bottom, 6)
        case .systemMedium:
            MediumScheduleView(entry: entry)
                .padding(14)
        default:
            SmallScheduleView(entry: entry)
                .padding(14)
        }
    }
}

// MARK: - 공통 요소

private struct DateHeader: View {
    let date: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(date.formatted(.dateTime.month(.defaultDigits).locale(Locale(identifier: "ko_KR"))))
                .font(.system(.caption, design: .rounded).weight(.bold))
                .foregroundStyle(AppTheme.primary)
            Text("\(Calendar.current.component(.day, from: date))")
                .scaledFont(size: 34, weight: .heavy, design: .rounded)
            Text(date.formatted(.dateTime.weekday(.wide).locale(Locale(identifier: "ko_KR"))))
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.secondary)
        }
    }
}

private struct EventRow: View {
    let event: DayEvent
    var showsTime = true

    private var barColor: Color {
        event.kind == .holiday ? AppTheme.primary : (event.colorTag?.color ?? .secondary)
    }

    var body: some View {
        HStack(spacing: 6) {
            RoundedRectangle(cornerRadius: 1.5)
                .fill(barColor)
                .frame(width: 3, height: 22)

            VStack(alignment: .leading, spacing: 0) {
                Text(event.title)
                    .font(.system(.caption, design: .rounded).weight(.semibold))
                    .lineLimit(1)
                if showsTime, let timeText = event.timeText {
                    Text(timeText)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer(minLength: 0)
        }
    }
}

private struct EmptyEventsView: View {
    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: "checkmark.circle")
                .font(.title3)
                .foregroundStyle(.secondary)
            Text("일정 없음")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Small

private struct SmallScheduleView: View {
    let entry: ScheduleEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .top) {
                DateHeader(date: entry.date)
                Spacer()
                if entry.today.count > 2 {
                    Text("+\(entry.today.count - 2)")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.secondary)
                }
            }

            Spacer(minLength: 0)

            if entry.today.isEmpty {
                EmptyEventsView()
                    .frame(height: 44)
            } else {
                VStack(spacing: 4) {
                    ForEach(entry.today.prefix(2)) { event in
                        EventRow(event: event)
                    }
                }
            }
        }
    }
}

// MARK: - Medium

private struct MediumScheduleView: View {
    let entry: ScheduleEntry

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            VStack(alignment: .leading) {
                DateHeader(date: entry.date)
                Spacer()
                if let nextGroup = entry.upcoming.first {
                    VStack(alignment: .leading, spacing: 1) {
                        Text(nextGroup.date.formatted(
                            .dateTime.month(.defaultDigits).day().weekday(.short)
                                .locale(Locale(identifier: "ko_KR"))
                        ))
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.secondary)
                        Text(nextGroup.items[0].title)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }
                }
            }
            .frame(width: 86, alignment: .leading)

            Divider()

            if entry.today.isEmpty {
                EmptyEventsView()
            } else {
                VStack(spacing: 5) {
                    ForEach(entry.today.prefix(3)) { event in
                        EventRow(event: event)
                    }
                    if entry.today.count > 3 {
                        HStack {
                            Text("외 \(entry.today.count - 3)개")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            Spacer()
                        }
                    }
                    Spacer(minLength: 0)
                }
            }
        }
    }
}

// MARK: - Large (달력형)

/// 월 그리드 셀들을 7칸 x N주 행으로 나눔
private func makeWeekRows(leadingBlanks: Int, cells: [MonthCell]) -> [[MonthCell?]] {
    var slots: [MonthCell?] = Array(repeating: nil, count: leadingBlanks) + cells.map { $0 }

    while slots.count % 7 != 0 {
        slots.append(nil)
    }

    return stride(from: 0, to: slots.count, by: 7).map {
        Array(slots[$0..<min($0 + 7, slots.count)])
    }
}

private struct CalendarGridView: View {
    let entry: ScheduleEntry
    var showsHeader = true

    private let weekdaySymbols = ["일", "월", "화", "수", "목", "금", "토"]

    var body: some View {
        VStack(spacing: 4) {
            if showsHeader {
                ZStack {
                    Text(entry.monthTitle)
                        .font(.system(.subheadline, design: .rounded).weight(.heavy))
                        .foregroundStyle(AppTheme.primary)
                        .frame(maxWidth: .infinity)

                    HStack {
                        Spacer()
                        Text("Kalender")
                            .font(.system(.caption2, design: .rounded).weight(.bold))
                            .foregroundStyle(.tertiary)
                            .padding(.trailing, 8)
                    }
                }
            }

            HStack(spacing: 2) {
                ForEach(weekdaySymbols.indices, id: \.self) { index in
                    Text(weekdaySymbols[index])
                        .scaledFont(size: 8, weight: .semibold)
                        .foregroundStyle(index == 0 || index == 6 ? AppTheme.holidayRed : .secondary)
                        .frame(maxWidth: .infinity)
                }
            }

            let rows = makeWeekRows(leadingBlanks: entry.leadingBlanks, cells: entry.monthCells)

            // 남은 위젯 영역을 주 단위 행으로 균등 분할 — 빈 공간 없이 꽉 채움
            VStack(spacing: 2) {
                ForEach(rows.indices, id: \.self) { rowIndex in
                    HStack(spacing: 2) {
                        ForEach(0..<7, id: \.self) { columnIndex in
                            if let cell = rows[rowIndex][columnIndex] {
                                dayCell(cell)
                            } else {
                                Color.clear
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                            }
                        }
                    }
                    .frame(maxHeight: .infinity)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private func dayCell(_ cell: MonthCell) -> some View {
        let scale = WidgetTextSizeSettings.current.widgetScale
        let daySide = 16 * scale

        return VStack(spacing: 1) {
            Text("\(cell.day)")
                .scaledFont(size: 10, weight: cell.isToday ? .heavy : .medium, design: .rounded)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .foregroundStyle(
                    cell.isToday ? Color.white
                        : cell.isHoliday ? AppTheme.holidayRed
                        : .primary
                )
                .frame(width: daySide, height: daySide)
                .background {
                    if cell.isToday {
                        RoundedRectangle(cornerRadius: 5 * scale).fill(AppTheme.primary)
                    }
                }

            // 날짜 아래 일정 제목 — 길면 말줄임(…), 더 있으면 +N
            if let title = cell.title {
                Text(title)
                    .scaledFont(size: 7.5, weight: .bold)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .foregroundStyle(cell.colorTag?.color ?? .secondary)
                    .brightness(-0.12)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 1)
                    .padding(.vertical, 1)
                    .background(
                        (cell.colorTag?.color ?? .secondary).opacity(0.18),
                        in: RoundedRectangle(cornerRadius: 3)
                    )

                if cell.extraCount > 0 {
                    Text("+\(cell.extraCount)")
                        .scaledFont(size: 7, weight: .heavy)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct LargeCalendarView: View {
    let entry: ScheduleEntry

    var body: some View {
        CalendarGridView(entry: entry)
    }
}
