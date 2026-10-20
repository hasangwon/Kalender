package com.hasangwon.planwidget

import com.hasangwon.planwidget.shared.KoreanHolidays
import com.hasangwon.planwidget.shared.Lunar
import com.ibm.icu.util.Calendar
import com.ibm.icu.util.ChineseCalendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import java.io.File

/** JVM 테스트용 음력 엔진 — 기기의 android.icu 와 같은 ICU 코드(icu4j). 서울 시간대 해석도 동일 */
object Icu4jLunarEngine : Lunar.Engine {
    override fun components(epochMillis: Long): Lunar.Components {
        val calendar = ChineseCalendar(TimeZone.getTimeZone("Asia/Seoul"), ULocale.KOREA)
        calendar.clear()
        calendar.timeInMillis = epochMillis
        return Lunar.Components(
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH),
            isLeapMonth = calendar.get(ChineseCalendar.IS_LEAP_MONTH) == 1,
        )
    }
}

/** 저장소 공용 폴더(common/) 접근. Gradle 단위 테스트의 작업 폴더는 모듈 폴더(android/app)다 */
object Common {
    val root: File by lazy {
        generateSequence(File(".").absoluteFile) { it.parentFile }
            .map { File(it, "common") }
            .firstOrNull { File(it, "data/korean-holidays.json").isFile }
            ?: error("common/ 폴더를 찾을 수 없습니다 (작업 폴더: ${File(".").absolutePath})")
    }

    fun read(relativePath: String): String = File(root, relativePath).readText()
}

/** 테스트 공통 준비 — 음력 엔진 교체 + 공휴일 확정 테이블 로드 */
fun prepareSharedForTests() {
    Lunar.engine = Icu4jLunarEngine
    Lunar.clearCache()
    KoreanHolidays.loadFixedTable(Common.read("data/korean-holidays.json"))
}
