# GeckoView - Keep all GeckoView classes
-keep class org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.** { *; }
-dontwarn org.mozilla.geckoview.**
-dontwarn org.mozilla.gecko.**

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Material
-keep class com.google.android.material.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep our security classes
-keep class com.Taybetibrowser.security.** { *; }
-keep class com.Taybetibrowser.keyboard.** { *; }
-keep class com.Taybetibrowser.browser.** { *; }
