import SwiftUI
import WidgetKit

struct ScheduleWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "ScheduleWidget", provider: ScheduleProvider()) { entry in
            ScheduleWidgetEntryView(entry: entry)
                .containerBackground(for: .widget) {
                    Color(.systemBackground)
                }
        }
        .configurationDisplayName("오늘 일정")
        .description("오늘과 다가오는 일정을 보여줍니다.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct ScheduleWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family

    let entry: ScheduleEntry

    var body: some View {
        switch family {
        case .systemMedium:
            MediumScheduleView(entry: entry)
        default:
            SmallScheduleView(entry: entry)
        }
    }
}

// MARK: - 공통 요소

private struct DateHeader: View {
    let date: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(date.formatted(.dateTime.month(.defaultDigits).locale(Locale(identifier: "ko_KR"))))
                .font(.caption.weight(.bold))
                .foregroundStyle(.red)
            Text("\(Calendar.current.component(.day, from: date))")
                .font(.system(size: 34, weight: .heavy, design: .rounded))
            Text(date.formatted(.dateTime.weekday(.wide).locale(Locale(identifier: "ko_KR"))))
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.secondary)
        }
    }
}

private struct ScheduleItemRow: View {
    let item: WidgetScheduleItem
    var showsTime = true

    var body: some View {
        HStack(spacing: 6) {
            RoundedRectangle(cornerRadius: 1.5)
                .fill(item.colorTag.color)
                .frame(width: 3, height: 22)

            VStack(alignment: .leading, spacing: 0) {
                Text(item.title)
                    .font(.caption.weight(.semibold))
                    .lineLimit(1)
                if showsTime, let timeText = item.timeText {
                    Text(timeText)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer(minLength: 0)
        }
    }
}

private struct EmptyScheduleView: View {
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
                EmptyScheduleView()
                    .frame(height: 44)
            } else {
                VStack(spacing: 4) {
                    ForEach(entry.today.prefix(2)) { item in
                        ScheduleItemRow(item: item)
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
                EmptyScheduleView()
            } else {
                VStack(spacing: 5) {
                    ForEach(entry.today.prefix(3)) { item in
                        ScheduleItemRow(item: item)
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
