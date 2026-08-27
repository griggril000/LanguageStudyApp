# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\grigg\AppData\Local\Android\Sdk\tools\proguard\proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep rules here:

# Retrofit/OkHttp/Moshi rules might be needed if not provided by the libraries
# But most modern libraries provide their own rules.

# If you use Kotlin Serialization, ensure rules are kept
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepnames class kotlinx.serialization.internal.GeneratedSerializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}

# AboutLibraries
-keep class com.mikepenz.aboutlibraries.entity.** { *; }
-keep class com.mikepenz.aboutlibraries.Libs { *; }
-keep class com.mikepenz.aboutlibraries.ui.compose.** { *; }

# Firebase Firestore / Data Models
# Keep all data models that are used with Firestore toObject/toObjects
-keep class io.github.langstudy.data.model.** {
    public <init>();
    <fields>;
    <methods>;
}

# Room Entities
-keep class io.github.langstudy.data.local.entity.** {
    public <init>(...);
    <fields>;
    <methods>;
}

# Firebase Firestore specific annotations
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
    @com.google.firebase.firestore.DocumentId *;
    @com.google.firebase.firestore.Exclude *;
}

# Android YouTube Player (pierfrancescosoffritti)
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView { *; }
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener { *; }
-dontwarn com.pierfrancescosoffritti.androidyoutubeplayer.core.**

# GSON rules
-keep class com.google.gson.reflect.TypeToken
-keepattributes Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Moshi rules
# Redundant as Moshi provides its own rules and we use KSP codegen,
# but keeping the annotation attribute just in case.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations

# OkHttp/Retrofit rules are provided by the libraries themselves in modern versions.
# Only keeping attributes necessary for reflection-based libraries.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
