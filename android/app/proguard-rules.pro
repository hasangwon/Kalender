# kotlinx.serialization — 백업 파일 직렬화 클래스 유지
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.hasangwon.planwidget.**$$serializer { *; }
-keepclassmembers class com.hasangwon.planwidget.** { *** Companion; }
-keepclasseswithmembers class com.hasangwon.planwidget.** { kotlinx.serialization.KSerializer serializer(...); }
