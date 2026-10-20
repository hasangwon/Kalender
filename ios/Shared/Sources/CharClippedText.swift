import SwiftUI

/// 한 줄에 들어가는 글자까지만 보여주고 말줄임(…) 없이 글자 단위로 끊는 텍스트 (앱·위젯 공용).
/// SwiftUI는 줄 수를 제한하면 항상 …를 붙이므로, 글자 사이에 폭 0 공백(U+200B)을 넣어
/// 아무 글자에서나 줄이 바뀌게 한 뒤 첫 줄 높이만 보이게 자른다.
/// 폰트·색은 바깥에서 `.font` / `.foregroundStyle`로 지정한다.
struct CharClippedText: View {
    let text: String

    var body: some View {
        // 한 줄 높이 기준 (보이지 않음) — 레이아웃 크기는 이것만 정하므로 긴 제목이 칸을 넓히지 않는다
        Text(verbatim: "가")
            .hidden()
            .frame(maxWidth: .infinity)
            .overlay(alignment: .top) {
                Text(verbatim: Self.breakable(text))
                    .multilineTextAlignment(.center)
                    // 높이 제한 때문에 …가 붙지 않도록 원래 줄 수만큼 그린 뒤 첫 줄만 남긴다
                    .fixedSize(horizontal: false, vertical: true)
            }
            .clipped()
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(text)
    }

    private static func breakable(_ text: String) -> String {
        text.map(String.init).joined(separator: "\u{200B}")
    }
}
