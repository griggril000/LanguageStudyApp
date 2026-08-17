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
