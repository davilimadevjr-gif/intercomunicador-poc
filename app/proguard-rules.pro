# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\User\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Keep Jetpack Compose rules
-keep class androidx.compose.** { *; }
-keep class com.intercomunicador.poc.** { *; }
