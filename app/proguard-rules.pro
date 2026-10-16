# ProGuard / R8 rules for App-Anuman
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
