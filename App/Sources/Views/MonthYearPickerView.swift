import SwiftUI

/// 연·월 빠른 이동 피커. 타이틀 탭 시 뜨며, 원하는 연도·월로 한 번에 점프.
struct MonthYearPickerView: View {
    @Environment(\.dismiss) private var dismiss

    let displayedMonth: Date
    let onSelect: (Date) -> Void

    @State private var year: Int
    @State private var month: Int

    private let calendar = Calendar.current
    private let years: [Int]

    init(displayedMonth: Date, onSelect: @escaping (Date) -> Void) {
        self.displayedMonth = displayedMonth
        self.onSelect = onSelect

        let cal = Calendar.current
        let currentYear = cal.component(.year, from: .now)
        _year = State(initialValue: cal.component(.year, from: displayedMonth))
        _month = State(initialValue: cal.component(.month, from: displayedMonth))
        // 과거 10년 ~ 미래 10년
        years = Array((currentYear - 10)...(currentYear + 10))
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                HStack(spacing: 0) {
                    Picker("연", selection: $year) {
                        ForEach(years, id: \.self) { y in
                            Text(verbatim: "\(y)년").tag(y)
                        }
                    }
                    .pickerStyle(.wheel)
                    .frame(maxWidth: .infinity)

                    Picker("월", selection: $month) {
                        ForEach(1...12, id: \.self) { m in
                            Text("\(m)월").tag(m)
                        }
                    }
                    .pickerStyle(.wheel)
                    .frame(maxWidth: .infinity)
                }
                .padding(.top, 8)

                Button {
                    if let date = calendar.date(from: DateComponents(year: year, month: month, day: 1)) {
                        onSelect(date)
                    }
                    dismiss()
                } label: {
                    Text("이동")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(AppTheme.primary, in: RoundedRectangle(cornerRadius: 14))
                }
                .padding(.horizontal, 16)
            }
            .padding(.bottom, 16)
            .background(AppTheme.background)
            .fontDesign(.rounded)
            .navigationTitle("연·월 이동")
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
        .presentationDetents([.height(320)])
    }
}
