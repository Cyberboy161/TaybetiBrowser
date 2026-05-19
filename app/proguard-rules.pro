# R8 keep rules for Taybeti Browser

# Suppress missing javax.annotation warnings (from crypto libraries)
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature

# General Android
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses
-keepattributes EnclosingMethod