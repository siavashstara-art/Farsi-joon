# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line numbers for readable crash stack traces
-keepattributes Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keepattributes *Annotation*

# Keep Kotlin metadata for reflection/Moshi/coroutines
-keep class kotlin.Metadata { *; }

# Firebase, App Check & Google Play Services / Credentials
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# Moshi Codegen & Kotlin Reflect Fallback
-keep class * extends com.squareup.moshi.JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @com.squareup.moshi.JsonClass *;
    @com.squareup.moshi.* <fields>;
}
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.model.** { *; }

# Jetpack Compose
-keep class androidx.compose.runtime.** { *; }
