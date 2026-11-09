# Proguard rules for SMS2API
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
