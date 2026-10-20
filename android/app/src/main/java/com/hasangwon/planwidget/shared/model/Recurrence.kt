package com.hasangwon.planwidget.shared.model

/** 반복 유형 (iOS Recurrence 대응) */
enum class Recurrence(
    override val raw: String,
    val label: String,
    val badgeText: String?,
) : RawValued {
    NONE("none", "단일", null),
    WEEKLY("weekly", "매주", "매주"),
    MONTHLY("monthly", "매달", "매달");

    companion object {
        fun from(raw: String?): Recurrence = entries.firstOrNull { it.raw == raw } ?: NONE
    }
}
