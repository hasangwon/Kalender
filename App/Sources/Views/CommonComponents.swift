import SwiftUI

// 여러 화면이 같은 모양으로 쓰는 공용 UI 조각

extension View {
    /// 설정·동기화·정보 화면 등의 흰 카드 컨테이너
    func cardStyle() -> some View {
        padding(18)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
            .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }

    /// 카드 제목 텍스트
    func cardTitleFont() -> some View {
        font(.system(.headline, design: .rounded).weight(.bold))
    }
}

/// 시트 우상단 닫기(xmark) 버튼
struct SheetCloseButton: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
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

/// 색 선택용 동그라미 버튼 (선택 시 흰 체크)
struct ColorSwatchButton: View {
    let color: Color
    let isSelected: Bool
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Circle()
                .fill(color)
                .frame(width: 28, height: 28)
                .overlay {
                    if isSelected {
                        Image(systemName: "checkmark")
                            .font(.caption2.bold())
                            .foregroundStyle(.white)
                    }
                }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}
