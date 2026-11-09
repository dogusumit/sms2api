# Proguard rules for SMS2API
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room generated classes & DAOs
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase

# Keep Data Models and Enums (Gson & Room serialization)
-keep class com.sms2api.app.data.model.** { *; }
-keepclassmembers class com.sms2api.app.data.model.** { *; }

# Gson rules
-keepattributes Signature
-keepclassmembers class * implements com.google.gson.TypeAdapterFactory { *; }
-keepclassmembers class * implements com.google.gson.JsonSerializer { *; }
-keepclassmembers class * implements com.google.gson.JsonDeserializer { *; }

# OkHttp & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

