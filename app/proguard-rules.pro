# ProGuard / R8 rules for App-Anuman
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-keep class com.appanuman.data.model.** { *; }
-keep class com.appanuman.model.** { *; }
