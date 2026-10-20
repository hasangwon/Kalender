package com.hasangwon.planwidget.shared.model

/**
 * 일정 색상 팔레트 (8색). 색값은 common/design/colors.json 의 colorTag 와 같다.
 * Compose Color 변환은 UI 층(ColorExt.kt)에서 — 이 모델은 JVM 테스트에서도 쓰인다.
 */
enum class ColorTag(
    override val raw: String,
    val label: String,
    val argb: Long,
) : RawValued {
    RED("red", "빨강", 0xFFE25A52),
    ORANGE("orange", "주황", 0xFFEC9043),
    YELLOW("yellow", "노랑", 0xFFE5BA43),
    GREEN("green", "초록", 0xFF73A965),
    TEAL("teal", "청록", 0xFF4DA29D),
    BLUE("blue", "파랑", 0xFF5587CE),
    PURPLE("purple", "보라", 0xFF9276C7),
    PINK("pink", "분홍", 0xFFE582A3);

    companion object {
        fun from(raw: String?): ColorTag = entries.firstOrNull { it.raw == raw } ?: BLUE
    }
}
