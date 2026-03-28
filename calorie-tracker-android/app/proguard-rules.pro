-keepattributes Signature
-keepattributes *Annotation*

# Gson-backed local settings/data classes
-keep class com.codex.calorielens.data.** { *; }

# Room
-keep class androidx.room.RoomDatabase_Impl
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase_Impl

# WebView privacy page activity
-keep class com.codex.calorielens.PrivacyPolicyActivity { *; }

# ML Kit and CameraX entry points used reflectively by dependencies
-keep class com.google.mlkit.** { *; }
-keep class androidx.camera.** { *; }
