# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers and source file attributes for actionable crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve generic type signatures and annotations for reflection & serialization
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# ==============================================================================
# Room Database ProGuard Rules
# ==============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
-keep @androidx.room.Dao interface * { *; }
-keep class * implements androidx.room.RoomDatabase
-keep class * extends androidx.room.SharedSQLiteStatement { *; }
-keep class * extends androidx.room.paging.LimitOffsetDataSource { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}
-dontwarn androidx.room.paging.**
-dontwarn androidx.sqlite.db.**

# ==============================================================================
# Moshi ProGuard Rules
# ==============================================================================
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep class * extends com.squareup.moshi.JsonAdapter$Factory { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# ==============================================================================
# Retrofit & OkHttp ProGuard Rules
# ==============================================================================
-keepclassmembers,allowshrinking,allowoptimization interface * {
    @retrofit2.http.* <methods>;
}
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ==============================================================================
# Kotlin Coroutines
# ==============================================================================
-dontwarn kotlinx.coroutines.**


