import SwiftUI

/// 하단에서 잠깐 떴다 사라지는 토스트. 연속 탭 시 쌓이지 않고 메시지만 갱신됨.
struct ToastModifier: ViewModifier {
    @Binding var message: String?

    func body(content: Content) -> some View {
        content.overlay(alignment: .bottom) {
            if let message {
                Text(message)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 12)
                    .background(.black.opacity(0.82), in: Capsule())
                    .padding(.bottom, 40)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    // message가 바뀔 때마다 타이머 리셋 → 쌓이지 않고 마지막 것만 유지
                    .id(message)
                    .task(id: message) {
                        try? await Task.sleep(for: .seconds(2))
                        withAnimation(.snappy) { self.message = nil }
                    }
            }
        }
        .animation(.snappy(duration: 0.25), value: message)
    }
}

extension View {
    func toast(message: Binding<String?>) -> some View {
        modifier(ToastModifier(message: message))
    }
}
