// AGP 9 는 Kotlin 을 내장한다(built-in Kotlin). org.jetbrains.kotlin.android 플러그인은 적용하지 않는다.
// 내장 KGP 기본 버전보다 높은 Kotlin 을 쓰기 위해 classpath 로 버전을 올린다
// (Compose·serialization 컴파일러 플러그인은 Kotlin 버전과 반드시 같아야 한다).
buildscript {
    dependencies {
        classpath(libs.kotlin.gradlePlugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
