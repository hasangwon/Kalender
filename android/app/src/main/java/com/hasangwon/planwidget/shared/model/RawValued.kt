package com.hasangwon.planwidget.shared.model

/**
 * 문자열 원시값으로 저장되는 enum.
 * 값은 iOS 의 rawValue 와 글자 그대로 같아야 한다 — 설정 키 호환과 백업 파일(.hscal) 호환의 전제.
 */
interface RawValued {
    val raw: String
}
