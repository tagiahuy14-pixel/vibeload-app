# Chaquopy rules
-keep class com.chaquo.python.** { *; }
-dontwarn com.chaquo.python.**

# FFmpegKit rules
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**

# Media3 rules
-keep class androidx.media3.** { *; }

# App Models & Entities
-keep class com.vibeload.app.domain.model.** { *; }
-keep class com.vibeload.app.data.local.entity.** { *; }
-keepattributes *Annotation*
