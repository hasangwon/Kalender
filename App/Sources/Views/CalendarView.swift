import StoreKit
import SwiftData
import SwiftUI
import WidgetKit

struct CalendarView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.requestReview) private var requestReview
    @Query(sort: \Schedule.createdAt) private var schedules: [Schedule]
    @Query(sort: \AnniversaryEntry.createdAt) private var anniversaries: [AnniversaryEntry]
    @EnvironmentObject private var appleCalendar: AppleCalendarManager

    @State private var displayedMonth: Date = Calendar.current.startOfMonth(for: .now)
    @State private var selectedDate: Date = Calendar.current.startOfDay(for: .now)
    @State private var editingSchedule: Schedule?
    @State private var isAddingSchedule = false
    @State private var isShowingAnniversaries = false
    @State private var isShowingSettings = false
    @State private var isShowingSearch = false
    @State private var isShowingInfo = false
    @State private var isShowingSync = false
    @State private var toastMessage: String?
    @State private var isShowingMonthPicker = false
    @State private var textSize = TextSizeSettings.current
    /// 설정 시트에서 색을 바꾸면 달력을 다시 그리기 위한 트리거
    @State private var colorRefreshID = UUID()
    /// 월 전환 슬라이드 방향 (true = 다음 달, 오른쪽/아래에서 들어옴)
    @State private var slideForward = true
    @State private var swipeDirection = MonthSwipeSettings.current
    /// 드래그로 넘기는 중인지 — 화면을 다시 그리지 않도록 참조 타입에 담는다
    @State private var pagerGate = MonthPagerGate()

    private let calendar = Calendar.current

    /// 툴바 아이콘 크기 — 나브바 높이 고정이라 배율 대신 기기별 고정값
    private var toolbarIconSize: CGFloat {
        #if os(iOS)
        return UIDevice.current.userInterfaceIdiom == .pad ? 27 : 18
        #else
        return 18
        #endif
    }

    /// 툴바 타이틀 크기
    private var toolbarTitleSize: CGFloat {
        #if os(iOS)
        return UIDevice.current.userInterfaceIdiom == .pad ? 30 : 20
        #else
        return 20
        #endif
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                topBar

                calendarCard
                    .padding(.horizontal, 16)
                    .padding(.top, 4)

                daySection
                    .frame(height: 220)
            }
            .background(AppTheme.background)
            .fontDesign(.rounded)
            .dynamicTypeSize(textSize.dynamicTypeSize)
            .toast(message: $toastMessage)
            .toolbar(.hidden, for: .navigationBar)
            .sheet(isPresented: $isAddingSchedule, onDismiss: requestReviewIfEarned) {
                ScheduleFormView(defaultDate: selectedDate)
            }
            .sheet(item: $editingSchedule) { schedule in
                ScheduleFormView(schedule: schedule)
            }
            .sheet(isPresented: $isShowingAnniversaries) {
                AnniversaryListView()
            }
            .sheet(isPresented: $isShowingSettings, onDismiss: {
                colorRefreshID = UUID()
                textSize = TextSizeSettings.current
                swipeDirection = MonthSwipeSettings.current
            }) {
                SettingsView()
            }
            .sheet(isPresented: $isShowingInfo) {
                InfoView()
            }
            .sheet(isPresented: $isShowingSync) {
                SyncView()
            }
            .sheet(isPresented: $isShowingSearch) {
                SearchView { targetDate in
                    setMonth(targetDate, alsoSelect: targetDate)
                }
            }
        }
    }

    // MARK: - 커스텀 상단바

    /// iPad에서 상단바를 더 높게 (시스템 나브바는 높이 고정이라 커스텀으로 대체)
    private var topBarHeight: CGFloat {
        #if os(iOS)
        return UIDevice.current.userInterfaceIdiom == .pad ? 76 : 50
        #else
        return 50
        #endif
    }

    private var topBar: some View {
        HStack {
            Menu {
                Button {
                    isShowingAnniversaries = true
                } label: {
                    Label("매년 기념일 등록", systemImage: "gift")
                }

                Button {
                    isShowingSync = true
                } label: {
                    Label("동기화", systemImage: "arrow.triangle.2.circlepath")
                }

                Button {
                    isShowingSettings = true
                } label: {
                    Label("설정", systemImage: "gearshape")
                }

                Button {
                    isShowingInfo = true
                } label: {
                    Label("정보", systemImage: "info.circle")
                }
            } label: {
                Image(systemName: "line.3.horizontal")
                    .font(.system(size: toolbarIconSize, weight: .semibold))
                    .foregroundStyle(.primary)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }

            Spacer()

            Text("달력")
                .font(.system(size: toolbarTitleSize, weight: .heavy, design: .rounded))
                .foregroundStyle(AppTheme.primary)

            Spacer()

            Button {
                isShowingSearch = true
            } label: {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: toolbarIconSize, weight: .semibold))
                    .foregroundStyle(.primary)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
        }
        .padding(.horizontal, 12)
        .frame(height: topBarHeight)
    }

    // MARK: - 달력 카드

    private var calendarCard: some View {
        VStack(spacing: 6) {
            monthHeader
            weekdayHeader
            monthGrid
                .frame(maxHeight: .infinity)
        }
        .padding(.vertical, 14)
        .padding(.horizontal, 6)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        // 그림자는 내용이 아니라 배경 도형에만 건다. 카드 전체에 걸면 월 넘기기 드래그 중
        // 움직이는 날짜 칸까지 매 프레임 다시 래스터라이즈·블러해 프레임이 크게 떨어진다.
        .background {
            RoundedRectangle(cornerRadius: 24)
                .fill(AppTheme.surface)
                .shadow(color: .black.opacity(0.05), radius: 12, y: 4)
        }
    }

    private var monthHeader: some View {
        HStack(spacing: 10) {
            Button {
                isShowingMonthPicker = true
            } label: {
                HStack(spacing: 4) {
                    Text(displayedMonth.formatted(.dateTime.year().month(.wide).locale(Locale(identifier: "ko_KR"))))
                        .font(.system(size: 22 * textSize.scale, weight: .heavy, design: .rounded))
                        .foregroundStyle(.primary)
                    Image(systemName: "chevron.down")
                        .font(.system(size: 12 * textSize.scale, weight: .bold))
                        .foregroundStyle(.secondary)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            Button("오늘") { goToToday() }
                .font(.system(size: 13 * textSize.scale, weight: .bold, design: .rounded))
                .foregroundStyle(.secondary)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(Color.primary.opacity(0.06), in: Capsule())

            Spacer()

            HStack(spacing: 8) {
                // 위아래 모드에선 화살표도 위/아래로 맞춘다
                let isVertical = swipeDirection == .vertical
                monthNavButton(systemName: isVertical ? "chevron.up" : "chevron.left") { moveMonth(by: -1) }
                monthNavButton(systemName: isVertical ? "chevron.down" : "chevron.right") { moveMonth(by: 1) }
            }
        }
        .padding(.horizontal, 12)
        .padding(.top, monthHeaderVerticalPadding)
        .padding(.bottom, monthHeaderVerticalPadding + 4)
        .sheet(isPresented: $isShowingMonthPicker) {
            MonthYearPickerView(displayedMonth: displayedMonth) { picked in
                setMonth(picked)
            }
        }
    }

    /// iPad에서만 월 헤더 영역 높이를 넉넉하게
    private var monthHeaderVerticalPadding: CGFloat {
        #if os(iOS)
        return UIDevice.current.userInterfaceIdiom == .pad ? 22 : 0
        #else
        return 0
        #endif
    }

    private func monthNavButton(systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 13 * textSize.scale, weight: .bold))
                .foregroundStyle(.secondary)
                .frame(width: 30 * min(textSize.scale, 1.4), height: 30 * min(textSize.scale, 1.4))
                .background(Color.primary.opacity(0.05), in: Circle())
        }
        .buttonStyle(.plain)
    }

    private var weekdayHeader: some View {
        HStack(spacing: 0) {
            ForEach(Array(["일", "월", "화", "수", "목", "금", "토"].enumerated()), id: \.offset) { index, symbol in
                Text(symbol)
                    .font(.system(size: 12 * textSize.scale, weight: .bold, design: .rounded))
                    .foregroundStyle(index == 0 || index == 6 ? AppTheme.holidayRed : .secondary)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 6)
        .padding(.top, 12)
        .padding(.bottom, 4)
    }

    // MARK: - 달력 그리드

    private var monthGrid: some View {
        // 이전/현재/다음 달 3장을 항상 미리 깔아 둔다 (이웃 달은 화면 밖, 잘려서 안 보임).
        // 드래그 중엔 MonthSwipePager가 위치만 옮기므로 날짜 칸을 새로 만들거나 다시 계산하지 않는다.
        GeometryReader { geometry in
            let size = geometry.size

            MonthSwipePager(
                size: size,
                pageGap: pageGap,
                swipeDirection: swipeDirection,
                gate: pagerGate,
                onPaged: { step in
                    if let next = calendar.date(byAdding: .month, value: step, to: displayedMonth) {
                        displayedMonth = next
                    }
                },
                onPagingFinished: {
                    appleCalendar.loadEvents(around: displayedMonth, calendar: calendar)
                }
            ) {
                ZStack(alignment: .top) {
                    ForEach(pagedMonths, id: \.self) { month in
                        monthPage(for: month, size: size)
                            .modifier(MonthPageSlot(
                                index: calendar.dateComponents([.month], from: displayedMonth, to: month).month ?? 0,
                                size: size,
                                gap: pageGap
                            ))
                            // 월 선택/검색으로 멀리 이동할 때는 페이지가 통째로 교체된다
                            .transition(monthSlide)
                    }
                }
                .frame(width: size.width, height: size.height, alignment: .top)
            }
        }
        .clipped()
        .id(colorRefreshID)
        .padding(.horizontal, 6)
    }

    private var pagedMonths: [Date] {
        [-1, 0, 1].compactMap { calendar.date(byAdding: .month, value: $0, to: displayedMonth) }
    }

    /// 버튼/월 선택 이동 시 미끄러지는 축 (좌우+위아래는 좌우)
    private var pagingAxis: Axis {
        swipeDirection == .vertical ? .vertical : .horizontal
    }

    /// 이웃 달과의 간격 — 붙어 있으면 두 달의 날짜가 이어져 읽히지 않는다
    private let pageGap: CGFloat = 40

    private func monthPage(for month: Date, size: CGSize) -> some View {
        let weeks = makeWeekRows(for: month)
        // 칸마다 전체 일정을 훑지 않도록 이 달 그리드 전체를 한 번에 계산
        let eventsByDay = DayEventResolver.eventsByDay(
            schedules: schedules, anniversaries: anniversaries,
            appleEvents: appleCalendar.events, days: weeks.flatMap { $0 }, calendar: calendar
        )
        // 남은 높이를 주 수로 나눠 각 주에 정확한 높이 부여.
        // 어떤 글자 크기/기기에서도 화면을 넘지 않으면서 주 높이가 완벽히 균일해진다.
        let rowHeight = size.height / CGFloat(max(weeks.count, 1))

        return VStack(spacing: 0) {
            ForEach(weeks.indices, id: \.self) { weekIndex in
                HStack(spacing: 2) {
                    ForEach(0..<7, id: \.self) { dayIndex in
                        let day = weeks[weekIndex][dayIndex]
                        dayCell(
                            for: day,
                            events: eventsByDay[calendar.startOfDay(for: day)] ?? [],
                            isCurrentMonth: calendar.isDate(day, equalTo: month, toGranularity: .month)
                        )
                    }
                }
                .frame(height: rowHeight)
            }
        }
        .frame(width: size.width, height: size.height, alignment: .top)
    }

    /// 월 전환 — 진행 방향에서 들어오고 반대쪽으로 나간다
    private var monthSlide: AnyTransition {
        let forwardIn: Edge = pagingAxis == .horizontal ? .trailing : .bottom
        let forwardOut: Edge = pagingAxis == .horizontal ? .leading : .top

        return .asymmetric(
            insertion: .move(edge: slideForward ? forwardIn : forwardOut).combined(with: .opacity),
            removal: .move(edge: slideForward ? forwardOut : forwardIn).combined(with: .opacity)
        )
    }

    private func dayCell(for day: Date, events dayEvents: [DayEvent], isCurrentMonth: Bool) -> some View {
        let isSelected = calendar.isDate(day, inSameDayAs: selectedDate)
        let isToday = calendar.isDateInToday(day)
        let weekday = calendar.component(.weekday, from: day)
        let isHoliday = KoreanHolidays.isHoliday(day, calendar: calendar)

        return Button {
            // 인접 달 날짜를 누르면 슬라이드로 그 달에 이동
            if isCurrentMonth {
                selectedDate = day
            } else {
                setMonth(day, alsoSelect: day)
            }
        } label: {
            VStack(spacing: 2) {
                Text("\(calendar.component(.day, from: day))")
                    .font(.system(size: 13 * textSize.scale, weight: isToday || isSelected ? .heavy : .semibold, design: .rounded))
                    .foregroundStyle(
                        isSelected ? Color.white
                            : isHoliday || weekday == 1 || weekday == 7 ? AppTheme.holidayRed
                            : isToday ? AppTheme.primary
                            : .primary
                    )
                    .frame(width: 24 * textSize.scale, height: 24 * textSize.scale)
                    .background {
                        if isSelected {
                            RoundedRectangle(cornerRadius: 8).fill(AppTheme.primary)
                        } else if isToday {
                            RoundedRectangle(cornerRadius: 8).fill(AppTheme.primary.opacity(0.12))
                        }
                    }

                // 위젯처럼 날짜 아래 일정 라벨 (최대 2개, 길면 …)
                // 고정 높이 영역에 담아 라벨 유무가 셀/주 높이에 영향을 주지 않게 함
                VStack(spacing: 1.5) {
                    ForEach(dayEvents.prefix(2)) { event in
                        dayEventChip(event)
                    }
                    if dayEvents.count > 2 {
                        Text("+\(dayEvents.count - 2)")
                            .font(.system(size: 7.5 * textSize.scale, weight: .heavy))
                            .foregroundStyle(.secondary)
                    }
                }
                .frame(maxHeight: 34 * textSize.scale, alignment: .top)
                .clipped()

                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .opacity(isCurrentMonth ? 1 : 0.32)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func dayEventChip(_ event: DayEvent) -> some View {
        let tint = event.kind == .holiday ? AppTheme.holidayRed : (event.colorTag?.color ?? .secondary)

        return Text(event.title)
            .font(.system(size: 8 * textSize.scale, weight: .bold))
            .lineLimit(1)
            .truncationMode(.tail)
            // .brightness 필터는 칩마다 오프스크린 패스를 만들어 드래그가 끊긴다 → 색 자체를 어둡게
            .foregroundStyle(tint.darkened(by: 0.12))
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 2)
            .padding(.vertical, 1.5)
            .background(tint.opacity(0.18), in: RoundedRectangle(cornerRadius: 4))
    }

    // MARK: - 선택한 날짜 일정

    private var daySection: some View {
        let dayEvents = DayEventResolver.events(
            schedules: schedules, anniversaries: anniversaries,
            appleEvents: appleCalendar.events, on: selectedDate, calendar: calendar
        )
        let daySchedules = ScheduleStore.occurrences(in: schedules, on: selectedDate, calendar: calendar)
        // 공휴일/생일/애플달력 = 수정 불가 카드로 표시
        let staticEvents = dayEvents.filter { $0.kind != .schedule }

        return VStack(spacing: 0) {
            HStack(alignment: .firstTextBaseline) {
                Text(selectedDate.formatted(
                    .dateTime.month().day().weekday(.wide).locale(Locale(identifier: "ko_KR"))
                ))
                .font(.system(size: 17 * textSize.scale, weight: .bold, design: .rounded))

                Text(Lunar.text(for: selectedDate))
                    .font(.system(size: 11 * textSize.scale, weight: .regular, design: .rounded))
                    .foregroundStyle(.tertiary)

                Spacer()

                Text("\(dayEvents.count)개")
                    .font(.system(size: 13 * textSize.scale, weight: .semibold, design: .rounded))
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 8)

            if dayEvents.isEmpty {
                Button {
                    isAddingSchedule = true
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "plus")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(AppTheme.primary)
                            .frame(width: 34, height: 34)
                            .background(AppTheme.primary.opacity(0.12), in: Circle())

                        VStack(alignment: .leading, spacing: 1) {
                            Text("일정이 없어요")
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(.primary)
                            Text("탭해서 일정을 추가해 보세요")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }

                        Spacer()
                    }
                    .padding(14)
                    .background(cardBackground())
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 16)

                Spacer(minLength: 0)
            } else {
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(staticEvents) { event in
                            staticEventCard(event)
                                .onTapGesture {
                                    if event.kind == .appleCalendar {
                                        toastMessage = "이 일정은 애플 달력 앱에서 수정할 수 있어요"
                                    }
                                }
                        }

                        ForEach(daySchedules) { schedule in
                            scheduleCard(schedule)
                        }

                        Button {
                            isAddingSchedule = true
                        } label: {
                            Label("일정 추가", systemImage: "plus")
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(AppTheme.primary)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(
                                    RoundedRectangle(cornerRadius: 16)
                                        .strokeBorder(AppTheme.primary.opacity(0.35), style: StrokeStyle(lineWidth: 1.5, dash: [5, 4]))
                                )
                        }
                        .padding(.top, 2)
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 20)
                }
            }
        }
    }

    private func cardBackground() -> some View {
        RoundedRectangle(cornerRadius: 16)
            .fill(AppTheme.surface)
            .shadow(color: .black.opacity(0.04), radius: 8, y: 2)
    }

    /// 공휴일/생일/애플달력 카드 (수정 불가 항목)
    private func staticEventCard(_ event: DayEvent) -> some View {
        let tint: Color = {
            switch event.kind {
            case .holiday: return AppTheme.primary
            case .appleCalendar: return .gray
            default: return event.colorTag?.color ?? .secondary
            }
        }()
        let badge: String = {
            switch event.kind {
            case .holiday: return "공휴일"
            case .appleCalendar: return "애플 달력"
            default: return "기념일"
            }
        }()

        return HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 2)
                .fill(tint)
                .frame(width: 4, height: 34)

            VStack(alignment: .leading, spacing: 2) {
                Text(event.title)
                    .font(.system(size: 15 * textSize.scale, weight: .bold, design: .rounded))
                if let timeText = event.timeText {
                    Text(timeText)
                        .font(.system(size: 12 * textSize.scale, weight: .regular, design: .rounded))
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()

            Text(badge)
                .font(.caption2.weight(.bold))
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(tint.opacity(0.13))
                .foregroundStyle(tint)
                .clipShape(Capsule())
        }
        .padding(14)
        .background(cardBackground())
    }

    private func scheduleCard(_ schedule: Schedule) -> some View {
        let typeColor = schedule.displayColor

        return Button {
            editingSchedule = schedule
        } label: {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(typeColor.color)
                    .frame(width: 4, height: 34)

                VStack(alignment: .leading, spacing: 2) {
                    Text(schedule.title)
                        .font(.system(size: 15 * textSize.scale, weight: .bold, design: .rounded))
                        .foregroundStyle(.primary)
                    if !schedule.memo.isEmpty {
                        Text(schedule.memo)
                            .font(.system(size: 12 * textSize.scale, weight: .regular, design: .rounded))
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 4) {
                    if let badge = schedule.recurrenceBadgeText {
                        Text(badge)
                            .font(.caption2.weight(.bold))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(typeColor.color.opacity(0.13))
                            .foregroundStyle(typeColor.color)
                            .clipShape(Capsule())
                    }
                    Text(schedule.timeText ?? "종일")
                        .font(.system(size: 12 * textSize.scale, weight: .regular, design: .rounded))
                        .foregroundStyle(.secondary)
                }
            }
            .padding(14)
            .background(cardBackground())
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .contextMenu {
            Button(role: .destructive) {
                deleteSchedule(schedule)
            } label: {
                Label("삭제", systemImage: "trash")
            }
        }
    }

    // MARK: - 동작

    private func makeWeekRows(for month: Date) -> [[Date]] {
        guard let range = calendar.range(of: .day, in: .month, for: month) else { return [] }

        let firstWeekday = calendar.component(.weekday, from: month)
        let leading = firstWeekday - 1

        // 이번 달 날짜
        let days: [Date] = range.compactMap {
            calendar.date(byAdding: .day, value: $0 - 1, to: month)
        }

        // 앞쪽: 전달 말일들 (같은 줄에 흐릿하게)
        var leadingDays: [Date] = []
        for offset in stride(from: leading, to: 0, by: -1) {
            if let date = calendar.date(byAdding: .day, value: -offset, to: month) {
                leadingDays.append(date)
            }
        }

        var slots = leadingDays + days

        // 뒤쪽: 다음 달 초반 (마지막 줄 채우기)
        while slots.count % 7 != 0 {
            if let last = slots.last,
               let next = calendar.date(byAdding: .day, value: 1, to: last) {
                slots.append(next)
            } else {
                break
            }
        }

        return stride(from: 0, to: slots.count, by: 7).map {
            Array(slots[$0..<min($0 + 7, slots.count)])
        }
    }

    private func moveMonth(by value: Int) {
        guard let next = calendar.date(byAdding: .month, value: value, to: displayedMonth) else { return }

        setMonth(next)
    }

    private func goToToday() {
        setMonth(.now, alsoSelect: .now)
    }

    /// 표시 월 변경의 단일 경로. 이동 방향을 먼저 정해 슬라이드 전환이
    /// 항상 실제 이동 방향과 일치하게 한다.
    /// 버튼/검색/오늘 등으로 넘길 때는 설정한 넘기기 방향의 축으로 미끄러진다.
    private func setMonth(_ target: Date, alsoSelect selection: Date? = nil) {
        // 드래그·버튼으로 넘기는 중엔 무시 — 화살표 연타로 애니메이션이 겹쳐
        // 여러 달이 한꺼번에 밀리며 빈 화면이 보이는 것을 막는다 (멀리 이동은 월 선택으로)
        guard !pagerGate.isBusy else { return }

        let normalized = calendar.startOfMonth(for: target)
        guard normalized != displayedMonth || selection != nil else { return }

        slideForward = normalized >= displayedMonth
        pagerGate.isBusy = true
        withAnimation(.snappy(duration: 0.28)) {
            displayedMonth = normalized
            if let selection {
                selectedDate = calendar.startOfDay(for: selection)
            }
        } completion: { [pagerGate] in
            pagerGate.isBusy = false
        }
        appleCalendar.loadEvents(around: normalized, calendar: calendar)
    }

    /// 일정을 막 추가한 직후(긍정적인 순간)에만, 조건을 만족하면 리뷰를 요청한다.
    /// 실제 노출 여부는 시스템이 정하므로 시도 자체를 버전당 한 번으로 제한한다.
    private func requestReviewIfEarned() {
        guard ReviewRequester.shouldRequest(scheduleCount: schedules.count, calendar: calendar)
        else { return }

        ReviewRequester.markRequested()
        Task { @MainActor in
            // 시트가 완전히 닫힌 뒤에 띄워야 시스템 팝업이 가려지지 않는다
            try? await Task.sleep(for: .seconds(1))
            requestReview()
        }
    }

    private func deleteSchedule(_ schedule: Schedule) {
        modelContext.delete(schedule)
        try? modelContext.save()
        WidgetCenter.shared.reloadAllTimelines()
        NotificationManager.refresh(context: modelContext)
    }
}

// MARK: - 월 넘기기 드래그

/// 드래그 넘기기 진행 여부. 값이 바뀌어도 CalendarView를 다시 그리지 않도록 참조 타입으로 둔다.
private final class MonthPagerGate {
    var isBusy = false
}

private struct MonthPagerAxisKey: EnvironmentKey {
    static let defaultValue: Axis = .horizontal
}

private extension EnvironmentValues {
    /// 이웃 달을 깔아 둘 축 — 드래그 시작 시 MonthSwipePager가 정한다
    var monthPagerAxis: Axis {
        get { self[MonthPagerAxisKey.self] }
        set { self[MonthPagerAxisKey.self] = newValue }
    }
}

/// 달 페이지를 index(-1/0/1)만큼 축 방향으로 비켜 놓는다.
/// 축이 바뀌어도 이 modifier만 다시 돌고 페이지 내용은 다시 계산되지 않는다.
private struct MonthPageSlot: ViewModifier {
    let index: Int
    let size: CGSize
    let gap: CGFloat
    @Environment(\.monthPagerAxis) private var axis

    func body(content: Content) -> some View {
        content.offset(
            x: axis == .horizontal ? CGFloat(index) * (size.width + gap) : 0,
            y: axis == .vertical ? CGFloat(index) * (size.height + gap) : 0
        )
    }
}

/// 설정한 방향으로 끌면 달력이 손가락을 따라 움직이고, 놓을 때 넘길지 정한다.
/// 왼쪽/위로 밀면 다음 달, 오른쪽/아래로 밀면 이전 달.
///
/// 드래그 상태(거리·축)는 모두 이 뷰 안에만 둔다. 드래그 중엔 이 body만 다시 돌고,
/// 페이지 내용(content)은 부모가 만든 값을 그대로 쓰므로 날짜 칸을 다시 계산하지 않는다.
private struct MonthSwipePager<Content: View>: View {
    let size: CGSize
    let pageGap: CGFloat
    let swipeDirection: MonthSwipeDirection
    let gate: MonthPagerGate
    /// 넘기기 애니메이션이 끝난 순간 — 기준 월을 step만큼 옮긴다 (애니메이션 없는 트랜잭션 안)
    let onPaged: (Int) -> Void
    /// 실제로 달이 바뀐 뒤 후속 작업
    let onPagingFinished: () -> Void
    @ViewBuilder let content: Content

    /// 드래그 중 손가락을 따라 밀린 거리 (잠긴 축 기준)
    @State private var dragOffset: CGFloat = 0
    /// 드래그 시작 시 잠근 축 — 넘기기 애니메이션이 끝날 때까지 유지
    @State private var dragAxis: Axis?
    /// 놓은 뒤 넘기기/복귀 애니메이션 중 — 새 드래그를 받지 않는다
    @State private var isPaging = false
    /// 설정에서 허용하지 않은 방향으로 시작한 드래그 (끝날 때까지 무시)
    @State private var isDragRejected = false
    /// 손가락이 닿아 있는 동안 true. 시스템이 제스처를 취소하면 onEnded 없이 false로 돌아온다.
    @GestureState private var isDragGestureActive = false

    /// 드래그 중이 아니면 설정 방향의 축 (좌우+위아래는 좌우)
    private var layoutAxis: Axis {
        dragAxis ?? (swipeDirection == .vertical ? .vertical : .horizontal)
    }

    var body: some View {
        content
            .environment(\.monthPagerAxis, layoutAxis)
            .offset(
                x: dragAxis == .horizontal ? dragOffset : 0,
                y: dragAxis == .vertical ? dragOffset : 0
            )
            .frame(width: size.width, height: size.height, alignment: .top)
            .contentShape(Rectangle())
            // 날짜 칸이 버튼이라 일반 .gesture면 버튼이 터치를 붙잡고 있다가 손을 뗄 때
            // 한꺼번에 넘겨줘 손가락을 따라가지 못한다. 드래그를 우선시하고 탭은 버튼에 맡긴다.
            .highPriorityGesture(monthSwipe)
            // 전화·알림 센터 등으로 제스처가 취소되면 onEnded가 불리지 않는다 → 밀린 상태 복구
            .onChange(of: isDragGestureActive) { _, isActive in
                guard !isActive else { return }

                isDragRejected = false
                if dragAxis != nil, !isPaging {
                    finishPaging(step: 0, pageLength: 0)
                }
            }
    }

    private func pageLength(for axis: Axis) -> CGFloat {
        (axis == .horizontal ? size.width : size.height) + pageGap
    }

    private var monthSwipe: some Gesture {
        DragGesture(minimumDistance: 10)
            .updating($isDragGestureActive) { _, isActive, _ in isActive = true }
            .onChanged { value in
                guard !isPaging else { return }
                if dragAxis == nil, !isDragRejected {
                    lockDragAxis(for: value.translation)
                }
                guard let axis = dragAxis else { return }

                let pageLength = pageLength(for: axis)
                let translation = axis == .horizontal ? value.translation.width : value.translation.height
                // 한 페이지 이상은 밀리지 않게
                dragOffset = min(max(translation, -pageLength), pageLength)
            }
            .onEnded { value in
                isDragRejected = false
                guard !isPaging, let axis = dragAxis else { return }

                let pageLength = pageLength(for: axis)
                let predicted = axis == .horizontal
                    ? value.predictedEndTranslation.width
                    : value.predictedEndTranslation.height

                // 페이지의 1/4 이상 끌었거나, 빠르게 튕겨 반 페이지 이상 갈 기세면 넘긴다
                let step: Int
                if dragOffset < 0, -dragOffset > pageLength * 0.25 || -predicted > pageLength * 0.5 {
                    step = 1
                } else if dragOffset > 0, dragOffset > pageLength * 0.25 || predicted > pageLength * 0.5 {
                    step = -1
                } else {
                    step = 0
                }

                finishPaging(step: step, pageLength: pageLength)
            }
    }

    /// 처음 움직인 방향으로 축을 잠근다. 설정에서 막힌 방향이면 이번 드래그는 무시.
    private func lockDragAxis(for translation: CGSize) {
        // 버튼으로 넘기는 애니메이션 중에 시작한 드래그는 무시
        guard !gate.isBusy else {
            isDragRejected = true
            return
        }

        let isHorizontal = abs(translation.width) > abs(translation.height)

        if isHorizontal, swipeDirection.allowsHorizontal {
            dragAxis = .horizontal
        } else if !isHorizontal, swipeDirection.allowsVertical {
            dragAxis = .vertical
        } else {
            isDragRejected = true
            return
        }
        gate.isBusy = true
    }

    /// 놓은 뒤 남은 거리를 마저 밀고(step 0이면 제자리로), 끝나면 표시 월을 확정한다.
    private func finishPaging(step: Int, pageLength: CGFloat) {
        isPaging = true

        withAnimation(.easeOut(duration: 0.22)) {
            dragOffset = -CGFloat(step) * pageLength
        } completion: {
            // 다음 달 페이지가 이미 제자리에 와 있으므로 애니메이션 없이 기준만 바꾼다
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) {
                if step != 0 {
                    onPaged(step)
                }
                dragOffset = 0
                dragAxis = nil
            }
            isPaging = false
            gate.isBusy = false

            if step != 0 {
                onPagingFinished()
            }
        }
    }
}

private extension Color {
    /// `.brightness(-amount)`와 같은 결과를 필터 없이 색으로 만든다 (라이트/다크 각각 계산)
    func darkened(by amount: CGFloat) -> Color {
        let base = UIColor(self)
        return Color(UIColor { traits in
            var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
            base.resolvedColor(with: traits).getRed(&red, green: &green, blue: &blue, alpha: &alpha)
            return UIColor(
                red: max(red - amount, 0),
                green: max(green - amount, 0),
                blue: max(blue - amount, 0),
                alpha: alpha
            )
        })
    }
}

extension Calendar {
    func startOfMonth(for date: Date) -> Date {
        self.date(from: dateComponents([.year, .month], from: date)) ?? date
    }
}
